package com.statsmind.scrcpy.client;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;

public class WebSocketClientWrapper extends WebSocketClient {
    private Callback callback;


    public WebSocketClientWrapper(URI serverURI, Callback callback) {
        super(serverURI);
        this.setCallback(callback);
    }

    public WebSocketClientWrapper(URI serverURI) {
        this(serverURI, null);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        if (callback != null) {
            callback.onWebSocketOpen(handshake);
        }
    }

    @Override
    public void onMessage(String message) {
        if (callback != null) {
            callback.onWebSocketMessage(message);
        }
    }

    @Override
    public void onMessage(ByteBuffer byteBuffer) {
        if (callback != null) {
            callback.onWebSocketMessage(byteBuffer);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (callback != null) {
            callback.onWebSocketClose(code, reason, remote);
        }
    }

    @Override
    public void onClosing(int code, String reason, boolean remote) {
        if (callback != null) {
            callback.onWebSocketClosing(code, reason, remote);
        }
    }

    @Override
    public void onCloseInitiated(int code, String reason) {
        if (callback != null) {
            callback.onWebSocketCloseInitiated(code, reason);
        }
    }

    @Override
    public void onError(Exception ex) {
        if (callback != null) {
            callback.onWebSocketError(ex);
        }
    }

    public interface Callback {
        void onWebSocketOpen(ServerHandshake handshake);

        void onWebSocketMessage(ByteBuffer byteBuffer);

        void onWebSocketClose(int code, String reason, boolean remote);

        void onWebSocketError(Exception ex);

        default void onWebSocketMessage(String message) {
        }

        default void onWebSocketClosing(int code, String reason, boolean remote) {
        }

        default void onWebSocketCloseInitiated(int code, String reason) {
        }
    }

}
