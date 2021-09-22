/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.example.echo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;
//Netty 服务端启动后，BossEventLoopGroup 会负责监听客户端的 Accept 事件。
//当有客户端新连接接入时，BossEventLoopGroup 中的 NioEventLoop 首先会新建客户端 Channel，
//然后在 NioServerSocketChannel 中触发 channelRead 事件传播，NioServerSocketChannel
//中包含了一种特殊的处理器 ServerBootstrapAcceptor，最终通过 ServerBootstrapAcceptor
//的 channelRead() 方法将新建的客户端 Channel 分配到 WorkerEventLoopGroup 中。
//WorkerEventLoopGroup 中包含多个 NioEventLoop，它会选择其中一个 NioEventLoop 与新建的客户端 Channel 绑定。

//完成客户端连接注册之后，就可以接收客户端的请求数据了。当客户端向服务端发送数据时，NioEventLoop 会监听到 OP_READ 事件，
//然后分配 ByteBuf 并读取数据，读取完成后将数据传递给 Pipeline 进行处理。一般来说，数据会从 ChannelPipeline 的第一个
//ChannelHandler 开始传播，将加工处理后的消息传递给下一个 ChannelHandler，整个过程是串行化执行。

/**
 * 一些组件之间的关系:
 * 1.一个EventLoopGroup 包含一个或者多个EventLoop；
 * 2.一个EventLoop 在它的生命周期内只和一个Thread 绑定；
 * 3.所有由EventLoop 处理的I/O 事件都将在它专有的Thread 上被处理；
 * 4.一个Channel 在它的生命周期内只注册于一个EventLoop；
 * 5.一个EventLoop 可能会被分配给一个或多个Channel。
 * 注意，在这种设计中，一个给定Channel 的I/O 操作都是由相同的Thread 执行的，实际
 * 上消除了对于同步的需要。
 * 问:eventLoop怎么跟线程绑定起来的?
 */

/**
 * Echoes back any received data from a client.
 */
public final class EchoServer {
    /*
        1.bind流程
        ...省略了直观的调用过程.
        1.1 AbstractChannelHandlerContext.bind
        1.2 AbstractChannelHandlerContext.invokeBind
        1.3
        2.accept流程
        2.1
        2.2
        2.3
        3.如何读数据:AdaptiveRecvByteBufAllocator
        3.1自适应数据大小分配器.
        3.2连续读.
        主线:
        1.多路复用器(Selector接收到OP_READ事件)
        2.处理OP_READ事件:NioSocketChannel.NioSocketChannelUnsafe.read()
        2.1
        2.2
        2.3
        2.4
        2.5
     */
    static final boolean SSL = System.getProperty("ssl") != null;
    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));

    //boss线程的启动:
    //worker线程的启动:
    //boss如何把工作转移到worker的呢?boss accept
    //ChannelHandlerContext的构造在哪里?如果是我设计我会放到哪里呢.肯定是在pipeline里吧.
    public static void main(String[] args) throws Exception {
        // Configure SSL.
        final SslContext sslCtx;
        if (SSL) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }
        // Configure the server.MultithreadEventExecutorGroup里分配线程.
        // children[i] = newChild(executor, args);线程的分配有了,那么线程的启动呢?
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        //worker
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        final EchoServerHandler serverHandler = new EchoServerHandler();
        final EchoServerHandler1 serverHandler1 = new EchoServerHandler1();
        try {
            ServerBootstrap b = new ServerBootstrap();
            //group的作用.boss保存到AbstractBootstrap的group里.
            //workerGroup.保存到ServerBootstrap的childGroup
            b.group(bossGroup, workerGroup)
                    //反射构造channel.
                    //客户端流的创建: buf.add(new NioSocketChannel(this, ch));
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            //从ch里获取pipeline:思考下,网络流程在Netty中的旅程.
                            //主要还是跟下出站入站的流程.
                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc()));
                            }
                            //p.addLast(new LoggingHandler(LogLevel.INFO));
                            //如何触发的?callHandlerAdded->
                            p.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));
                            p.addLast(serverHandler);
                            p.addLast(serverHandler1);
                            p.addLast(new HeartBeatServerHandler());
                            p.addLast(new SampleInBoundHandler("SampleInBoundHandlerA", false))
                                    .addLast(new SampleInBoundHandler("SampleInBoundHandlerB", false))
                                    .addLast(new SampleInBoundHandler("SampleInBoundHandlerC", true));
                            //p.addLast()
                        }
                    });

            // Start the server.
            ChannelFuture f = b.bind(PORT).sync();
            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
