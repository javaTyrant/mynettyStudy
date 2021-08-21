package io.netty.example.echo.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author lufengxiang
 * @since 2021/6/1
 **/
public class NioEchoServer {
    public static void main(String[] args) throws IOException {
        //打开服务端的流
        ServerSocketChannel ssChannel = ServerSocketChannel.open();
        int port = 9001;
        //bind
        ssChannel.bind(new InetSocketAddress(port));
        //open Selector
        Selector selector = Selector.open();
        //config
        ssChannel.configureBlocking(false);
        //服务器流注册
        ssChannel.register(selector, SelectionKey.OP_ACCEPT); //注册监听连接请求
        //
        while (true) {
            selector.select();//阻塞 直到某个channel注册的事件被触发
            Set<SelectionKey> keys = selector.selectedKeys();
            for (SelectionKey key : keys) {
                if (key.isAcceptable()) { //客户端连接请求
                    ServerSocketChannel ssc = (ServerSocketChannel) key
                            .channel();
                    SocketChannel sc = ssc.accept();
                    sc.configureBlocking(false);
                    sc.register(selector, SelectionKey.OP_READ); //注册监听客户端输入
                }
                //可读的
                if (key.isReadable()) { //客户端输入
                    //获取客户端流.
                    SocketChannel sc = (SocketChannel) key.channel();
                    //
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    sc.read(buffer);
                    //读写转换.
                    buffer.flip();
                    sc.write(buffer);
                }
            }
            keys.clear();
        }
    }
}

