package io.netty.example.echo.demo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author lufengxiang
 * @since 2021/5/27
 **/
public class ThreadLocalDemo {
    public static void main(String[] args) {
        for (int i = 0; i != 2; i++) {
            System.out.println(i);
        }
        System.out.println(0x61c88647);
        //多线程共享一个ThreadLocal,threadLocal里做隔离.
        ThreadLocal<Integer> local = new ThreadLocal<>();
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        for (int i = 0; i < 8; i++) {
            int finalI = i;
            executorService.execute(() -> {
                String name = Thread.currentThread().getName();
                System.out.println("set:" + name + ":" + finalI);
                local.set(finalI);
            });
        }
        for (int i = 0; i < 8; i++) {
            executorService.execute(() -> {
                System.out.println("get:" + Thread.currentThread().getName() + ":" + local.get());
            });
        }
        executorService.shutdown();
    }
}
