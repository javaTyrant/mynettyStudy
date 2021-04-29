package io.netty.example.echo.schedule;

import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * @author lufengxiang
 * @since 2021/4/16
 **/
public class ScheduleDemo {
    public static void main(String[] args) {
        /**
         * netty定时调度的原理:
         * 1.netty的每个线程有两个延时队列,一个用于执行的taskQueue.一个调度的scheduleTaskQueue.
         * 2.第一次跑任务的时候会加入的taskQueue里,如果delayNanos大于0会转移到scheduleTaskQueue里.
         * 3.然后scheduleTaskQueue里poll的时候等待一定的时间.
         * 代码调用逻辑:schedule
         * 1.调用AbstractScheduledEventExecutor.schedule()
         * 2.调用SingleThreadEventExecutor.execute()->addTask->startThread
         * 3. doStartThread里调用SingleThreadEventExecutor.this.run();
         * 4.调用DefaultEventLoop的run方法.里面有个核心的takeTask方法.
         * 5.SingleThreadEventExecutor.takeTask.
         */
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        EventLoop loop = new DefaultEventLoop();
        System.out.println(dtf.format(LocalDateTime.now()));
        //走到了自己无法理解的分支了.柳暗花明.
        //如果时间过长,那么第二次执行的入口在哪呢.
        loop.schedule(() -> {
            System.out.println("五秒后执行1");
            System.out.println(dtf.format(LocalDateTime.now()));
        }, 100, TimeUnit.SECONDS);
    }
}
