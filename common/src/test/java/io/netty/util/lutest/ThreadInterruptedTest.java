package io.netty.util.lutest;

/**
 * @author lufengxiang
 * @since 2022/2/25
 **/
public class ThreadInterruptedTest {
    //
    static class MyThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    System.out.println("exception");
                }
                if (true) {
                    System.out.println("exit MyThread");
                    break;
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Thread thread = new MyThread();
        thread.start();
        System.out.println(thread.getState());
//        thread.interrupt();
        Thread.sleep(4000);//等到thread线程被中断之后
//        System.out.println(thread.isInterrupted());
        System.out.println(thread.getState());
    }
}
