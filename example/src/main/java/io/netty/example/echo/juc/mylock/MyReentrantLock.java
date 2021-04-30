package io.netty.example.echo.juc.mylock;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;

/**
 * 主要还是依赖aqs的实现
 *
 * @author lufengxiang
 * @since 2021/4/30
 **/
public class MyReentrantLock implements MyLock, java.io.Serializable {
    private static final long serialVersionUID = 7373984872572414699L;

    private final Sync sync;

    //默认是非公平的锁
    public MyReentrantLock() {
        sync = new NonfairSync();
    }

    @Override
    public void lock() {
        sync.lock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    @Override
    public boolean tryLock() {
        return sync.nonfairTryAcquire(1);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(time));
    }

    @Override
    public void unlock() {
        sync.release(1);
    }

    @Override//todo aqs写完再替换condition实现
    public Condition newCondition() {
        return sync.newCondition();
    }

    public int getHoldCount() {
        return sync.getHoldCount();
    }

    protected Thread getOwner() {
        return sync.getOwner();
    }

    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null) throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject)) {
            throw new IllegalArgumentException("not owner");
        }
        return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject) condition);
    }

    public final boolean hasQueuedThread(Thread thread) {
        return sync.isQueued(thread);
    }

    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    public boolean hasWaiters(Condition condition) {
        if (condition == null) throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject)) {
            throw new IllegalArgumentException("not owner");
        }
        return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject) condition);
    }

    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    public boolean isHeldByCurrentThread() {
        return sync.isHeldExclusively();
    }

    public boolean isLocked() {
        return sync.isLocked();
    }

    abstract static class Sync extends AbstractQueuedSynchronizer {
        abstract void lock();

        //尝试非公平的获取
        final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            //此时无人获取到锁
            if (c == 0) {
                //cas set
                if (compareAndSetState(0, acquires)) {
                    //cas成功
                    setExclusiveOwnerThread(current);
                    return true;
                }//判断是否可以重入
            } else if (current == getExclusiveOwnerThread()) {
                //
                int nextc = c + acquires;
                if (nextc < 0) {
                    throw new Error("Maximum lock count exceeded");
                }
                //更新数量
                setState(nextc);
            }
            //获取锁失败
            return false;
        }

        protected final boolean tryRelease(int releases) {
            int c = getState() - releases;
            if (Thread.currentThread() != getExclusiveOwnerThread()) {
                throw new IllegalMonitorStateException();
            }
            boolean free = true;
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null);
            }
            setState(c);
            return free;
        }

        protected final boolean isHeldExclusively() {
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        final Thread getOwner() {
            return getState() == 0 ? null : getExclusiveOwnerThread();
        }

        final int getHoldCount() {
            return isHeldExclusively() ? getState() : 0;
        }

        final boolean isLocked() {
            return getState() != 0;
        }

        //从流中构造
        private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
            stream.defaultReadObject();
            setState(0);
        }
    }

    static final class FairSync extends Sync {
        @Override
        protected boolean tryAcquire(int arg) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                //公平不能插队,所以要判断是否有等待的节点
                if (!hasQueuedPredecessors() && compareAndSetState(0, arg)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            } else if (current == getExclusiveOwnerThread()) {
                int nextc = c + arg;
                if (nextc < 0) {
                    throw new Error("maximum lock count exceeded");
                }
                setState(nextc);
                return true;
            }
            return false;
        }

        @Override
        void lock() {
            acquire(1);
        }
    }

    static final class NonfairSync extends Sync {
        @Override
        protected boolean tryAcquire(int accquires) {
            return nonfairTryAcquire(accquires);
        }

        @Override
        void lock() {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
            } else {
                acquire(1);
            }
        }
    }

    @Override
    public String toString() {
        Thread o = sync.getOwner();
        return super.toString() + ((o == null) ? "unlocked" : "locked by thread" + o.getName());
    }
}
