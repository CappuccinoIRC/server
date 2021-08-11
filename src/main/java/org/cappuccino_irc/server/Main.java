package org.cappuccino_irc.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Main {

    private static final Map<Integer, Client> CLIENT_MAP = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        final ServerSocket server = new ServerSocket(3001);
        System.out.println("Server is listening for connections at port 3001.");

        while (!server.isClosed()) {
            Socket clientSocket;

            while ((clientSocket = server.accept()) != null) {
                CLIENT_MAP.put(CLIENT_MAP.size() + 1, new Client(clientSocket));
            }
        }
    }

    private static final class Client implements Runnable {
        private final Socket socket;
        private final Thread thread;

        private InputStream in;
        private OutputStream out;

        public Client(Socket socket) {
            this.socket = socket;
            this.thread = new Thread(this);
            this.thread.start();
        }

        public InputStream getInputStream() throws IOException {
            if (in == null) {
                in = new EncapsulatedInputStream(socket.getInputStream());
            }

            return in;
        }

        public OutputStream getOutputStream() throws IOException {
            if (out == null) {
                out = new EncapsulatedOutputStream(socket.getOutputStream());
            }

            return out;
        }

        public void send(byte[] data) throws IOException {
            if (data == null || data.length == 0) {
                throw new IllegalArgumentException("parameter 'data' is either null or empty.");
            }

            socket.getOutputStream().write(data, 0, data.length);
        }

        @Override
        public String toString() {
            return "Client (address=" + socket.getInetAddress().getHostAddress()+ ", port=" + socket.getPort() + ")";
        }

        @Override
        public void run() {
            while (!socket.isClosed()) {
                try (InputStream in = this.getInputStream()) {
                    final Scanner scanner = new Scanner(in, StandardCharsets.UTF_8);

                    while (scanner.useDelimiter("\r\n\r\n").hasNext()) {
                        String data = scanner.useDelimiter("\r\n\r\n").next();
                        Matcher matcher = Pattern.compile("Connection: Upgrade").matcher(data);

                        if (matcher.find()) {
                            try {
                                System.out.println("[INFO] " + this + " protocol upgrade.");
                                handleProtocolUpgrade(this, data);
                            } catch (NoSuchAlgorithmException | IOException e) {
                                e.printStackTrace();
                            }
                        }

                        System.out.println("[INFO] " + this + " {\n" + Arrays.stream(data.split("\r\n")).map(s -> "\t" + s).collect(Collectors.joining("\n")) + "\n}");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleProtocolUpgrade(Client client, String data) throws NoSuchAlgorithmException, IOException {
            Matcher matcher = Pattern.compile("^GET").matcher(data);

            if (!matcher.find()) {
                throw new IOException("Protocol upgrade requires GET method.");
            }

            if (matcher.find()) {
                matcher = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);

                if (!matcher.find()) {
                    throw new IOException("Sec-WebSocket-Key is required.");
                }

                final StringBuilder sb = new StringBuilder();
                sb.append("HTTP/1.1 101 Switching Protocols\r\n");
                sb.append("Connection: Upgrade\r\n");
                sb.append("Upgrade: websocket\r\n");
                sb.append("Sec-WebSocket-Accept: ").append(Base64.getEncoder().encodeToString(
                        MessageDigest.getInstance("SHA-1").digest(
                                (matcher.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.UTF_8)
                        )
                ));
                sb.append("\r\n\r\n");

                byte[] response = sb.toString().getBytes(StandardCharsets.UTF_8);
                client.send(response);
            }
        }

        private static final class EncapsulatedInputStream extends InputStream {

            private final InputStream in;

            public EncapsulatedInputStream(InputStream in) {
                this.in = in;
            }

            @Override
            public int read() throws IOException {
                return in.read();
            }

            @Override
            public void close() {
                // Prevent closing the stream, because it closes the underlying socket.
            }
        }

        private static final class EncapsulatedOutputStream extends OutputStream {

            private final OutputStream out;

            public EncapsulatedOutputStream(OutputStream out) {
                this.out = out;
            }

            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }

            @Override
            public void close() {
                // Prevent closing the stream, because it closes the underlying socket.
            }
        }
    }
}
