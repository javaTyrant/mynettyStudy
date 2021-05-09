package io.netty.example.echo.juc.test;

import io.netty.example.echo.juc.MyFutureTask;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author lufengxiang
 * @since 2021/4/19
 **/
public class Demo<E> {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        MyFutureTask<Integer> task = new MyFutureTask<>(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                Thread.sleep(10000);
                return 1;
            }
        });
        Thread thread = new Thread(task);
        thread.start();
        System.out.println("hello");
        System.out.println(task.get());
    }
}
