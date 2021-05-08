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
    //作用是啥?
    private static class Generation {
        Generation() {
        }

        boolean broken;
    }

    private final ReentrantLock lock = new ReentrantLock();

    private final Condition trip = lock.newCondition();

    private final int parties;

    private final Runnable barrierCommand;

    private Generation generation = new Generation();

    private int count;

    private void nextGeneration() {
        trip.signalAll();
        count = parties;
        generation = new Generation();
    }

    private void breakBarrier() {
        generation.broken = true;
        count = parties;
        trip.signalAll();
    }

    //核心方法
    private int dowait(boolean timed, long nanos) throws BrokenBarrierException, InterruptedException, TimeoutException {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            final Generation g = generation;
            if (generation.broken) {
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
                        ranAction = true;
                        nextGeneration();
                        return 0;
                    }
                } finally {
                    if (!ranAction) {
                        breakBarrier();
                    }
                }
            }
            for (; ; ) {
                try {
                    if (!timed) {
                        trip.await();
                    } else {
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
            return dowait(false, 0L);
        } catch (TimeoutException toe) {
            throw new Error(toe);
        }
    }

    public int await(long timeout, TimeUnit unit) throws BrokenBarrierException, InterruptedException, TimeoutException {
        return dowait(true, unit.toNanos(timeout));
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

    public int getNumberWaiting() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return parties - count;
        } finally {
            lock.unlock();
        }
    }
}
