package io.netty.example.echo.juc.test;

import java.util.concurrent.locks.LockSupport;

/**
 * @author lufengxiang
 * @since 2021/4/20
 **/
public class LockSupportDemo {
    public static Object u = new Object();
    static ChangeObjectThread t1 = new ChangeObjectThread("t1");
    static ChangeObjectThread t2 = new ChangeObjectThread("t2");

    public static class ChangeObjectThread extends Thread {
        public ChangeObjectThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            synchronized (u) {
                System.out.println("in " + getName());
                //unpark会回到上次断掉的地方.
                LockSupport.park();
                System.out.println("park之后还会执行吗");
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println(getName() + "被中断了");
                } else {
                    System.out.println(getName() + "继续执行");
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        t1.start();
        Thread.sleep(1000L);
        t2.start();
        Thread.sleep(3000L);
        //interrupt会调用unpark
        t1.interrupt();
        //如果不唤醒t2那么t2会一直阻塞,那么t1是谁唤醒的呢?
        LockSupport.unpark(t2);
        t1.join();
        t2.join();
    }
}
