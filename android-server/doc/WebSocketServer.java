package com.statsmind.scrcpy;

import lombok.Data;
import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

public class WebSocketServer extends org.java_websocket.server.WebSocketServer {
    private static final String PID_FILE_PATH = "/data/local/tmp/ws_scrcpy.pid";
    private static final HashMap<Integer, WebSocketConnection> STREAM_BY_DISPLAY_ID = new HashMap<>();
    private final Options options;

    public WebSocketServer(Options options) {
        super(new InetSocketAddress(options.getListenOnAllInterfaces() ? "0.0.0.0" : "127.0.0.1", options.getPortNumber()));
        this.options = options;
        unlinkPidFile();
    }

    private static synchronized void joinStreamForDisplayId(
            WebSocket webSocket, VideoSettings videoSettings, Options options, int displayId, WebSocketServer webSocketServer) {
        SocketInfo socketInfo = webSocket.getAttachment();
        WebSocketConnection connection = STREAM_BY_DISPLAY_ID.get(displayId);
        if (connection == null) {
            connection = new WebSocketConnection(options, videoSettings, webSocketServer);
            STREAM_BY_DISPLAY_ID.put(displayId, connection);
        }

        socketInfo.setConnection(connection);
        connection.join(webSocket, videoSettings);
    }

    private static void unlinkPidFile() {
        try {
            File pidFile = new File(PID_FILE_PATH);
            if (pidFile.exists()) {
                if (!pidFile.delete()) {
                    Ln.e("Failed to delete PID file");
                }
            }
        } catch (Exception e) {
            Ln.e("Failed to delete PID file:", e);
        }
    }

    private static void writePidFile() {
        File file = new File(PID_FILE_PATH);
        FileOutputStream stream;
        try {
            stream = new FileOutputStream(file, false);
            stream.write(Integer.toString(android.os.Process.myPid()).getBytes(StandardCharsets.UTF_8));
            stream.close();
        } catch (IOException e) {
            Ln.e(e.getMessage());
        }
    }

    public static WebSocketConnection getConnectionForDisplay(int displayId) {
        return STREAM_BY_DISPLAY_ID.get(displayId);
    }

    public static void releaseConnectionForDisplay(int displayId) {
        STREAM_BY_DISPLAY_ID.remove(displayId);
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake handshake) {
        Ln.i("WebSocketServer::onOpen");

        if (webSocket.isOpen()) {
            short clientId = SocketInfo.getNextClientId();
            if (clientId == -1) {
                // 已经没有 clientId (max=264*264) 可以分配了，
                Ln.e("连接数超过最大限制");
                webSocket.close(CloseFrame.TRY_AGAIN_LATER);
                return;
            }

            SocketInfo info = new SocketInfo(clientId);
            info.stage = SocketInfo.Stage.AUTHORIZED;

            webSocket.setAttachment(info);

            /*
             * 作为握手协议的第一部分，服务器向客户端发送连接及设备的信息，这块后续可以加认证
             * 需要注意的是这时还没有绑定到视频和音频流
             */
            DeviceInfoMessage deviceInfo = WebSocketConnection.getDeviceInfo();
            deviceInfo.setClientId(clientId);

            webSocket.send(deviceInfo.toBuffer());
        }
    }

