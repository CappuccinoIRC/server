package org.cappuccino_irc.server;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public final class Main {

    private Main() { }

    public static void main(String[] args) {
        final WebSocketServer server = new WebSocketServer(new InetSocketAddress(8000)) {
            final List<WebSocket> sockets = new ArrayList<>();

            @Override
            public void onOpen(WebSocket socket, ClientHandshake handshake) {
                System.out.println("Got a connection: " + socket.getRemoteSocketAddress());

                sockets.add(socket);
            }

            @Override
            public void onClose(WebSocket socket, int code, String reason, boolean remote) { }

            @Override
            public void onMessage(WebSocket socket, String message) {
                System.out.println("Got a message from " + socket.getRemoteSocketAddress() + ": " + message);
            }

            @Override
            public void onError(WebSocket socket, Exception ex) {
                System.out.println("Got an error from " + socket.getRemoteSocketAddress() + ": " + ex);
            }

            @Override
            public void onStart() {
                System.out.println("Listening for connections.");
            }
        };

        server.start();
    }
}
