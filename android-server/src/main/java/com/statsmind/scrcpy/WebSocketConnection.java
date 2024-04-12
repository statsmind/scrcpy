package com.statsmind.scrcpy;

import android.content.Context;
import android.content.Intent;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.projection.MediaProjection;
import com.statsmind.scrcpy.wrappers.MediaProjectionManager;
import org.java_websocket.WebSocket;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

public class WebSocketConnection extends Connection implements AudioEncoder.OnAudioEncodeListener {
    private static final byte[] MAGIC_BYTES_INITIAL = "scrcpy_initial".getBytes(StandardCharsets.UTF_8);
    private static final byte[] MAGIC_BYTES_MESSAGE = "scrcpy_message".getBytes(StandardCharsets.UTF_8);
    private static final byte[] DEVICE_NAME_BYTES = Device.getDeviceName().getBytes(StandardCharsets.UTF_8);
    private final WebSocketServer webSocketServer;
    private final HashSet<WebSocket> sockets = new HashSet<>();
    private ScreenEncoder screenEncoder;

    public WebSocketConnection(Options options, VideoSettings videoSettings, WebSocketServer webSocketServer) {
        super(options, videoSettings);
        this.webSocketServer = webSocketServer;
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    public static DeviceInfoMessage getDeviceInfo() {
        DeviceInfoMessage msg = new DeviceInfoMessage();
        msg.setDeviceName(Device.getDeviceName());

        int[] displayIds = Device.getDisplayIds();
        for (int displayId : displayIds) {
            DeviceInfoMessage.Display display = new DeviceInfoMessage.Display();

            DisplayInfo displayInfo = Device.getDisplayInfo(displayId);
            display.setDisplayInfo(displayInfo);

            WebSocketConnection connection = WebSocketServer.getConnectionForDisplay(displayId);
            if (connection == null) {
                display.setConnectionsCount(0);
            } else {
                display.setConnectionsCount(connection.sockets.size());
                display.setScreenInfo(connection.getDevice().getScreenInfo());
                display.setVideoSettings(connection.getVideoSettings());
            }

            msg.getDisplays().add(display);
        }

        MediaCodecInfo[] encoders = ScreenEncoder.listEncoders();
        if (encoders != null && encoders.length > 0) {
            for (MediaCodecInfo encoder : encoders) {
                msg.getEncoderNames().add(encoder.getName());
            }
        }

        return msg;
    }

    public void join(WebSocket webSocket, VideoSettings videoSettings) {
        sockets.add(webSocket);

        boolean changed = setVideoSettings(videoSettings);
        webSocketServer.broadcastDeviceInfo();
        if (!Device.isScreenOn()) {
            controller.turnScreenOn();
        }

        if (screenEncoder == null || !screenEncoder.isAlive()) {
            Ln.d("First connection. Start new encoder.");
            device.setRotationListener(this);

            screenEncoder = new ScreenEncoder(videoSettings);
            screenEncoder.start(device, this);
        } else {
            if (!changed) {
                if (this.streamInvalidateListener != null) {
                    /**
                     * 这块逻辑还要研究一下
                     */
                    streamInvalidateListener.onStreamInvalidate();
                }
            }
        }

        try {
            MediaProjectionManager mediaProjectionManager = Device.getMediaProjectionManager();
            for (Method method : mediaProjectionManager.getManager().getClass().getMethods()) {
                Ln.e("media projection method=" + method.getName());

                for (Class<?> parameterType : method.getParameterTypes()) {
                    Ln.e(" param = " + parameterType);
                }
            }

            AudioSettings audioSettings = AudioSettings.createDefault();
            AudioEncoder audioEncoder = new AudioEncoder(audioSettings);
            audioEncoder.setOnAudioEncodeListener(this);
            audioEncoder.prepareEncoder();

            AudioRecord audioRecord = AudioEncoder.getAudioRecord(audioSettings);
//        new AudioProcessor(audioRecord, audioSettings).start();
        } catch (Exception ex) {
            Ln.e(ex.getMessage(), ex);
        }
    }

//    public static ByteBuffer deviceMessageToByteBuffer(DeviceMessage msg) {
//        ByteBuffer buffer = ByteBuffer.wrap(msg.writeToByteArray(MAGIC_BYTES_MESSAGE.length));
//        buffer.put(MAGIC_BYTES_MESSAGE);
//        buffer.rewind();
//        return buffer;
//    }

    public void leave(WebSocket webSocket) {
        Ln.i("WebSocketConnection::leave");
        sockets.remove(webSocket);
        if (sockets.isEmpty()) {
            Ln.d("Last client has left");
            this.release();
        }

        webSocketServer.broadcastDeviceInfo();
    }

//    public static void sendInitialInfo(ByteBuffer initialInfo, WebSocket webSocket, int clientId) {
//        initialInfo.position(initialInfo.capacity() - 4);
//        initialInfo.putInt(clientId);
//        initialInfo.rewind();
//
//        ByteBuffer byteBuffer = ByteBuffer.allocate(1 + initialInfo.remaining());
//        byteBuffer.put((byte) ControlMessage.TYPE_DEVICE_INFO);
//        byteBuffer.put(initialInfo);
//        byteBuffer.flip();
//
//        webSocket.send(byteBuffer);
//    }

//    public void sendDeviceMessage(DeviceMessage msg) {
//        ByteBuffer buffer = deviceMessageToByteBuffer(msg);
//
//        ByteBuffer byteBuffer = ByteBuffer.allocate(1 + buffer.remaining());
//        byteBuffer.put((byte)ControlMessage.TYPE_DEVICE_MESSAGE);
//        byteBuffer.put(buffer);
//        byteBuffer.flip();
//
//        send(byteBuffer);
//    }

    @Override
    void send(ByteBuffer data) {
        if (sockets.isEmpty()) {
            return;
        }

        synchronized (sockets) {
            for (WebSocket webSocket : sockets) {
                WebSocketServer.SocketInfo info = webSocket.getAttachment();
                if (!webSocket.isOpen() || info == null) {
                    continue;
                }

                webSocket.send(data);
            }
        }
    }

    @Override
    public boolean hasConnections() {
        return !sockets.isEmpty();
    }

    @Override
    public void close() throws Exception {
        // 暂时没什么用，这个用于客户端主动断开连接
    }

    public VideoSettings getVideoSettings() {
        return videoSettings;
    }

    public Controller getController() {
        return controller;
    }

    public Device getDevice() {
        return device;
    }

    public void onRotationChanged(int rotation) {
        super.onRotationChanged(rotation);
        webSocketServer.broadcastDeviceInfo();
    }

    private void release() {
        WebSocketServer.releaseConnectionForDisplay(this.videoSettings.getDisplayId());
        // encoder will stop itself after checking .hasConnections()
    }

    @Override
    public void onAudioEncode(ByteBuffer bb, MediaCodec.BufferInfo bi) {
        Ln.w("audio stream!");

        ByteBuffer buffer = ByteBuffer.allocate(bb.remaining() + 1);
        buffer.put((byte)ControlMessage.TYPE_AUDIO_STREAM);
        buffer.put(bb);

        send(buffer);
    }
}
