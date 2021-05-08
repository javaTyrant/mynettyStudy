package io.netty.example.echo.juc;

import java.sql.Time;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author lufengxiang
 * @since 2021/4/29
 **/
public class MyThreadPoolTest {
    //start的语义:
    //Causes this thread to begin execution; the Java Virtual Machine calls the run method of this thread.
    //The result is that two threads are running concurrently:
    // the current thread (which returns from the call to the start method) and the other thread (which executes its run method).
    //It is never legal to start a thread more than once. In particular, a thread may not be restarted once it has completed execution.
    //Throws:
    //IllegalThreadStateException – if the thread was already started.
    //See Also:
    //run(), stop()
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
