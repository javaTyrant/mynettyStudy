package io.netty.example.echo.juc;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

/**
 * 手抄FutureTask
 * 心领神会
 *
 * @author lufengxiang
 * @since 2021/4/19
 **/
@SuppressWarnings("unused")
public class MyFutureTask<V> implements RunnableFuture<V> {
    //任务的状态
    private volatile int state;
    //状态枚举值
    private static final int NEW = 0;
    private static final int COMPLETING = 1;
    private static final int NORMAL = 2;
    private static final int EXCEPTIONAL = 3;
    private static final int CANCELLED = 4;
    private static final int INTERRUPTING = 1;
    //cas
    private static final sun.misc.Unsafe UNSAFE;
    private static final long stateOffset;
    private static final long runnerOffset;
    private static final long waitersOffset;

    static {
        try {
            UNSAFE = createUnsafe();
            Class<?> k = MyFutureTask.class;
            stateOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("state"));
            runnerOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("runner"));
            waitersOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("waiters"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    //
    private Callable<V> callable;
    //输出结果
    private Object outcome;

    //干活者
    private volatile Thread runner;

    //等待链表
    private volatile WaitNode waiters;

    @SuppressWarnings("unchecked")
    private V report(int s) throws ExecutionException {
        Object x = outcome;
        if (s == NORMAL) {
            return (V) x;
        }
        if (s >= CANCELLED)
            throw new CancellationException();
        throw new ExecutionException((Throwable) x);
    }

    public MyFutureTask(Callable<V> callable) {
        if (callable == null) {
            throw new NullPointerException();
        }
        this.callable = callable;
        this.state = NEW;
    }

    public MyFutureTask(Runnable runnable, V result) {
        //runnable转callable
        this.callable = Executors.callable(runnable, result);
        this.state = NEW;
    }

    static final class WaitNode {
        //一个线程
        volatile Thread thread;
        //一个指针
        volatile WaitNode next;

        WaitNode() {
            thread = Thread.currentThread();
        }

    }

    @Override
    public void run() {
        if (state != NEW || !UNSAFE.compareAndSwapObject(this, runnerOffset,
                null, Thread.currentThread())) {
            return;
        }
        try {
            Callable<V> c = callable;
            if (c != null && state == NEW) {
                V result;
                boolean ran;
                try {
                    result = c.call();
                    ran = true;
                } catch (Throwable ex) {
                    result = null;
                    ran = false;
                    setException(ex);
                }
                if (ran) {
                    set(result);
                }
            }
        } finally {
            runner = null;
            int s = state;
            if (s >= INTERRUPTING) {
                handlePossibleCancellationInterrupt(s);
            }
        }
    }

    private void handlePossibleCancellationInterrupt(int s) {
        if (s == INTERRUPTING) {
            while (state == INTERRUPTING) {
                Thread.yield();
            }
        }
    }

    protected void setException(Throwable t) {
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            outcome = t;
            UNSAFE.putOrderedInt(this, stateOffset, EXCEPTIONAL);
            finishCompletion();
        }
    }

    protected void set(V v) {
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            outcome = v;
            UNSAFE.putOrderedInt(this, stateOffset, NORMAL);
            finishCompletion();
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        //1.如果state 不等于 new return false.直接返回false
        //2.如果cas失败 直接返回false.
        if (!(state == NEW && UNSAFE.compareAndSwapInt(this, stateOffset, NEW,
                mayInterruptIfRunning ? INTERRUPTING : CANCELLED)))
            return false;
        //state == new and cas成功
        try {
            if (mayInterruptIfRunning) {
                try {
                    Thread t = runner;
                    if (t != null) {
                        //
                        t.interrupt();
                    }
                } finally {
                    //
                    UNSAFE.putOrderedInt(this, stateOffset, INTERRUPTING);
                }
            }
        } finally {
            finishCompletion();
        }
        return true;
    }

