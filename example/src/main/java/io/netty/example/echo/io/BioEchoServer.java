package io.netty.example.echo.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author lufengxiang
 * @since 2021/6/1
 **/
public class BioEchoServer {
    public static void main(String[] args) throws IOException {
        int port = 9000;
        ServerSocket ss = new ServerSocket(port);
        while (true) {
            final Socket socket = ss.accept();
            new Thread(new Runnable() {
                public void run() {
                    while (true) {
                        try {
                            BufferedInputStream in = new BufferedInputStream(
                                    socket.getInputStream());
                            byte[] buf = new byte[1024];
                            int len = in.read(buf); // read message from client
                            String message = new String(buf, 0, len);
                            BufferedOutputStream out = new BufferedOutputStream(
                                    socket.getOutputStream());
                            out.write(message.getBytes()); // echo to client
                            out.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }).start();
        }
    }
}
