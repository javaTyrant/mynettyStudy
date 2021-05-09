package io.netty.example.echo.juc.test;

import io.netty.example.echo.juc.MyArrayBlockingQueue;

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
                //没有值的时候进入等待队列
                System.out.println("返回值:" + queues.take());
            } catch (InterruptedException ignore) {

            }
        });
        Thread.sleep(10000);
        service.execute(() -> {
            try {
                //有值的时候唤醒等待队列
                queues.put(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
}
