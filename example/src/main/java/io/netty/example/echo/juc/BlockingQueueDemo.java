package io.netty.example.echo.juc;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author lufengxiang
 * @since 2021/5/8
 **/
public class BlockingQueueDemo {
    public static void main(String[] args) throws InterruptedException {
        MyArrayBlockingQueue<Integer> queues = new MyArrayBlockingQueue<>(5);
        ExecutorService service = Executors.newFixedThreadPool(2);
        service.execute(() -> {
            try {
                System.out.println("返回值:" + queues.take());
            } catch (InterruptedException ignore) {

            }
        });
        Thread.sleep(10000);
        service.execute(() -> {
            try {
                queues.put(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
}
