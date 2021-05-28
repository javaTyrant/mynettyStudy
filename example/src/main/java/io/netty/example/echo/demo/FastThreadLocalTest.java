package io.netty.example.echo.demo;

import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.FastThreadLocal;

/**
 * @author lufengxiang
 * @since 2021/5/27
 **/
public class FastThreadLocalTest {
    //
    public static final int MAX = 10000000;

    public static void main(String[] args) {
        new Thread(FastThreadLocalTest::threadLocal).start();
        new Thread(FastThreadLocalTest::fastThreadLocal).start();
    }

    private static void fastThreadLocal() {
        long start = System.currentTimeMillis();
        DefaultThreadFactory defaultThreadFactory = new DefaultThreadFactory(FastThreadLocalTest.class);

        FastThreadLocal<String>[] fastThreadLocal = new FastThreadLocal[MAX];

        for (int i = 0; i < MAX; i++) {
            fastThreadLocal[i] = new FastThreadLocal<>();
        }

        Thread thread = defaultThreadFactory.newThread(() -> {
            for (int i = 0; i < MAX; i++) {
                fastThreadLocal[i].set("java: " + i);
            }

            System.out.println("fastThreadLocal set: " + (System.currentTimeMillis() - start));

            for (int i = 0; i < MAX; i++) {
                for (int j = 0; j < MAX; j++) {
                    fastThreadLocal[i].get();
                }
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("fastThreadLocal total: " + (System.currentTimeMillis() - start));
    }

    private static void threadLocal() {
        long start = System.currentTimeMillis();
        ThreadLocal<String>[] threadLocals = new ThreadLocal[MAX];

        for (int i = 0; i < MAX; i++) {
            threadLocals[i] = new ThreadLocal<>();
        }

        Thread thread = new Thread(() -> {
            for (int i = 0; i < MAX; i++) {
                threadLocals[i].set("java: " + i);
            }

            System.out.println("threadLocal set: " + (System.currentTimeMillis() - start));

            for (int i = 0; i < MAX; i++) {
                for (int j = 0; j < MAX; j++) {
                    threadLocals[i].get();
                }
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("threadLocal total: " + (System.currentTimeMillis() - start));
    }
}
