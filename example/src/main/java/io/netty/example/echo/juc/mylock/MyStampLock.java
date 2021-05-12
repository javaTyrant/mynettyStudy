package io.netty.example.echo.juc.mylock;

import io.netty.example.echo.juc.MyCountDownLatch;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author lumac
 * @since 2021/5/9
 */
public class MyStampLock {

    /**
     * @author lufengxiang
     * @since 2021/5/8
     **/
    public static class MyCountDownLatchDemo {
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
}
