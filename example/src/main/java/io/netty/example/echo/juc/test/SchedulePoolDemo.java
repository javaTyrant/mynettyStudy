package io.netty.example.echo.juc.test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author lufengxiang
 * @since 2021/5/13
 **/
public class SchedulePoolDemo {
    public static void main(String[] args) {
        ScheduledExecutorService service = Executors.newScheduledThreadPool(2);
        service.schedule(() -> System.out.println("hello"), 1000, TimeUnit.MILLISECONDS);
    }
}