    @Override
    public void onClose(WebSocket webSocket, int code, String reason, boolean remote) {
        Ln.i("WebSocketServer::onClose");
        // 目前暂时不支持 FilePush
        // FilePushHandler.cancelAllForConnection(webSocket);
        SocketInfo socketInfo = webSocket.getAttachment();
        if (socketInfo != null) {
            WebSocketConnection connection = socketInfo.getConnection();
            if (connection != null) {
                // 这一步要向所有客户端发送消息，异常内部已经捕获
                connection.leave(webSocket);
            }
            socketInfo.release();
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, String message) {
        /*
         * 发送文本消息是很好玩的东西，多人可以同屏操作？
         */
        SocketInfo socketInfo = webSocket.getAttachment();
        if (socketInfo != null) {
            broadcastTextMessage(message, socketInfo.getId());
        }

        String address = webSocket.getRemoteSocketAddress().getAddress().getHostAddress();
        Ln.i("?  Client from " + address + " says: \"" + message + "\"");
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteBuffer message) {
        Ln.i("WSServer:onMessage: " + ByteUtil.bytesToHex(message.array()));

        SocketInfo socketInfo = webSocket.getAttachment();
        if (socketInfo == null) {
            Ln.e("No info attached to connection");
            return;
        }

        WebSocketConnection connection = socketInfo.getConnection();
        String address = webSocket.getRemoteSocketAddress().getAddress().getHostAddress();
        ControlMessage controlMessage = ControlMessage.parseEvent(message);

        if (controlMessage == null) {
            Ln.w("?  Client from " + address + " sends bytes: " + message);
            return;
        }

        Ln.d("WSServer:onMessage: " + controlMessage);

        // 不支持
//        if (controlMessage.getType() == ControlMessage.TYPE_PUSH_FILE) {
//            FilePushHandler.handlePush(webSocket, controlMessage);
//            return;
//        }

        /*
         * 这个作为握手协议的第二部分，客户端在收到 DeviceInfo 后需要传给服务器 TYPE_CHANGE_STREAM_PARAMETERS 消息以建立流媒体连接
         * 按照 WebRTC 做法，应该返回另一个socket通道给客户端，这里简化处理了
         */
        if (controlMessage.getType() == ControlMessage.TYPE_UPDATE_VIDEO_SETTINGS) {
            VideoSettings videoSettings = ((UpdateVideoSettingsMessage)controlMessage).getVideoSettings();
            Ln.i("VideoSettings: " + videoSettings);

            int displayId = videoSettings.getDisplayId();
            if (connection != null) {
                if (connection.getVideoSettings().getDisplayId() != displayId) {
                    connection.leave(webSocket);
                }
            }

            joinStreamForDisplayId(webSocket, videoSettings, options, displayId, this);
            socketInfo.stage = SocketInfo.Stage.JOINED;
        } else if (connection != null && socketInfo.stage == SocketInfo.Stage.JOINED) {
            /**
             * 只有建立流媒体连接后才能发送除TYPE_CHANGE_STREAM_PARAMETERS之外其他消息
             */
            Controller controller = connection.getController();
            controller.handleEvent(controlMessage);
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception ex) {
        Ln.e("WebSocketServer::onError", ex);
//        if (webSocket != null) {
//            // some errors like port binding failed may not be assignable to a specific websocket
//            FilePushHandler.cancelAllForConnection(webSocket);
//        }
        /**
         * 系统级别错误，关闭服务
         */
        if (ex instanceof BindException) {
            System.exit(1);
        }
    }

    @Override
    public void onStart() {
        Ln.i("Server started! " + this.getAddress().toString());
        this.setConnectionLostTimeout(0);
        this.setConnectionLostTimeout(100);
        writePidFile();
    }

    public void broadcastDeviceInfo() {
        Ln.i("WebSocketServer::broadcastDeviceInfo");

        Collection<WebSocket> webSockets = this.getConnections();
        if (webSockets.isEmpty()) {
            return;
        }

        DeviceInfoMessage deviceInfo = WebSocketConnection.getDeviceInfo();
        for (WebSocket webSocket : webSockets) {
            SocketInfo socketInfo = webSocket.getAttachment();
            if (!webSocket.isOpen() || socketInfo == null) {
                continue;
            }

            deviceInfo.setClientId(socketInfo.getId());
            try {
                webSocket.send(deviceInfo.toBuffer());
            } catch (Exception ex) {
                Ln.e(ex.getMessage(), ex);
            }
        }
    }

    public void broadcastTextMessage(String text, short fromClientId) {
        Ln.i("WebSocketServer::broadcastTextMessage");

        Collection<WebSocket> webSockets = this.getConnections();
        if (webSockets.isEmpty()) {
            return;
        }

        TextMessage textMessage = new TextMessage();
        textMessage.setText(text);

        for (WebSocket webSocket : webSockets) {
            SocketInfo socketInfo = webSocket.getAttachment();
            if (!webSocket.isOpen() || socketInfo == null || Objects.equals(socketInfo.getId(), fromClientId)) {
                continue;
            }

            textMessage.setClientId(socketInfo.getId());
            try {
                webSocket.send(textMessage.toBuffer());
            } catch (Exception ex) {
                Ln.e(ex.getMessage(), ex);
            }
        }
    }

    @Data
    public static final class SocketInfo {
        private static final HashSet<Short> INSTANCES_BY_ID = new HashSet<>();
        private final short id;
        private Stage stage;
        private WebSocketConnection connection;

        SocketInfo(short id) {
            this.id = id;
            INSTANCES_BY_ID.add(id);
        }

        public static short getNextClientId() {
            short nextClientId = 0;
            while (INSTANCES_BY_ID.contains(++nextClientId)) {
                if (nextClientId == Short.MAX_VALUE) {
                    return -1;
                }
            }

            return nextClientId;
        }

        public void release() {
            INSTANCES_BY_ID.remove(id);
        }

        public enum Stage {
            AUTHORIZED, JOINED
        }
    }
}
