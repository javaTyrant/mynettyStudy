package io.netty.example.echo;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author lumac
 * @since 2021/9/20
 */
public class SampleInBoundHandler extends ChannelInboundHandlerAdapter {
    private final String name;
    private final boolean flush;

    public SampleInBoundHandler(String name, boolean flush) {
        this.name = name;
        this.flush = flush;
    }

    @Override
<<<<<<< HEAD

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

=======
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
>>>>>>> 2f92fae69586fa6bdd10db6861133dba54815141
        System.out.println("InBoundHandler: " + name);
        if (flush) {
            ctx.channel().writeAndFlush(msg);
        } else {
            super.channelRead(ctx, msg);
        }
<<<<<<< HEAD

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("InBoundHandlerException: " + name);
        ctx.fireExceptionCaught(cause);
=======
>>>>>>> 2f92fae69586fa6bdd10db6861133dba54815141
    }
}
