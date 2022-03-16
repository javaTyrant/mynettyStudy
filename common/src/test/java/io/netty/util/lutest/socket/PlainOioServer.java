package io.netty.util.lutest.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * @author lufengxiang
 * @since 2022/3/2
 **/
public class PlainOioServer {
    public static void main(String[] args) throws IOException {
        server(8080);
    }

    public static void server(int port) throws IOException {
        final ServerSocket socket = new ServerSocket(port);
        try {
            for (; ; ) {
                final Socket clientSocket = socket.accept();
                System.out.println("Accepted connection from " + clientSocket);
                new Thread(() -> {
                    OutputStream out;
                    try {
                        out = clientSocket.getOutputStream();
                        out.write("hi!\r\n".getBytes(StandardCharsets.UTF_8));
                        out.flush();
                        //clientSocket.close();
                    } catch (IOException ignore) {

                    }
                }).start();
            }
        } catch (IOException ignore) {

        }
    }
}