    private void finishCompletion() {
        for (WaitNode q; (q = waiters) != null; ) {
            if (UNSAFE.compareAndSwapObject(this, waitersOffset, q, null)) {
                for (; ; ) {
                    Thread t = q.thread;
                    if (t != null) {
                        q.thread = null;
                        LockSupport.unpark(t);
                    }
                    WaitNode next = q.next;
                    if (next == null) {
                        break;
                    }
                    q.next = null;
                    q = next;
                }
                break;
            }
        }
        done();
        callable = null;
    }

    protected void done() {

    }

    @Override
    public boolean isCancelled() {
        return state >= CANCELLED;
    }

    @Override
    public boolean isDone() {
        return state != NEW;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        //获取state
        int s = state;
        //如果没有完成
        if (s <= COMPLETING) {
            //等待完成
            s = awaitDone(false, 0L);
        }
        return report(s);
    }

    protected boolean runAndReset() {
        if (state != NEW || !UNSAFE.compareAndSwapObject(this, runnerOffset, null, Thread.currentThread())) {
            return false;
        }
        boolean ran = false;
        int s = state;
        try {
            Callable<V> c = callable;
            if (c != null && s == NEW) {
                try {
                    c.call();
                    ran = true;
                } catch (Throwable ex) {
                    setException(ex);
                }
            }
        } finally {
            runner = null;
            s = state;
            if (s >= INTERRUPTING) {
                handlePossibleCancellationInterrupt(s);
            }
        }
        return ran && s == NEW;
    }

    //核心逻辑
    private int awaitDone(boolean timed, long nanos)
            throws InterruptedException {
        //截止日期
        final long deadline = timed ? System.nanoTime() + nanos : 0L;
        //等待节点
        WaitNode q = null;
        //是否入队列
        boolean queued = false;
        for (; ; ) {
            //如果线程被打断
            if (Thread.interrupted()) {
                removeWaiter(q);
                throw new InterruptedException();
            }
            //
            int s = state;
            //
            if (s > COMPLETING) {
                //已完成了,且q != null 把q的线程置空
                if (q != null) {
                    q.thread = null;
                }
                //成功返回state
                return s;
            } else if (s == COMPLETING) {
                //完成中:A hint to the scheduler that the current thread is willing to yield its current use of a processor.
                Thread.yield();
            } else if (q == null) {//初始化等待节点
                //构造一个新节点
                q = new WaitNode();
            } else if (!queued) {//没有入队列
                //waiters入队列:cas入队列.cas需要的对象.类对象,偏移量.比较q的地址.
                queued = UNSAFE.compareAndSwapObject(this, waitersOffset, q.next = waiters, q);
            } else if (timed) {//是否超时控制
                //减去消耗的时间
                nanos = deadline - System.nanoTime();
                //已经超时了
                if (nanos <= 0L) {
                    //移除等待节点
                    removeWaiter(q);
                    return state;
                }
                //parkNanos.暂停线程.
                LockSupport.parkNanos(this, nanos);
            }
        }
    }

    //两种情况会移除waiter
    private void removeWaiter(WaitNode node) {
        //
        if (node != null) {
            node.thread = null;
            //为什么一定要用goto呢.
            retry:
            for (; ; ) {
                for (WaitNode pred = null, q = waiters, s; q != null; q = s) {
                    s = q.next;
                    if (q.thread != null) {
                        pred = q;
                    } else if (pred != null) {
                        pred.next = s;
                        if (pred.thread == null) {
                            continue retry;
                        }
                    } else if (!UNSAFE.compareAndSwapObject(this, waitersOffset, q, s)) {
                        continue retry;
                    }
                }
                break;
            }
        }
    }

    @Override
    public V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        if (unit == null) {
            throw new NullPointerException();
        }
        int s = state;
        if (s <= COMPLETING &&
                (s = awaitDone(true, unit.toNanos(timeout))) <= COMPLETING) {
            throw new TimeoutException();
        }
        return report(s);
    }

    //反射获取unsafe.
    public static Unsafe createUnsafe() {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
