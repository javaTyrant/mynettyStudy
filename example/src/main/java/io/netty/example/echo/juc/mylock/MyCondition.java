package io.netty.example.echo.juc.mylock;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author lufengxiang
 * @since 2021/4/30
 **/
public interface MyCondition {
    void await() throws InterruptedException;

    void awaitUninterruptibly();

    long awaitNanos(long nanosTimeout) throws InterruptedException;

    boolean await(long time, TimeUnit unit) throws InterruptedException;

    boolean awaitUntil(Date deadLine) throws InterruptedException;

    void signal();

    void signalAll();
}
