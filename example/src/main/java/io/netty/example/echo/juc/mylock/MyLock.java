package io.netty.example.echo.juc.mylock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * @author lufengxiang
 * @since 2021/4/30
 **/
public interface MyLock {
    //
    void lock();
    //
    void lockInterruptibly() throws InterruptedException;
    //
    boolean tryLock();
    //
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;
    //
    void unlock();
    //
    Condition newCondition();

}
