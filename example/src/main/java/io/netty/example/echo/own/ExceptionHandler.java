package io.netty.example.echo.own;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author lumac
 * @since 2021/9/22
 */
public class ExceptionHandler extends ChannelDuplexHandler {
    //异常捕获器.
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof RuntimeException) {
            System.out.println("Handle Business Exception Success.");
        }

    }
}
