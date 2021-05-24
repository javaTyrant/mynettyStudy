package io.netty.example.echo.demo;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;

import java.util.concurrent.TimeUnit;

/**
 * @author lufengxiang
 * @since 2021/5/19
 **/
public class HashedWheelTimerDemo {
    //newTimeout的过程
    public static void main(String[] args) {
        // 构造一个 Timer 实例
        Timer timer = new HashedWheelTimer();
        // 提交一个任务，让它在 5s 后执行
        Timeout timeout1 = timer.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) {
                System.out.println("5s 后执行该任务");
            }
        }, 5, TimeUnit.SECONDS);
        // 再提交一个任务，让它在 10s 后执行
        Timeout timeout2 = timer.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) {
                System.out.println("10s 后执行该任务");
            }
        }, 10, TimeUnit.SECONDS);
        // 再提交一个任务，让它在 10s 后执行
        Timeout timeout3 = timer.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) {
                System.out.println("100s 后执行该任务");
            }
        }, 100, TimeUnit.SECONDS);
    }
}
