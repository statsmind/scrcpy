package com.statsmind.scrcpy.client;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.media.*;
import android.util.Log;
import android.view.*;
import com.statsmind.scrcpy.*;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.media.AudioRecord.MetricsConstants.CHANNELS;
import static android.media.MediaFormat.*;
import static com.statsmind.scrcpy.Consts.TAG;

public class CodecDecoder implements View.OnTouchListener, View.OnKeyListener {
    protected WebSocketClient webSocketClient;
    protected MediaCodec videoCodec;
    protected MediaCodec audioCodec;
    protected DeviceInfoMessage deviceInfo;
    protected Activity activity;
    protected SurfaceView surfaceView;
    protected AudioTrack audioTrack;
    protected boolean hasSentVideoUpdateRequest = false;


    protected WebSocketClientWrapper.Callback webSocketCallback = new WebSocketClientWrapper.Callback() {

        @Override
        public void onWebSocketOpen(ServerHandshake handshake) {
            Ln.i("web socket is connected.");
        }

        @SuppressLint("WrongConstant")
        @Override
        public void onWebSocketMessage(ByteBuffer buffer) {
            int messageType = buffer.get();
            switch (messageType) {
                case ControlMessage.TYPE_VIDEO_STREAM:
                    processVideoStream(buffer);
                    break;
                case ControlMessage.TYPE_AUDIO_STREAM:
                    processAudioStream(buffer);
                    break;
                default:
                    buffer.rewind();
                    processControlMessage(buffer);
            }
        }

        private void processControlMessage(ByteBuffer buffer) {
            onControlMessage(ControlMessage.parseEvent(buffer));
        }

        @Override
        public void onWebSocketClose(int code, String reason, boolean remote) {
            Ln.e("websocket is closed");
        }

        @Override
        public void onWebSocketError(Exception ex) {
            Ln.e(ex.getMessage(), ex);
        }
    };

    public CodecDecoder(Activity activity, SurfaceView surfaceView, Surface surface, AudioTrack audioTrack) {
        this("ws://192.168.10.13:18888/ws", activity, surfaceView, surface, audioTrack);
    }

