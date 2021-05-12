package io.netty.example.echo.juc.mylock;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.LockSupport;

import static io.netty.example.echo.juc.UnsafeUtil.createUnsafe;

/**
 * @author lufengxiang
 * @since 2021/5/6
 **/
@SuppressWarnings("unused")
public class MyAbstractQueuedSynchronizer
        extends MyAbstractOwnableSynchronizer
        implements Serializable {
    private static final long serialVersionUID = 7373984972572414691L;

    protected MyAbstractQueuedSynchronizer() {
    }

    //
    static final class Node {
        //共享
        static final Node SHARED = new Node();
        //独占
        static final Node EXCLUSIVE = null;
        //几个状态的含义
        static final int CANCELLED = 1;
        //successor's thread needs unparking
        static final int SIGNAL = -1;
        //wait on condition
        static final int CONDITION = -2;
        //the next acquireShared should unconditionally propagate
        static final int PROPAGATE = -3;

        //volatile类型变量
        volatile int waitStatus;
        volatile Node prev;
        volatile Node next;
        volatile Thread thread;
        //为什么nextWaiter不需要加volatile呢
        Node nextWaiter;

        final boolean isShared() {
            return nextWaiter == SHARED;
        }

        //前任
        final Node predecessor() {
            Node p = prev;
            if (p == null) {
                throw new NullPointerException();
            } else {
                return p;
            }
        }

        Node() {
        }

        Node(Thread thread, Node mode) {
            this.nextWaiter = mode;
            this.thread = thread;
        }

        Node(Thread thread, int waitStatus) {
            this.waitStatus = waitStatus;
            this.thread = thread;
        }
    }

    private transient volatile Node head;
    private transient volatile Node tail;
    private volatile int state;

    protected final int getState() {
        return state;
    }

    protected final void setState(int newState) {
        state = newState;
    }

    private static final long stateOffset;
    private static final long headOffset;
    private static final long tailOffset;
    private static final long waitStatusOffset;
    private static final long nextOffset;
    static final long spinForTimeoutThreshold = 1000L;
    private static final sun.misc.Unsafe unsafe;

    static {
        try {
            unsafe = createUnsafe();
            stateOffset = unsafe.objectFieldOffset(MyAbstractQueuedSynchronizer.class.getDeclaredField("state"));
            headOffset = unsafe.objectFieldOffset(MyAbstractQueuedSynchronizer.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset(MyAbstractQueuedSynchronizer.class.getDeclaredField("tail"));
            waitStatusOffset = unsafe.objectFieldOffset(Node.class.getDeclaredField("waitStatus"));
            nextOffset = unsafe.objectFieldOffset(Node.class.getDeclaredField("next"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private boolean compareAndSetHead(Node update) {
        return unsafe.compareAndSwapObject(this, headOffset, null, update);
    }

    private boolean compareAndSetTail(Node expect, Node update) {
        return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
    }

    private static boolean compareAndSetWaitStatus(Node node, int expect, int update) {
        return unsafe.compareAndSwapInt(node, waitStatusOffset, expect, update);
    }

    private static boolean compareAndSetNext(Node node, Node expect, Node update) {
        return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
    }

    protected final boolean compareAndSetState(int expect, int update) {
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }


    //ConditionObject
    public class ConditionObject implements MyCondition, Serializable {
        private static final long serialVersionUID = 1173984872572414699L;
        private transient Node firstWaiter;
        private transient Node lastWaiter;
        public ConditionObject conditionObject;
        private static final int REINTERRUPT = 1;
        private static final int THROW_IE = -1;

        public ConditionObject() {

        }

        //添加等待节点
        private Node addConditionWaiter() {
            Node t = lastWaiter;
            //如果lastWaiter被取消了,清理掉
            if (t != null && t.waitStatus != Node.CONDITION) {
                unlinkCancelledWaiters();
                t = lastWaiter;
            }
            //构造一个等待节点
            Node node = new Node(Thread.currentThread(), Node.CONDITION);
            //如果last不存在
            if (t == null) {
                firstWaiter = node;
            } else {
                //如果有last,就放入last后面
                t.nextWaiter = node;
            }
            //
            lastWaiter = node;
            return node;
        }


        //通知
        private void doSignal(Node first) {
            do {
                //如何这个节点的下一个等待者是空
                if ((firstWaiter = first.nextWaiter) == null) {
                    //
                    lastWaiter = null;
                }
                //又写错地方了
                first.nextWaiter = null;
            } while (!transferForSignal(first) &&
                    (first = firstWaiter) != null);
        }

        private void doSignalAll(Node first) {
            lastWaiter = firstWaiter = null;
            do {
                Node next = first.nextWaiter;
                first.nextWaiter = null;
                transferForSignal(first);
                first = next;
            } while (first != null);
        }

        //
        private void unlinkCancelledWaiters() {
            Node t = firstWaiter;
            Node trail = null;
            while (t != null) {
                Node next = t.nextWaiter;
                if (t.waitStatus != Node.CONDITION) {
                    t.nextWaiter = null;
                    if (trail == null) {
                        firstWaiter = next;
                    } else {
                        trail.nextWaiter = next;
                    }
                    if (next == null) {
                        lastWaiter = trail;
                    }
                } else {
                    trail = t;
                }
                t = next;
            }
        }

        @Override
        public final void signal() {
            if (!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            Node first = firstWaiter;
            if (first != null) {
                doSignal(first);
            }
        }

        @Override
        public final void signalAll() {
            if (!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            Node first = firstWaiter;
            if (first != null) {
                doSignalAll(first);
            }
        }

        @Override
        public void awaitUninterruptibly() {
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean interrupted = false;
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);
                if (Thread.interrupted()) {
                    interrupted = true;
                }
            }
            if (acquireQueued(node, savedState) || interrupted) {
                selfInterrupt();
            }
        }

        @Override
        public void await() throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            //加入等待队列
            Node node = addConditionWaiter();
            System.out.println("node" + node);
            //释放锁:返回修改后的state值.
            int savedState = fullyRelease(node);
            int interruptMode = 0;
            //
            while (!isOnSyncQueue(node)) {
                System.out.println("准备暂停线程了:" + Thread.currentThread().getName());
                LockSupport.park(this);
                System.out.println("恢复执行了");
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0) {
                    break;
                }
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE) {
                interruptMode = REINTERRUPT;
            }
            if (node.nextWaiter != null) {
                unlinkCancelledWaiters();
            }
            if (interruptMode != 0) {
                reportInterruptAfterWait(interruptMode);
            }
        }


        @Override
        public long awaitNanos(long nanosTimeout) throws InterruptedException {
            //线程状态校验
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            //
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold) {
                    //会自动唤醒吧.
                    LockSupport.parkNanos(this, nanosTimeout);
                }
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0) {
                    break;
                }
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE) {
                interruptMode = REINTERRUPT;
            }
            if (node.nextWaiter != null) {
                unlinkCancelledWaiters();
            }
            if (interruptMode != 0) {
                reportInterruptAfterWait(interruptMode);
            }
            return deadline - System.nanoTime();
        }

        @Override
        public boolean await(long time, TimeUnit unit) throws InterruptedException {
            long nanosTimeout = unit.toNanos(time);
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold) {
                    LockSupport.parkNanos(this, nanosTimeout);
                }
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0) {
                    break;
                }
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE) {
                interruptMode = REINTERRUPT;
            }
            if (node.nextWaiter != null) {
                unlinkCancelledWaiters();
            }
            if (interruptMode != 0) {
                reportInterruptAfterWait(interruptMode);
            }
            return !timedout;
        }

        @Override
        public boolean awaitUntil(Date deadLine)
                throws InterruptedException {
            long abstime = deadLine.getTime();
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            Node node = addConditionWaiter();

            int savedState = fullyRelease(node);
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (System.currentTimeMillis() > abstime) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                LockSupport.parkUntil(this, abstime);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0) {
                    break;
                }
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE) {
                interruptMode = REINTERRUPT;
            }
            if (node.nextWaiter != null) {
                unlinkCancelledWaiters();
            }
            if (interruptMode != 0) {
                reportInterruptAfterWait(interruptMode);
            }
            return !timedout;
        }

        private int checkInterruptWhileWaiting(Node node) {
            return Thread.interrupted() ?
                    (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) :
                    0;
        }

        private void reportInterruptAfterWait(int interruptMode) throws InterruptedException {
            if (interruptMode == THROW_IE) {
                throw new InterruptedException();
            } else if (interruptMode == REINTERRUPT) {
                selfInterrupt();
            }
        }

        final boolean isOwnedBy(MyAbstractQueuedSynchronizer synchronizer) {
            return synchronizer == MyAbstractQueuedSynchronizer.this;
        }

        //下面三个方法思路都是很相近的.
        protected final boolean hasWaiters() {
            if (!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION) {
                    return true;
                }
            }
            return false;
        }

        protected final int getWaitQueueLength() {
            if (!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            int n = 0;
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION) {
                    ++n;
                }
            }
            return n;
        }

        protected final Collection<Thread> getWaitingThreads() {
            if (!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            ArrayList<Thread> list = new ArrayList<>();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION) {
                    Thread t = w.thread;
                    if (t != null) {
                        list.add(t);
                    }
                }
            }
            return list;
        }
    }

    private boolean transferAfterCancelledWait(Node node) {
        if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
            enq(node);
            return true;
        }
        while (!isOnSyncQueue(node)) {
            Thread.yield();
        }
        return false;
    }

    private void selfInterrupt() {
        Thread.currentThread().interrupt();
    }

    //difference between
    public final void acquire(int arg) {
        //acquire的逻辑:tryAcquire由子类实现.如果获取失败了那么就入队列.
        //如果入队列失败了,那么就打断.addWaiter:封装线程.acquireQueued返回线程被打断的状态.
        if (!tryAcquire(arg) && acquireQueued(addWaiter(Node.EXCLUSIVE), arg)) {
            selfInterrupt();
        }
    }

    public final void acquireInterruptibly(int arg) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        //tryAcquire由子类来实现
        if (!tryAcquire(arg)) {
            doAcquireInterruptibly(arg);
        }
        System.out.println("获取锁成功...");
    }

    public final void acquireShared(int arg) {
        //尝试获取
        if (tryAcquireShared(arg) < 0) {
            //如果小于0不返回,doAcquireShared.
            doAcquireShared(arg);
        }
    }

    //响应中断的获取.
    public final void acquireSharedInterruptibly(int arg) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        //如果没有许可
        if (tryAcquireShared(arg) < 0) {
            doAcquireSharedInterruptibly(arg);
        }
    }
    //中断获取
    private void doAcquireSharedInterruptibly(int arg) throws InterruptedException {
        //先添加等待节点
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            for (; ; ) {
                //获取前一个节点
                final Node p = node.predecessor();
                //如果p是head
                if (p == head) {
                    //尝试获取共享锁.死循环获取
                    int r = tryAcquireShared(arg);
                    //如果获取到了
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null;
                        failed = false;
                        return;
                    }
                }
                //响应中断.如果为true才走后面的方法.什么时候决定要park线程呢.
                //如果前一个节点是signal
                //什么时候节点会被设置成signal呢?
                if (shouldParkAfterFailedAcquire(p, node) &&
                        parkAndCheckInterrupt()) {
                    throw new InterruptedException();
                }
            }
        } finally {
            if (failed) {
                cancelAcquire(node);
            }
        }
    }

    final boolean apparentlyFirstQueuedIsExclusive() {
        Node s, h;
        return (h = head) != null &&
                (s = h.next) != null &&
                !s.isShared() &&
                s.thread != null;
    }

    private void doAcquireShared(int arg) {
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (; ; ) {
                final Node p = node.predecessor();
                //一直等待当前的节点是头结点为止.
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, arg);
                        p.next = null;
                        if (interrupted) {
                            selfInterrupt();
                        }
                        failed = false;
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()) {
                    interrupted = true;
                }
            }
        } finally {
            if (failed) {
                cancelAcquire(node);
            }
        }
    }

    private void doAcquireInterruptibly(int arg) throws InterruptedException {
        //添加等待节点
        final Node node = addWaiter(Node.EXCLUSIVE);
        //默认是失败的,如果成功了,更新下
        boolean failed = true;
        try {
            //死循环
            for (; ; ) {
                //获取前节点
                final Node p = node.predecessor();
                //乐观获取一次
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null;
                    failed = false;
                    return;
                }
                //和acquire比这里是有区别的.
                if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()) {
                    throw new InterruptedException();
                }
            }
        } finally {
            if (failed) {
                cancelAcquire(node);
            }
        }
    }

    //
    private boolean acquireQueued(Node node, int arg) {
        boolean interrupted = false;
        try {
            //是否被打断
            for (; ; ) {
                //先获取前面的节点
                final Node p = node.predecessor();
                //如果之前是head.那么就在tryAcquire一次.
                if (p == head && tryAcquire(arg)) {
                    //获取成功了.
                    setHead(node);
                    //
                    p.next = null;
                    return interrupted;
                }
                //说明前面的不是头节点.或者没有tryAcquire成功.
                //是否要park该线程.写个测试用例测测.
                if (shouldParkAfterFailedAcquire(p, node))
                    //位运算
                    interrupted |= parkAndCheckInterrupt();
            }
        } catch (Throwable t) {
            cancelAcquire(node);
            if (interrupted) {
                selfInterrupt();
            }
            throw t;
        }
    }

    //取消获取的时候的逻辑:
    private void cancelAcquire(Node node) {
        if (node == null) {
            return;
        }
        node.thread = null;
        Node pred = node.prev;
        while (pred.waitStatus > 0) {
            node.prev = pred = pred.prev;
        }
        Node predNext = pred.next;
        node.waitStatus = Node.CANCELLED;
        if (node == tail && compareAndSetTail(node, pred)) {
            compareAndSetNext(pred, predNext, null);
        } else {
            int ws;
            //判断逻辑:
            if (pred != head && ((ws = pred.waitStatus) == Node.SIGNAL ||
                    (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&
                    pred.thread != null) {
                Node next = node.next;
                if (next != null && next.waitStatus <= 0) {
                    compareAndSetNext(pred, predNext, next);
                } else {
                    unparkSuccessor(node);
                }
            }
            node.next = node;//help gc
        }
    }

    private boolean parkAndCheckInterrupt() {
        //park当前的线程
        LockSupport.park(this);
        //线程被唤醒之后返回线程是否被打断过.
        //currentThread().isInterrupted(true);native方法
        return Thread.interrupted();
    }

    //前置节点和当前节点.节点的signal是何时赋值的呢?
    private boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        //获取前置节点的等待状态
        int ws = pred.waitStatus;
        //如果是signal.返回true.如果之前的节点是Signal说明当前的节点需要unpark.为什么返回true呢?
        if (ws == Node.SIGNAL) {
            //是不是还没有park过,所以要park一下,后面才有unpark的语义.
            return true;
        }
        //大于0是取消了.
        if (ws > 0) {
            //过滤掉取消的节点
            do {
                //把pred.prev同时设置给node.prev和pred
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            //连接
            pred.next = node;
        } else {//如果ws<=0.如果前置节点的等待状态小于等于0
            //cas设置pred的等待状态.设置为signal.
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }
        return false;
    }

    //把node的线程置空.因为已经获取成功了.
    private void setHead(Node node) {
        head = node;
        node.thread = null;
        node.prev = null;
    }

    //
    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }

    public final boolean tryAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        return tryAcquire(arg) || doAcquireNanos(arg, nanosTimeout);
    }

    protected int tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }

    public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        return tryAcquireShared(arg) >= 0 || doAcquireSharedNanos(arg, nanosTimeout);
    }

    public final boolean owns(ConditionObject condition) {
        return condition.isOwnedBy(this);
    }

    //
    public final boolean isQueued(Thread thread) {
        if (thread == null) {
            throw new NullPointerException();
        }
        for (Node p = tail; p != null; p = p.prev) {
            if (p.thread == thread) {
                return true;
            }
        }
        return false;
    }

    public final boolean hasQueuedThreads() {
        return head != tail;
    }

    public final boolean hasContended() {
        return head != null;
    }

    public final Thread getFirstQueuedThread() {
        return (head == tail) ? null : fullGetFirstQueuedThread();
    }

    //
    private Thread fullGetFirstQueuedThread() {
        Node h, s;
        Thread st;
        //
        if (((h = head) != null && (s = h.next) != null &&
                s.prev == head && (st = s.thread) != null) ||
                ((h = head) != null && (s = h.next) != null &&
                        s.prev == head && (st = s.thread) != null)) {
            return st;
        }
        Node t = tail;
        Thread firstThread = null;
        while (t != null && t != head) {
            Thread tt = t.thread;
            if (tt != null) {
                firstThread = tt;
            }
            t = t.prev;
        }
        return firstThread;
    }

    public final boolean hasQueuedPredecessors() {
        Node t = tail;
        Node h = head;
        Node s;
        return h != t &&
                ((s = h.next) == null || s.thread != Thread.currentThread());
    }

    public final int getQueueLength() {
        int n = 0;
        for (Node p = tail; p != null; p = p.prev) {
            if (p.thread != null) {
                ++n;
            }
        }
        return n;
    }

    public final Collection<Thread> getWaitingThreads(ConditionObject condition) {
        if (!owns(condition)) {
            throw new IllegalArgumentException("Not owner");
        }
        return condition.getWaitingThreads();
    }

    public final Collection<Thread> getQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<>();
        for (Node p = tail; p != null; p = p.prev) {
            Thread t = p.thread;
            if (t != null) {
                list.add(t);
            }
        }
        return list;
    }

    public final int getWaitQueueLength(MyAbstractQueuedSynchronizer.ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitQueueLength();
    }

    public final Collection<Thread> getExclusiveQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<>();
        for (Node p = tail; p != null; p = p.prev) {
            //非共享的才加入
            if (!p.isShared()) {
                Thread t = p.thread;
                if (t != null) {
                    list.add(t);
                }
            }
        }
        return list;
    }

    public final Collection<Thread> getSharedQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<>();
        for (Node p = tail; p != null; p = p.prev) {
            //非共享的才加入
            if (p.isShared()) {
                Thread t = p.thread;
                if (t != null) {
                    list.add(t);
                }
            }
        }
        return list;
    }

    @Override
    public String toString() {
        int s = getState();
        String q = hasQueuedThreads() ? "non" : "";
        return super.toString() + "[State = " + s + "," + q + "empty queue";
    }

    public final boolean hasWaiters(ConditionObject condition) {
        if (!owns(condition)) {
            throw new IllegalArgumentException("Not owner");
        }
        return condition.hasWaiters();
    }

    private boolean doAcquireSharedNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (nanosTimeout <= 0L) {
            return false;
        }
        final long deadline = System.nanoTime() + nanosTimeout;
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            for (; ; ) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null;
                        failed = false;
                        return true;
                    }
                }
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L) {
                    return false;
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                        nanosTimeout > spinForTimeoutThreshold) {
                    LockSupport.parkNanos(this, nanosTimeout);
                }
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
            }
        } finally {
            if (failed) {
                cancelAcquire(node);
            }
        }
    }

    private void setHeadAndPropagate(Node node, int propagate) {
        Node h = head;
        setHead(node);
        if (propagate > 0 || h == null || h.waitStatus < 0 ||
                (h = head) == null || h.waitStatus < 0) {
            Node s = node.next;
            if (s == null || s.isShared()) {
                doReleaseShared();
            }
        }
    }

    private Node addWaiter(Node mode) {
        //封装新的节点
        Node node = new Node(Thread.currentThread(), mode);
        //
        Node pred = tail;
        if (pred != null) {
            //当前节点的前置节点设置
            node.prev = pred;
            //cas设置尾节点.把node设置成tail
            if (compareAndSetTail(pred, node)) {
                //这一步还没有执行到,其他的线程执行遍历,如果从头遍历,少了一个指针
                pred.next = node;
                return node;
            }
        }
        //cas失败了,会走到这一步
        enq(node);
        //死循环去报成功,返回node
        return node;
    }

    private boolean doAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
        if (nanosTimeout <= 0L) {
            return false;
        }
        final long deadline = System.nanoTime() + nanosTimeout;
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            for (; ; ) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null;
                    failed = false;
                    return true;
                }
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L) {
                    return false;
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                        nanosTimeout > spinForTimeoutThreshold) {
                    LockSupport.parkNanos(this, nanosTimeout);
                }
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
            }
        } finally {
            if (failed) {
                cancelAcquire(node);
            }
        }
    }

    //节点是否在SyncQueue里.
    final boolean isOnSyncQueue(Node node) {
        if (node.waitStatus == Node.CONDITION || node.prev == null) {
            return false;
        }
        if (node.next != null) {
            return true;
        }
        return findNodeFromTail(node);
    }

    //从尾部往前找
    private boolean findNodeFromTail(Node node) {
        Node t = tail;
        for (; ; ) {
            if (t == node) {
                return true;
            }
            if (t == null) {
                return false;
            }
            t = t.prev;
        }
    }

    //
    private int fullyRelease(Node node) {
        boolean failed = true;
        try {
            //获取现在的state的状态,大于0
            int savedState = getState();
            if (release(savedState)) {
                failed = false;
                return savedState;
            } else {
                throw new IllegalMonitorStateException();
            }
        } finally {
            if (failed) {
                node.waitStatus = Node.CANCELLED;
            }
        }
    }

    public final boolean release(int arg) {
        if (tryRelease(arg)) {
            Node h = head;
            if (h != null && h.waitStatus != 0) {
                unparkSuccessor(h);
            }
            //写上面的括号里了.
            return true;
        }
        return false;
    }

    public final boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) {
            doReleaseShared();
            return true;
        }
        return false;
    }

    private void doReleaseShared() {
        for (; ; ) {
            Node h = head;
            if (h != null && h != tail) {
                int ws = h.waitStatus;
                if (ws == Node.SIGNAL) {
                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0)) {
                        continue;
                    }
                    unparkSuccessor(h);
                } else if (ws == 0 && !compareAndSetWaitStatus(h, 0, Node.PROPAGATE)) {
                    continue;
                }
            }
            if (h == head) {
                break;
            }
        }
    }

    private boolean tryReleaseShared(int arg) {
        throw new UnsupportedOperationException();
    }

    //唤醒后面的节点
    private void unparkSuccessor(Node node) {
        //先获取node的状态
        int ws = node.waitStatus;
        //如果小于0,cas修改成0
        if (ws < 0) {
            compareAndSetWaitStatus(node, ws, 0);
        }
        //获取node的下一个节点
        Node s = node.next;
        //
        if (s == null || s.waitStatus > 0) {
            s = null;
            //从尾部开始:从tail往前找.t不能是node.不能唤醒自己.
            for (Node t = tail; t != null && t != node; t = t.prev) {
                if (t.waitStatus <= 0) {
                    //一直找到第一个要唤醒的节点
                    s = t;
                }
            }
        }
        //抄到上面的循环里了,导致一直没有unpark.干
        if (s != null) {
            LockSupport.unpark(s.thread);
        }
    }

    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }


    //
    final boolean transferForSignal(Node node) {
        if (!compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
            return false;
        }
        //node入队列
        Node p = enq(node);
        int ws = p.waitStatus;
        //如果ws <= 0.且cas成功.返回true.否则设置singal失败,则unpark线程.
        if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL)) {
            LockSupport.unpark(node.thread);
        }
        return true;
    }

    //核心:加入队列
    private Node enq(final Node node) {
        for (; ; ) {
            Node t = tail;
            if (t == null) {
                if (compareAndSetHead(new Node())) {
                    tail = head;
                }
            } else {
                node.prev = t;
                if (compareAndSetTail(t, node)) {
                    t.next = node;
                    return t;
                }
            }
        }
    }

    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }
}
