package io.netty.example.echo.juc.mylock;

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

    public void acquire() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    public void acquireUninterruptibly() {
        sync.acquireShared(1);
    }

    public void acquire(int permits) throws InterruptedException {
        if (permits < 0) throw new IllegalArgumentException();
        sync.acquireSharedInterruptibly(permits);
    }

    public void acquireUninterruptibly(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        sync.acquireShared(permits);
    }

    public boolean tryAcquire() {
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
            for (; ; ) {
                int available = getState();
                int remaining = available - acquires;
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

    static final class NonFairSync extends Sync {

        NonFairSync(int permits) {
            super(permits);
        }

        protected int tryAcquireShared(int acquires) {
            return nonfairTryAcquireShared(acquires);
        }
    }
}
