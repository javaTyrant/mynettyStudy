package io.netty.example.echo.juc;

import io.netty.example.echo.juc.mylock.MyAbstractQueuedSynchronizer;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * @author lufengxiang
 * @since 2021/5/6
 **/
@SuppressWarnings("unused")
public class MySemaphore {
    private static final long serialVersionUID = -3222578661600680210L;

    private final Sync sync;

    public MySemaphore(int permits) {
        sync = new NonFairSync(permits);
    }

    public MySemaphore(int permits, boolean fair) {
        sync = fair ? new FairSync(permits) : new NonFairSync(permits);
    }

    //信号量:共享的获取
    public void acquire() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    //获取允许,阻塞直到完成
    public void acquireUninterruptibly() {
        sync.acquireShared(1);
    }

    //获取一定数量的许可
    public void acquire(int permits) throws InterruptedException {
        if (permits < 0) throw new IllegalArgumentException();
        sync.acquireSharedInterruptibly(permits);
    }

    //不打断:直到等到有许可为止.
    public void acquireUninterruptibly(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        sync.acquireShared(permits);
    }

    /**
     * <p>
     *     1.立马返回.
     * </p>
     * Acquires a permit from this semaphore, only if one is available at the
     * time of invocation.
     *
     * <p>Acquires a permit, if one is available and returns immediately,
     * with the value {@code true},
     * reducing the number of available permits by one.
     *
     * <p>If no permit is available then this method will return
     * immediately with the value {@code false}.
     *
     * <p>Even when this semaphore has been set to use a
     * fair ordering policy, a call to {@code tryAcquire()} <em>will</em>
     * immediately acquire a permit if one is available, whether or not
     * other threads are currently waiting.
     * This &quot;barging&quot; behavior can be useful in certain
     * circumstances, even though it breaks fairness. If you want to honor
     * the fairness setting, then use
     * {@link #tryAcquire(long, TimeUnit) tryAcquire(0, TimeUnit.SECONDS) }
     * which is almost equivalent (it also detects interruption).
     *
     * @return {@code true} if a permit was acquired and {@code false}
     *         otherwise
     */
    public boolean tryAcquire() {
        //tryAcquire为什么要用非公平的实现
        return sync.nonfairTryAcquireShared(1) >= 0;
    }

    public boolean tryAcquire(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        return sync.nonfairTryAcquireShared(permits) >= 0;
    }

    public boolean tryAcquire(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    public boolean tryAcquire(int permits, long timeout, TimeUnit unit)
            throws InterruptedException {
        if (permits < 0) throw new IllegalArgumentException();
        return sync.tryAcquireSharedNanos(permits, unit.toNanos(timeout));
    }

    public void release() {
        sync.release(1);
    }

    public int availablePermits() {
        return sync.getPermits();
    }

    public int drainPermits() {
        return sync.drainPermits();
    }

    protected void reducePermits(int reduction) {
        if (reduction < 0) throw new IllegalArgumentException();
        sync.reducePermits(reduction);
    }

    public boolean isFair() {
        return sync instanceof FairSync;
    }

    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    public String toString() {
        return super.toString() + "[Permits = " + sync.getPermits() + "]";
    }

    abstract static class Sync extends MyAbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1192457210091910933L;

        Sync(int permits) {
            setState(permits);
        }

        final int getPermits() {
            return getState();
        }

        final int nonfairTryAcquireShared(int acquires) {
            //死循环获取
            for (; ; ) {
                //获取可用数量
                int available = getState();
                //剩余的许可
                int remaining = available - acquires;
                //如果剩余的大于0就直接返回.如果小于0,则cas设置为true才返回.
                if (remaining < 0 ||
                        compareAndSetState(available, remaining)) {
                    return remaining;
                }
            }
        }

        protected final boolean tryReleaseShared(int releases) {
            //死循环确保cas一定成功
            for (; ; ) {
                int current = getState();
                int next = current + releases;
                if (next < current) {
                    throw new Error("maximum permits count exceeded");
                }
                if (compareAndSetState(current, next)) {
                    return true;
                }
            }
        }

        final void reducePermits(int reductions) {
            for (; ; ) {
                int current = getState();
                int next = current - reductions;
                //溢出判断
                if (next > current) {
                    throw new Error("Permit count underflow");
                }
                if (compareAndSetState(current, next)) {
                    return;
                }
            }
        }

        //state置0
        final int drainPermits() {
            for (; ; ) {
                int current = getState();
                if (current == 0 || compareAndSetState(current, 0)) {
                    return current;
                }
            }
        }
    }

    static final class FairSync extends Sync {

        FairSync(int permits) {
            super(permits);
        }

        protected int tryAcquireShared(int acquires) {
            for (; ; ) {
                if (hasQueuedPredecessors()) {
                    return -1;
                }
                int available = getState();
                int remaining = available - acquires;
                if (remaining < 0 ||
                        compareAndSetState(available, remaining)) {
                    return remaining;
                }
            }
        }
    }

    //不公平的
    static final class NonFairSync extends Sync {

        NonFairSync(int permits) {
            super(permits);
        }

        protected int tryAcquireShared(int acquires) {
            //不公平的获取.
            return nonfairTryAcquireShared(acquires);
        }
    }
}
