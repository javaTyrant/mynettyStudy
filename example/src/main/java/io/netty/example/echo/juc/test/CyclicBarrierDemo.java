package io.netty.example.echo.juc.test;

import io.netty.example.echo.juc.MyCyclicBarrier;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author lufengxiang
 * @since 2021/5/17
 **/
public class CyclicBarrierDemo {
    public static void main(String[] args) {
        ExecutorService service = Executors.newCachedThreadPool();
        final MyCyclicBarrier cb = new MyCyclicBarrier(3);//设置等待到达的线程数目
        Runnable runnable = () -> {
            try {
                Thread.sleep((long) (Math.random() * 10000));
                System.out.println("线程" + Thread.currentThread().getName() +
                        "即将到达集合地点1，当前已有" + (cb.getNumberWaiting() + 1) + "个已经到达，" + (cb.getNumberWaiting() == 2 ? "都到齐了继续走呀" : "正在等候"));
                cb.await();//到此如果没有达到公共屏障点，则该线程处于等待状态，如果达到公共屏障点则所有处于等待的线程都继续往下运行

                Thread.sleep((long) (Math.random() * 10000));
                System.out.println("线程" + Thread.currentThread().getName() +
                        "即将到达集合地点2，当前已有" + (cb.getNumberWaiting() + 1) + "个已经到达，" + (cb.getNumberWaiting() == 2 ? "都到齐了继续走呀" : "正在等候"));
                cb.await();
                Thread.sleep((long) (Math.random() * 10000));
                System.out.println("线程" + Thread.currentThread().getName() +
                        "即将到达集合地点3，当前已有" + (cb.getNumberWaiting() + 1) + "个已经到达，" + (cb.getNumberWaiting() == 2 ? "都到齐了继续走呀" : "正在等候"));
                cb.await();
            } catch (Exception ignore) {
            }
        };
        for (int i = 0; i < 3; i++) {
            service.execute(runnable);
        }
        service.shutdown();
    }
}
