package com.statsmind;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

public class MyClass {
    public static void main(String[] args) throws URISyntaxException, InterruptedException {
        WebSocketClient webSocketClient = new WebSocketClient(new URI("ws://192.168.10.13:18888/ws")) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                System.out.println("onOpen");
            }

            @Override
            public void onMessage(String message) {
                System.out.println("onMessage");
            }

            @Override
            public void onMessage(ByteBuffer buffer) {
                System.out.println("onBinaryMessage");
            }


            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("onClose");
            }

            @Override
            public void onError(Exception ex) {
                System.out.println("onError");
            }
        };
        webSocketClient.connect();

        Thread.sleep(10000);
    }
}