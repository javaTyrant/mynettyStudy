package io.netty.example.echo.juc;

import java.sql.Time;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author lufengxiang
 * @since 2021/4/29
 **/
public class MyThreadPoolTest {
    public static void main(String[] args) {
        MyThreadPoolExecutor threadPoolExecutor = new MyThreadPoolExecutor(4, 8, 10,
                TimeUnit.SECONDS, new ArrayBlockingQueue<>(4));
        for (int i = 0; i < 8; i++) {
            int finalI = i;
            //会开启四个核心线程
            threadPoolExecutor.execute(() -> {
                System.out.println("hello:" + finalI + Thread.currentThread().getName());
                try {
                    TimeUnit.SECONDS.sleep(1000);
                } catch (InterruptedException ignore) {

                }
            });
        }
    }
}
