package io.netty.example.echo.juc.promise;

import io.netty.util.concurrent.GenericFutureListener;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author lufengxiang
 * @since 2021/5/22
 **/
public interface MyFuture<V> extends Future<V> {

    boolean isSuccess();

    boolean isCancellable();

    Throwable cause();

    //PECS
    MyFuture<V> addListener(GenericFutureListener<? extends MyFuture<? super V>> listener);

    MyFuture<V> addListeners(GenericFutureListener<? extends MyFuture<? super V>>... listeners);

    MyFuture<V> removeListener(GenericFutureListener<? extends MyFuture<? super V>> listener);

    MyFuture<V> removeListeners(GenericFutureListener<? extends MyFuture<? super V>>... listeners);

    MyFuture<V> sync() throws InterruptedException;

    MyFuture<V> syncUninterruptibly();

    MyFuture<V> await() throws InterruptedException;

    MyFuture<V> awaitUninterruptibly();

    boolean await(long timeout, TimeUnit unit) throws InterruptedException;

    boolean await(long timeoutMillis) throws InterruptedException;

    boolean awaitUninterruptibly(long timeout, TimeUnit unit);

    boolean awaitUninterruptibly(long timeoutMillis);

    V getNow();

    boolean cancel(boolean mayInterruptIfRunning);
}
