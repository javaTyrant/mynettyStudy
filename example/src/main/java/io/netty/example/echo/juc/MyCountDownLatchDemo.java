package io.netty.example.echo.juc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author lufengxiang
 * @since 2021/5/8
 **/
public class MyCountDownLatchDemo {
    public static void main(String[] args) throws InterruptedException {
        ExecutorService service = Executors.newFixedThreadPool(2);
        MyCountDownLatch latch = new MyCountDownLatch(2);
        service.execute(() -> {
            latch.countDown();
        });
        service.execute((() -> {
            latch.countDown();
        }));
        latch.await();
        System.out.println("执行到了");
    }
}
