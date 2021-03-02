package io.cappuccino.server;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

public final class Main {

    private Main() { }

    public static void main(String[] args) {
        final WebSocketServer server = new WebSocketServer(new InetSocketAddress(8000)) {
            @Override
            public void onOpen(WebSocket conn, ClientHandshake handshake) {
                System.out.println("Got a connection: " + conn.getRemoteSocketAddress());
            }

            @Override
            public void onClose(WebSocket conn, int code, String reason, boolean remote) { }

            @Override
            public void onMessage(WebSocket conn, String message) {
                System.out.println("Got a message from " + conn.getRemoteSocketAddress() + ": " + message);
            }

            @Override
            public void onError(WebSocket conn, Exception ex) {
                System.out.println("Got an error from " + conn.getRemoteSocketAddress() + ": " + ex);
            }

            @Override
            public void onStart() {
                System.out.println("Listening for connections.");
            }
        };

        server.start();
    }
}
