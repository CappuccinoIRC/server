package org.cappuccino_irc.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Main {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        final ServerSocket server = new ServerSocket(3001);
        System.out.println("Server is listening for connections at port 3001.");

        Socket client;

        while ((client = server.accept()) != null) {
            final InputStream in = client.getInputStream();
            final OutputStream out = client.getOutputStream();
            final Scanner scanner = new Scanner(in, StandardCharsets.UTF_8);

            String data;

            while (scanner.hasNext()) {
                data = scanner.useDelimiter("\r\n\r\n").next();

                System.out.println("Request from " + (client.getInetAddress().getHostAddress() + ":" + client.getPort()) + " (\n" + Arrays.stream(data.split("\r\n")).map(s -> "\t" + s).collect(Collectors.joining("\n")) + "\n)");

                Matcher matcher = Pattern.compile("^GET").matcher(data);

                if (matcher.find()) {
                    matcher = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);

                    if (matcher.find()) {
                        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        final DataOutputStream daos = new DataOutputStream(baos);

                        daos.writeUTF("HTTP/1.1 101 Switching Protocols\r\n");
                        daos.writeUTF("Connection: Upgrade\r\n");
                        daos.writeUTF("Upgrade: websocket\r\n");
                        daos.writeUTF("Sec-WebSocket-Accept: " + Base64.getEncoder().encodeToString(
                                        MessageDigest.getInstance("SHA-1").digest(
                                                (matcher.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.UTF_8)
                                        )
                                )
                        );
                        daos.writeUTF("\r\n\r\n");
                        out.write(baos.toByteArray());
                    }
                }
            }
        }
    }
}
