package io.netty.example.echo.juc;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author lumac
 * @since 2021/5/4
 */
@SuppressWarnings("unused")
public class MyCyclicBarrier {
    //
    private static class Generation {
        boolean broken;
    }
    //可重入锁.
    private final ReentrantLock lock = new ReentrantLock();
    //条件队列
    private final Condition trip = lock.newCondition();
    //参与的线程数量
    private final int parties;
    //由最后一个进入 barrier 的线程执行的操作
    private final Runnable barrierCommand;
    //当前代
    private Generation generation = new Generation();
    // 正在等待进入屏障的线程数量
    private int count;

    //
    private void nextGeneration() {
        //通知所有.
        trip.signalAll();
        count = parties;
        generation = new Generation();
    }

    private void breakBarrier() {
        generation.broken = true;
        count = parties;
        //通知所有.
        trip.signalAll();
    }

    //核心方法:
    private int doWait(boolean timed, long nanos)
            throws BrokenBarrierException, InterruptedException, TimeoutException {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            final Generation g = generation;
            if (g.broken) {
                throw new BrokenBarrierException();
            }
            if (Thread.interrupted()) {
                breakBarrier();
                throw new InterruptedException();
            }
            int index = --count;
            if (index == 0) {
                boolean ranAction = false;
                try {
                    final Runnable command = barrierCommand;
                    if (command != null) {
                        command.run();
                    }
                    ranAction = true;
                    nextGeneration();
                    return 0;
                } finally {
                    if (!ranAction) {
                        //
                        breakBarrier();
                    }
                }
            }
            //
            for (; ; ) {
                try {
                    if (!timed) {
                        //await的时候.await有哪些操作,要牢记.
                        trip.await();
                    } else if (nanos > 0L) {
                        nanos = trip.awaitNanos(nanos);
                    }
                } catch (InterruptedException e) {
                    if (g == generation && !g.broken) {
                        breakBarrier();
                        throw e;
                    } else {
                        Thread.currentThread().interrupt();
                    }
                }
                if (g.broken) {
                    throw new BrokenBarrierException();
                }
                if (g != generation) {
                    return index;
                }
                if (timed && nanos <= 0L) {
                    breakBarrier();
                    throw new TimeoutException();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public MyCyclicBarrier(int parties, Runnable barrierAction) {
        //给parties赋值,然后也赋值给count,任务到了的时候修改count,nextgeneration的时候
        //再把parties赋值给count.
        this.parties = parties;
        this.barrierCommand = barrierAction;
        this.count = parties;
    }

    public MyCyclicBarrier(int parties) {
        this(parties, null);
    }

    public int getParties() {
        return parties;
    }

    //公开.
    public int await() throws InterruptedException, BrokenBarrierException {
        try {
            return doWait(false, 0L);
        } catch (TimeoutException toe) {
            throw new Error(toe);
        }
    }

    public int await(long timeout, TimeUnit unit)
            throws BrokenBarrierException, InterruptedException, TimeoutException {
        return doWait(true, unit.toNanos(timeout));
    }

    public boolean isBroken() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return generation.broken;
        } finally {
            lock.unlock();
        }
    }

    //可以重置.Countdownlatch不可以.
    public void reset() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            breakBarrier();
            nextGeneration();
        } finally {
            lock.unlock();
        }
    }

    //获取等待的线程数量
    public int getNumberWaiting() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            //简单.
            return parties - count;
        } finally {
            lock.unlock();
        }
    }
}
