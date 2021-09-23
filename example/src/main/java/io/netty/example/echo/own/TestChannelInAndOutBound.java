package io.netty.example.echo.own;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.example.echo.SampleInBoundHandler;

/**
 * @author lumac
 * @since 2021/9/22
 */
public class TestChannelInAndOutBound {
    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap server = new ServerBootstrap();
        server.group(bossGroup,workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new SampleInBoundHandler("SampleInBoundHandlerA", false))

                                .addLast(new SampleInBoundHandler("SampleInBoundHandlerB", false))

                                .addLast(new SampleInBoundHandler("SampleInBoundHandlerC", true));
                        ch.pipeline()

                                .addLast(new SampleOutBoundHandler("SampleOutBoundHandlerA"))

                                .addLast(new SampleOutBoundHandler("SampleOutBoundHandlerB"))

                                .addLast(new SampleOutBoundHandler("SampleOutBoundHandlerC"));
                        //必须放在后面再有效果.
                        //通过异常传播机制的学习，我们应该可以想到最好的方法是在 ChannelPipeline 自定义处理器的末端添加统一的异常处理器，此时 ChannelPipeline 的内部结构如下图所示。
                        ch.pipeline().addLast(new ExceptionHandler());
                    }
                });
        ChannelFuture f = server.bind(8080).sync();
        // Wait until the server socket is closed.
        f.channel().closeFuture().sync();
    }
}
