package io.netty.example.echo.juc.mylock;

/**
 * @author lufengxiang
 * @since 2021/4/30
 **/
public interface MyReadWriteLock {
    //
    MyLock readLock();

    //
    MyLock writeLock();
}

