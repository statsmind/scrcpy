package com.genymobile.scrcpy;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.system.ErrnoException;
import android.system.Os;
import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 简化版 WebSocketServer, 负责建立与 DesktopConnection LocalSocket 的通道
 */
public class WebSocketProxy extends org.java_websocket.server.WebSocketServer {
    // 这个长度是 MediaCodec 的输入缓冲区最大长度
    private final static int MAX_PACKET_SIZE = 1 << 21;

    private Options options;
    private LocalServerSocket localServerSocket = null;
    private LocalSocket localVideoSocket = null;
    private LocalSocket localAudioSocket = null;
    private LocalSocket localControlSocket = null;
    private Set<Short> clientIds = new HashSet<>();
    private final ExecutorService threadPool;
    private boolean isRunning = true;
    private final Semaphore semaphore = new Semaphore(1, true);

    private byte[] spsFrame = null;

    public WebSocketProxy(Options options) {
        super(new InetSocketAddress("0.0.0.0", options.getWebsocketPort()));
        setReuseAddr(true);

        this.options = options;
        // 暂时不考虑摄像头问题，只有三个通道 video, audio, control
        this.threadPool = Executors.newFixedThreadPool(3);

        try {
            // 等待 LocalSocketServer 接受 video, audio, control 的连接
            this.semaphore.acquire(1);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void start() {
        super.start();
        /**
         * 等待服务启动
         */
        try {
            if (!semaphore.tryAcquire(10, TimeUnit.SECONDS)) {
                throw new RuntimeException("websocket server is failed");
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void stop() throws InterruptedException {
        this.isRunning = false;

        try {
            this.threadPool.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        super.stop();
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake handshake) {
        if (!webSocket.isOpen()) {
            return;
        }

        short clientId = getNextClientId();
        if (clientId == -1) {
            Ln.e("连接数超过最大限制");
            webSocket.close(CloseFrame.TRY_AGAIN_LATER);
            return;
        }

        SocketInfo info = new SocketInfo(clientId);
        webSocket.setAttachment(info);

        clientIds.add(clientId);
        Ln.i("New websocket client is accepted");
    }

    @Override
    public void onClose(WebSocket webSocket, int code, String reason, boolean remote) {
        SocketInfo socketInfo = webSocket.getAttachment();
        if (socketInfo != null) {
            clientIds.remove(socketInfo.getClientId());
            Ln.w("Websocket client #" + socketInfo.getClientId() + " has quit");
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, String message) {
        // 文本消息直接广播到其他的客户端
        for (WebSocket connection : this.getConnections()) {
            try {
                if (connection.isOpen() && connection != webSocket) {
                    connection.send(message);
                }
            } catch (Exception ex) {
                Ln.e(ex.getMessage(), ex);
            }
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteBuffer message) {
        super.onMessage(webSocket, message);

        if (localControlSocket != null && message.hasRemaining()) {
            // 接收到的都是信令数据，转发到本地信令通道
            Ln.d("Received control message from remote with type=" + message.get());
            message.rewind();

            try {
                IO.writeFully(localControlSocket.getFileDescriptor(), message);
            } catch (Exception ex) {
                // 信令通道出了问题，直接退出系统，由supervisor保护进程重启
                System.exit(1);
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        Ln.e(ex.getMessage(), ex);

        if (ex instanceof BindException) {
            System.exit(1);
        }
    }

    @Override
    public void onStart() {
        Ln.i("Websocket server is ready on 0.0.0.0:" + options.getWebsocketPort());
        try {
            this.waitForLocalSocketConnections();
        } catch (Exception ex) {
            Ln.e(ex.getMessage(), ex);
        }
    }

    private void waitForLocalSocketConnections() throws IOException {
        String socketName = DesktopConnection.getSocketName(options.getScid());
        Ln.i("Creating local server socket " + socketName);

        this.localServerSocket = new LocalServerSocket(socketName);
        this.semaphore.release(1);

        if (options.getVideo()) {
            localVideoSocket = localServerSocket.accept();
            threadPool.submit(localSocketDataCapture(localVideoSocket, "video"));
            Ln.i("Local video socket is connected");
        }

        if (options.getAudio()) {
            localAudioSocket = localServerSocket.accept();
            threadPool.submit(localSocketDataCapture(localAudioSocket, "audio"));
            Ln.i("Local audio socket is connected");
        }

        if (options.getVideo()) {
            localControlSocket = localServerSocket.accept();
            threadPool.submit(localSocketDataCapture(localControlSocket, "control"));
            Ln.i("Local control socket is connected");
        }
    }

    private Runnable localSocketDataCapture(LocalSocket localSocket, String socketName) {
        return () -> {
            FileDescriptor localSocketFD = localSocket.getFileDescriptor();
            byte[] buffer = new byte[MAX_PACKET_SIZE];

            try {
                while (isRunning) {
                    // 第一个字节保留给插入信令 type
                    int len = Os.read(localSocketFD, buffer, 1, MAX_PACKET_SIZE - 1);
                    if (len <= 0) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        continue;
                    }

                    try {
                        if ("video".equals(socketName)) {
                            dispatchVideoStream(buffer, len);
                        } else if ("audio".equals(socketName)) {
                            dispatchAudioStream(buffer, len);
                        } else {
                            dispatchControlStream(buffer, len);
                        }

                    } catch (Exception ex) {
                        Ln.e(ex.getMessage(), ex);
                    }
                }
            } catch (ErrnoException e) {
                throw new RuntimeException(e);
            } catch (InterruptedIOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private void dispatchVideoStream(byte[] buffer, int len) {
        // Ln.d("video captured " + len + " bytes, broadcast to " + this.getConnections().size() + " clients");
        if (buffer[1] == 0 && buffer[2] == 0 && buffer[3] == 0 && buffer[4] == 1 && buffer[5] == 0x67) {
            // 记录下 SPS 帧的数据，当客户端连接时，要发一次。如果有更新的时候也会记录新的。
            spsFrame = Binary.duplicate(buffer, 1, len);
            Ln.w("Detected SPS Frame = " + Binary.bytesToHex(spsFrame));
            return;
        }

        if (spsFrame == null) {
            // 到目前为止还没有收到过 SPS 帧，暂时不往客户端发送视频流，发了也解不了
            return;
        }

        ByteBuffer bufferWithTypeAndSPS = ByteBuffer.allocate(1 + len + spsFrame.length);
        bufferWithTypeAndSPS.put((byte)ControlMessage.TYPE_STREAM_VIDEO);
        bufferWithTypeAndSPS.put(spsFrame);
        bufferWithTypeAndSPS.put(buffer, 1, len);
        bufferWithTypeAndSPS.flip();

        broadcast(bufferWithTypeAndSPS);
//        for (WebSocket connection : this.getConnections()) {
//            SocketInfo socketInfo = connection.getAttachment();
//            if (socketInfo == null) {
//                continue;
//            }
//
//            // 如果客户端没发送过 SPS 帧，需要发一次。后面考虑能不能每隔1s发一次，用户快速重建视频流
//            if (!socketInfo.isSpsSent) {
//                connection.send(spsFrame);
//                socketInfo.isSpsSent = true;
//            }
//
//            // 已经发送过 SPS 帧，可以发送正常的视频流了
//            if (socketInfo.isSpsSent) {
//                connection.send(data);
//            }
//        }
    }

    private void dispatchAudioStream(byte[] buffer, int len) {
        ByteBuffer bufferWithType = ByteBuffer.allocate(1 + len);
        bufferWithType.put((byte)ControlMessage.TYPE_STREAM_AUDIO);
        bufferWithType.put(buffer, 0, len);

        broadcast(ByteBuffer.wrap(buffer, 0, 1 + len));
    }

    private void dispatchControlStream(byte[] buffer, int len) {
        broadcast(ByteBuffer.wrap(buffer, 1, len));
    }

    private short getNextClientId() {
        for (short i = 0; i < Short.MAX_VALUE; ++i) {
            if (!clientIds.contains(i)) {
                return i;
            }
        }

        return -1;
    }

    public static class SocketInfo {
        private short clientId;
        private boolean isSpsSent = false;

        public SocketInfo(short clientId) {
            this.clientId = clientId;
        }

        public short getClientId() {
            return clientId;
        }

        public boolean isSpsSent() {
            return isSpsSent;
        }
    }
}