    @SuppressLint("ClickableViewAccessibility")
    public CodecDecoder(String serverURL, Activity activity, SurfaceView surfaceView, Surface surface, AudioTrack audioTrack) {
        try {
            this.webSocketClient = new WebSocketClientWrapper(new URI(serverURL), webSocketCallback);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        this.activity = activity;

        this.surfaceView = surfaceView;
//        this.surfaceView.setOnTouchListener(this);
//        this.surfaceView.setOnKeyListener(this);

        this.videoCodec = VideoCodec.setupVideoCodec(surface, surfaceView.getWidth(), surfaceView.getHeight());
        this.audioCodec = AudioCodec.setupAudioCodec();

        this.audioTrack = audioTrack;
    }

    protected void onControlMessage(ControlMessage message) {
        Ln.i("receive message: " + message);

        if (message instanceof DeviceInfoMessage) {
            onDeviceInfoMessage((DeviceInfoMessage) message);
        }
    }

    @SuppressLint("WrongConstant")
    protected void onDeviceInfoMessage(DeviceInfoMessage message) {
        if (message.getDisplays() == null || message.getDisplays().isEmpty()) {
            return;
        }

        this.deviceInfo = message;

        DisplayInfo displayInfo = this.deviceInfo.getDisplays().get(0).getDisplayInfo();
        if (isLandScrape(displayInfo.getRotation())) {
            activity.setRequestedOrientation(SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }

        if (!hasSentVideoUpdateRequest) {
            hasSentVideoUpdateRequest = true;

            VideoSettings videoSettings = new VideoSettings();
            Ln.i("set screen size to width=" + surfaceView.getWidth() + ", height=" + surfaceView.getHeight());

            videoSettings.setBounds(surfaceView.getWidth(), surfaceView.getHeight());
            videoSettings.setDisplayId(displayInfo.getDisplayId());

            UpdateVideoSettingsMessage updateVideoSettingsMessage = new UpdateVideoSettingsMessage();
            updateVideoSettingsMessage.setVideoSettings(videoSettings);

            webSocketClient.send(updateVideoSettingsMessage.toBuffer());
        }
    }

    public void setOutputSurface(Surface surface) {
        videoCodec.setOutputSurface(surface);
    }

    public void close() {
//        this.queueProcessFuture.cancel(true);
//        this.queueProcessFuture = null;
    }

    public void start() {
        try {
            this.videoCodec.start();
            Ln.i("Video codec has been started");
        } catch (Exception ex) {
            Ln.e(ex.getMessage(), ex);
        }

        try {
            this.audioCodec.start();
            Ln.i("Audio codec has been started");
        } catch (Exception ex) {
            Ln.e(ex.getMessage(), ex);
        }

        Ln.i("Connecting to server");
        this.webSocketClient.connect();
    }

    public void reconnect() {
        this.webSocketClient.reconnect();
    }

    public void processAudioStream(ByteBuffer buffer) {
        try {
            processAudioStream(buffer, buffer.remaining());
        } catch (Exception ex) {
            Ln.e(ex.getMessage(), ex);
        }
    }

    public void processVideoStream(ByteBuffer data) {
        try {
            processVideoStream(data, data.remaining());
        } catch (Exception ex) {
            Ln.e(ex.getMessage(), ex);
        }
    }

    long latestTimestamp = System.currentTimeMillis();

    boolean foundIDSFrame = false;

    private void processVideoStream(ByteBuffer data, int dataLength) {
        if (data != null) {
            int inputBufferIndex = videoCodec.dequeueInputBuffer(1);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = videoCodec.getInputBuffer(inputBufferIndex);
                if (null != inputBuffer) {
                    inputBuffer.clear();
                    inputBuffer.put(data);

                    videoCodec.queueInputBuffer(inputBufferIndex, 0, dataLength, System.currentTimeMillis(), 0);
                }
            } else {
                Ln.e("video input buffer index = " + inputBufferIndex);
            }
        }

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = videoCodec.dequeueOutputBuffer(bufferInfo, 1);
        while (outputBufferIndex >= 0) {
            videoCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = videoCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
    }

    private void processAudioStream(ByteBuffer data, int dataLength) {
        Ln.i("incoming audio data");
        if (data != null) {
            int inputBufferIndex = audioCodec.dequeueInputBuffer(1);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = audioCodec.getInputBuffer(inputBufferIndex);
                if (null != inputBuffer) {
                    inputBuffer.clear();
                    while (data.hasRemaining()) {
                        inputBuffer.put(data);
                    }

                    audioCodec.queueInputBuffer(inputBufferIndex, 0, dataLength, 0, 0);
                }
            } else {
                Ln.e("audio input buffer not available");
            }
        }

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = audioCodec.dequeueOutputBuffer(bufferInfo, 1);
        Ln.i("outputBufferIndex=" + outputBufferIndex);
        if (outputBufferIndex < 0) {
            audioCodec.flush();
            return;
        }

        while (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = audioCodec.getOutputBuffer(outputBufferIndex);
            Ln.i("send data to audio track");
            if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                audioTrack.write(outputBuffer, outputBuffer.remaining(), AudioTrack.WRITE_NON_BLOCKING);
            } else {
                Ln.i("audiotrack is not ready");
            }

            audioCodec.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = audioCodec.dequeueOutputBuffer(bufferInfo, 0);
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (this.deviceInfo == null) {
            return true;
        }

        DisplayInfo displayInfo = this.deviceInfo.getDisplays().get(0).getDisplayInfo();
        Size screenSize = displayInfo.getSize();

        int viewWidth = surfaceView.getWidth();
        int viewHeight = surfaceView.getHeight();

        for (int pointerIndex = 0; pointerIndex < event.getPointerCount(); ++pointerIndex) {
            InjectTouchEventMessage message = new InjectTouchEventMessage();

            message.setAction((byte)event.getAction());
            message.setPointerId(event.getPointerId(pointerIndex));
            message.setPressure((short)event.getPressure());
            message.setButtons(event.getActionButton());

            float x = event.getX(pointerIndex) * screenSize.getWidth() / viewWidth;
            float y = event.getY(pointerIndex) * screenSize.getHeight() / viewHeight;

            message.setPosition(new Position((int) x, (int) y, screenSize.getWidth(), screenSize.getHeight()));
            webSocketClient.send(message.toBuffer());
        }

        return true;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        InjectKeyCodeMessage message = new InjectKeyCodeMessage();
        message.setAction(event.getAction());
        message.setKeycode(event.getKeyCode());
        message.setRepeat(event.getRepeatCount());
        message.setMetaState(event.getMetaState());

        webSocketClient.send(message.toBuffer());
        return true;
    }

    public static boolean isLandScrape(int rotation) {
        int orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

        boolean isLandscape;// 是横屏

        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
            // 设备的自然方向是纵向
            if (rotation == Surface.ROTATION_0) {
                // 屏幕的实际方向也是纵向
                orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                isLandscape = false;
            } else {
                // 屏幕的实际方向是横向
                orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                isLandscape = true;
            }
        } else {
            // 设备的自然方向是横向
            if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
                // 屏幕的实际方向也是横向
                orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                isLandscape = true;
            } else {
                // 屏幕的实际方向是纵向
                orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                isLandscape = false;
            }
        }
//        setRequestedOrientation(orientation);// 设置屏幕方向
        return isLandscape;
    }
}
