package com.statsmind.scrcpy.websocket;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.system.ErrnoException;
import android.system.Os;
import com.statsmind.scrcpy.Binary;
import com.statsmind.scrcpy.DesktopConnection;
import com.statsmind.scrcpy.Ln;
import com.statsmind.scrcpy.Options;
import lombok.Data;
import lombok.SneakyThrows;
import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;

import java.io.FileDescriptor;
import java.io.InterruptedIOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.statsmind.scrcpy.Binary.byteBufferToHex;

public class WebSocketProxy extends org.java_websocket.server.WebSocketServer {
    // 这个长度是 MediaCodec 的输入缓冲区最大长度
    private final static int MAX_PACKET_SIZE = 1 << 21;

    private Options options;
    private LocalServerSocket localServerSocket = null;
    private LocalSocket localVideoSocket = null;
    private LocalSocket localAudioSocket = null;
    private LocalSocket localControlSocket = null;
    private Map<Short, WebSocket> remoteWebSockets = new HashMap<>();
    private final ExecutorService threadPool;
    private boolean isRunning = true;
    private final Semaphore semaphore = new Semaphore(1, true);

    private ByteBuffer SPS = null;
    private ByteBuffer PPS = null;

    @SneakyThrows
    public WebSocketProxy(Options options) {
        super(new InetSocketAddress("0.0.0.0", options.getServerPort()));
        setReuseAddr(true);

        this.options = options;
        this.threadPool = Executors.newFixedThreadPool(3);
        this.semaphore.acquire(1);
    }

    @SneakyThrows
    @Override
    public void start() {
        super.start();
        /**
         * 等待服务启动
         */
        if (!semaphore.tryAcquire(10, TimeUnit.SECONDS)) {
            throw new RuntimeException("websocket server is failed");
        }
    }

    @SneakyThrows
    @Override
    public void stop() {
        this.isRunning = false;
        this.threadPool.awaitTermination(2, TimeUnit.SECONDS);

        super.stop();
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake handshake) {
        if (!webSocket.isOpen()) {
            return;
        }

        short clientId = getNextClientId();
        if (clientId == -1) {
            // 已经没有 clientId (max=264*264) 可以分配了，
            Ln.e("连接数超过最大限制");
            webSocket.close(CloseFrame.TRY_AGAIN_LATER);
            return;
        }

        SocketInfo info = new SocketInfo(clientId);
        webSocket.setAttachment(info);

        remoteWebSockets.put(clientId, webSocket);
        Ln.i("new client is joined");
    }

    @Override
    public void onClose(WebSocket webSocket, int code, String reason, boolean remote) {
        webSocket.setAttachment(new SocketInfo((short)0));
    }

    @Override
    public void onMessage(WebSocket webSocket, String message) {
        // 广播到其他的客户端
        for (WebSocket connection : this.getConnections()) {
            try {
                if (connection.isOpen() && connection != webSocket) {
                    connection.send(message);
                }
            } catch (Exception ignored) {}
        }
    }

    @SneakyThrows
    @Override
    public void onMessage(WebSocket webSocket, ByteBuffer message) {
        super.onMessage(webSocket, message);

        if (localControlSocket != null) {
            // 降信令数据转发到本地信令通道
            Ln.i("received control message from remote: " + message.get());
            message.rewind();

            Os.write(localControlSocket.getFileDescriptor(), message);
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
        Ln.i("Websocket server is ready");
        try {
            String socketName = DesktopConnection.getSocketName(options.getScid());
            Ln.d("Creating local server socket " + socketName);

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
        } catch (Exception ex) {
            Ln.e(ex.getMessage(), ex);
        }
    }

    private Runnable localSocketDataCapture(LocalSocket localSocket, String socketName) {
        return () -> {
            FileDescriptor fd = localSocket.getFileDescriptor();
            byte[] buffer = new byte[MAX_PACKET_SIZE];

            try {
                while (isRunning) {
                    int len = Os.read(fd, buffer, 1, MAX_PACKET_SIZE - 1);
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
                            // Ln.d("video captured " + len + " bytes, broadcast to " + this.getConnections().size() + " clients");
                            buffer[0] = (byte) 106;
                            ByteBuffer data = ByteBuffer.wrap(buffer, 0, len + 1);

                            if (buffer[1] == 0 && buffer[2] == 0 && buffer[3] == 0 && buffer[4] == 1) {
                                 if (buffer[5] == 0x67) {
                                     SPS = Binary.duplicate(data);
                                     Ln.w("Detected SPS=" + byteBufferToHex(SPS));
                                 } else if (buffer[5] == 0x68) {
                                     PPS = Binary.duplicate(data);
                                     Ln.w("Detected PPS=" + byteBufferToHex(PPS));
                                 }
                            }

                            if (SPS == null) {
                                continue;
                            }

                            for (WebSocket connection : this.getConnections()) {
                                SocketInfo socketInfo = connection.getAttachment();
                                if (socketInfo == null) {
                                    continue;
                                }

                                if (!socketInfo.isSpsSent) {
                                    Ln.w("Sent SPS to client, clientId=" + socketInfo.clientId + ", " + byteBufferToHex(SPS));
                                    connection.send(SPS);
                                    if (PPS != null) {
                                        Ln.w("Sent PPS to client, clientId=" + socketInfo.clientId + ", " + byteBufferToHex(PPS));
                                        connection.send(PPS);
                                    }

                                    socketInfo.isSpsSent = true;
                                }

                                if (socketInfo.isSpsSent) {
                                    connection.send(data);
                                }
                            }

                        } else if ("audio".equals(socketName)) {
                            buffer[0] = (byte) 107;
                            broadcast(ByteBuffer.wrap(buffer, 0, len + 1));
                        } else {
                            broadcast(ByteBuffer.wrap(buffer, 1, len));
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

    private short getNextClientId() {
        for (short i = 0; i < Short.MAX_VALUE; ++i) {
            if (!remoteWebSockets.containsKey(i)) {
                return i;
            }
        }

        return -1;
    }

    @Data
    public static class SocketInfo {
        private short clientId;
        private boolean isSpsSent = false;

        public SocketInfo(short clientId) {
            this.clientId = clientId;
        }
    }
}
