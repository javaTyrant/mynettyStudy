package io.netty.example.echo.juc.mylock;

import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * 读写锁要解决什么问题?
 * 这个实现还能优化吗?
 *
 * @author lufengxiang
 * @since 2021/4/30
 **/
public class MyReentrantReadWriteLock
        implements MyReadWriteLock, Serializable {

    private static final long serialVersionUID = -6992448646407690164L;

    private final MyReentrantReadWriteLock.ReadLock readerLock;
    private final MyReentrantReadWriteLock.WriteLock writerLock;

    final Sync sync;

    public MyReentrantReadWriteLock() {
        this(false);
    }

    public MyReentrantReadWriteLock(boolean fair) {
        sync = fair ? new FariSync() : new NonfairSync();
        readerLock = new ReadLock();
        writerLock = new WriteLock();
    }
    //get

    @Override
    public MyLock writeLock() {
        return writerLock;
    }

    @Override
    public MyLock readLock() {
        return readerLock;
    }

    //内部类
    abstract static class Sync extends MyAbstractQueuedSynchronizer {
        private static final long serialVersionUID = 6317671515068378041L;
        static final int SHARED_SHIFT = 16;
        static final int SHARED_UNIT = (1 << SHARED_SHIFT);
        static final int MAX_COUNT = (1 << SHARED_SHIFT) - 1;
        static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;

        static int sharedCount(int c) {
            return c >>> SHARED_SHIFT;
        }

        static int exclusiveCount(int c) {
            return c & EXCLUSIVE_MASK;
        }

        static final class HoldCounter {
            int count = 0;
            final long tid = getThreadId(Thread.currentThread());
        }

        static final class ThreadLocalHoldCounter extends ThreadLocal<HoldCounter> {
            public HoldCounter initialValue() {
                return new HoldCounter();
            }
        }

        private transient ThreadLocalHoldCounter readHolds;
        private transient HoldCounter cachedHoldCounter;
        private transient Thread firstReader = null;
        private transient int firstReaderHoldCount;

        Sync() {
            readHolds = new ThreadLocalHoldCounter();
            setState(getState());
        }

        abstract boolean readerShouldBlock();

        abstract boolean writerShouldBlock();

        protected final boolean tryRelease(int releases) {
            if (!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            int nextc = getState() - releases;
            boolean free = exclusiveCount(nextc) == 0;
            if (free) {
                setExclusiveOwnerThread(null);
            }
            setState(nextc);
            return free;
        }

        protected final boolean tryAcquire(int acquires) {
            Thread current = Thread.currentThread();
            int c = getState();
            int w = exclusiveCount(c);
            if (c != 0) {
                if (w == 0 || current != getExclusiveOwnerThread()) {
                    return false;
                }
                if (w + exclusiveCount(acquires) > MAX_COUNT) {
                    throw new Error("Maximum lock count exceeded");
                }
                setState(c + acquires);
                return true;
            }
            if (writerShouldBlock() ||
                    !compareAndSetState(c, c + acquires)) {
                return false;
            }
            setExclusiveOwnerThread(current);
            return true;
        }

        protected final boolean tryReleaseShared(int unused) {
            Thread current = Thread.currentThread();
            if (firstReader == current) {
                if (firstReaderHoldCount == 1) {
                    firstReader = null;
                } else {
                    firstReaderHoldCount--;
                }
            } else {
                HoldCounter rh = cachedHoldCounter;
                if (rh == null || rh.tid != getThreadId(current)) {
                    rh = readHolds.get();
                }
                int count = rh.count;
                if (count <= 1) {
                    readHolds.remove();
                    if (count <= 0) {
                        throw unmatchedUnlockException();
                    }
                }
                --rh.count;
            }
            for (; ; ) {
                int c = getState();
                int nextc = c - SHARED_SHIFT;
                if (compareAndSetState(c, nextc)) {
                    return nextc == 0;
                }
            }
        }

        private IllegalMonitorStateException unmatchedUnlockException() {
            return new IllegalMonitorStateException(
                    "attempt to unlock read lock,not locked by current thread");
        }

        protected final int tryAcquireShared(int unused) {

        }

        final int fullTryAcquireShared(Thread current) {

        }

        final boolean tryWriteLock() {

        }

        final boolean tryReadLock() {

        }

        protected final boolean isHeldExclusively() {

        }

        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        final Thread getOwner() {

        }

        final int getReadLockCount() {

        }

        final isWriteLocked() {

        }

        final int getWriteHoldCount() {

        }

        final int getReadHoldCount() {

        }

        private void readObject(ObjectInputStream s) {

        }

        final int getCount() {

        }
    }

    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = -8159625535654395037L;

        NonfairSync() {

        }

        final boolean readerShouldBlock() {

        }

        final boolean writerShouldBlock() {

        }
    }

    static final class FariSync extends Sync {
        private static final long serialVersionUID = -2274990926593161451L;

        FariSync() {

        }

        final boolean writerShouldBlock() {

        }

        final boolean readerShouldBlock() {

        }
    }

    public static class ReadLock implements MyLock, Serializable {
        private static final long serialVersionUID = -5992448646407690164L;
        private final Sync sync;


        protected ReadLock(MyReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        public void lock() {

        }

        public void lockInterruptibly()
                throws InterruptedException {

        }

        public boolean tryLock() {

        }

        public boolean tryLock(long timeout, TimeUnit unit)
                throws InterruptedException {

        }

        public void unlock() {

        }

        public MyCondition newCondition() {

        }

        @Override
        public String toString() {
            return "ReadLock{" +
                    "sync=" + sync +
                    '}';
        }
    }

    public static class WriteLock implements MyLock, Serializable {
        private static final long serialVersionUID = -4992448646407690164L;
        private final Sync sync;

        protected WriteLock(MyReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        public void lock() {

        }
        public void lockInterruptibly()throws InterruptedException {

        }

        public boolean tryLock() {

        }

        public void unlock() {

        }

        public MyCondition newCondition() {

        }

        @Override
        public String toString() {
            return "WriteLock{" +
                    "sync=" + sync +
                    '}';
        }

        public boolean isHeldByCurrentThread() {

        }

        public int getHoldCount() {

        }
    }
}
