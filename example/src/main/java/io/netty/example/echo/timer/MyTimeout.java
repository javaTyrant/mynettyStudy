package io.netty.example.echo.timer;

/**
 * @author lufengxiang
 * @since 2021/5/18
 **/
public interface MyTimeout {

    MyTimer timer();

    MyTimerTask task();

    boolean isExpired();

    boolean isCancelled();

    boolean cancel();

}
