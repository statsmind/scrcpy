package com.statsmind.scrcpy.websocket;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import com.statsmind.scrcpy.DesktopConnection;
import com.statsmind.scrcpy.Ln;
import com.statsmind.scrcpy.Options;
import lombok.Data;
import lombok.SneakyThrows;
import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;

import java.io.DataInputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class WebSocketProxy extends org.java_websocket.server.WebSocketServer {
    private final static int MAX_PACKET_SIZE = 1024 * 1024 * 8;

    private Options options;
    private LocalServerSocket localServerSocket = null;
    private LocalSocket localVideoSocket = null;
    private LocalSocket localAudioSocket = null;
    private LocalSocket localControlSocket = null;
    private Map<Short, WebSocket> remoteWebSockets = new HashMap<>();
    private final ExecutorService threadPool;
    private final Semaphore semaphore = new Semaphore(1, true);

    @SneakyThrows
    public WebSocketProxy(Options options) {
        super(new InetSocketAddress("0.0.0.0", 21708));
        setReuseAddr(true);

        this.options = options;
        this.threadPool = Executors.newFixedThreadPool(3);
        this.semaphore.acquire(1);
    }

    @SneakyThrows
    public void waitForReady() {
        semaphore.acquire(1);
    }

    @Override
    public void stop() {

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

    }

    @Override
    public void onMessage(WebSocket webSocket, String message) {

    }

    @SneakyThrows
    @Override
    public void onMessage(WebSocket webSocket, ByteBuffer message) {
        super.onMessage(webSocket, message);
        // 降信令数据转发到本地信令通道
        Ln.i("received control message from remote: " + message.get());
        message.rewind();

        localControlSocket.getOutputStream().write(message.array());
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (ex instanceof BindException) {
            Ln.e(ex.getMessage(), ex);
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

    private Runnable localSocketDataCapture(LocalSocket socket, String socketName) {
        return () -> {
            try (DataInputStream inputStream = new DataInputStream(socket.getInputStream())) {
                while (true) {
                    /*
                     * 预留第一个 byte 位放入控制头
                     */
                    byte[] buffer = new byte[MAX_PACKET_SIZE];
                    int len = inputStream.read(buffer, 1, MAX_PACKET_SIZE - 1);
                    if (len > 0) {
                        try {
                            if ("video".equals(socketName)) {
                                Ln.d("video captured " + len + " bytes, broadcast to " + this.getConnections().size() + " clients");
                                buffer[0] = (byte)106;
                                ByteBuffer newBuffer = ByteBuffer.wrap(buffer, 0, len + 1);
//                                broadcast(ByteBuffer.wrap(buffer, 0, len + 1));

                                for (WebSocket connection : this.getConnections()) {
                                    try {
                                        connection.send(newBuffer);
                                    } catch (Exception ex) {
                                        Ln.e(ex.getMessage(), ex);
                                    }
                                }
                            } else if ("audio".equals(socketName)) {
                                buffer[0] = (byte)107;
                                broadcast(ByteBuffer.wrap(buffer, 0, len + 1));
                            } else {
                                broadcast(ByteBuffer.wrap(buffer, 1, len));
                            }

                        } catch (Exception ex) {
                            Ln.e(ex.getMessage(), ex);
                        }
                    }
                }
            } catch (Exception ex) {
                Ln.e(ex.getMessage(), ex);
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

        public SocketInfo(short clientId) {
            this.clientId = clientId;
        }
    }
}
