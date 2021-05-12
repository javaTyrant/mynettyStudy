package io.netty.example.echo.juc;

import io.netty.example.echo.juc.mylock.MyCondition;
import io.netty.example.echo.juc.mylock.MyReentrantLock;

import java.util.*;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * 延时队列的核心原理
 * 这个延时队列有什么问题呢?
 * netty是如何优化的?
 * 延时队列为什么不允许为null呢,因为没有超期的元素的时候会返回null
 * 如果是null,就不知道这个null的具体含义了.
 * @author lufengxiang
 * @since 2021/5/7
 **/

@SuppressWarnings("unused")
public class MyDelayQueue<E extends Delayed> extends AbstractQueue<E>
        implements MyBlockingQueue<E> {
    //可重入锁.
    private final transient MyReentrantLock lock = new MyReentrantLock();
    //
    private final PriorityQueue<E> q = new PriorityQueue<>();
    //做什么用的呢?minimize unnecessary timed waiting.如何减少等待的时间呢
    //await变种的区别.
    /**
     * Thread designated to wait for the element at the head of
     * the queue.  This variant of the Leader-Follower pattern
     * (http://www.cs.wustl.edu/~schmidt/POSA/POSA2/) serves to
     * minimize unnecessary timed waiting.  When a thread becomes
     * the leader, it waits only for the next delay to elapse, but
     * other threads await indefinitely.  The leader thread must
     * signal some other thread before returning from take() or
     * poll(...), unless some other thread becomes leader in the
     * interim.  Whenever the head of the queue is replaced with
     * an element with an earlier expiration time, the leader
     * field is invalidated by being reset to null, and some
     * waiting thread, but not necessarily the current leader, is
     * signalled.  So waiting threads must be prepared to acquire
     * and lose leadership while waiting.
     */
    private Thread leader = null;
    //
    private final MyCondition available = lock.newCondition();

    //
    public MyDelayQueue() {

    }

    //pecs:生产者.
    public MyDelayQueue(Collection<? extends E> c) {
        this.addAll(c);
    }

    public boolean add(E e) {
        return offer(e);
    }

    public boolean offer(E e) {
        final MyReentrantLock lock = this.lock;
        lock.lock();
        try {
            q.offer(e);
            if (q.peek() == e) {
                //如果是堆顶元素,leader设为空
                leader = null;
                //signal一下.太棒了.
                available.signal();
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void put(E e) {
        offer(e);
    }

    public boolean offer(E e, long timeout, TimeUnit unit) {
        return offer(e);
    }

    public E poll() {
        final MyReentrantLock lock = this.lock;
        lock.lock();
        try {
            E first = q.peek();
            //如果大于0,就返回null.没有等待.
            if (first == null || first.getDelay(NANOSECONDS) > 0)
                return null;
            else
                return q.poll();
        } finally {
            lock.unlock();
        }
    }
    //精彩的回答.还要再体会下这个leader的设计.
    //如果有新的节点到堆顶了.
    //the leader is not used for minimizing awaitNanos, it is used for avoiding unnecessary wake up & sleep.
    // If you let all threads available.awaitNanos(delay) in take method,
    // they will be called up simultaneously but only one can really get element from the queue,
    // the others will fall into sleeping again, which is unnecessary and resource-wasting.
    // a b两个线程来take.a先到.
    // 此时leader为空,所以leader = 线程a
    // 线程a开始等待.
    // 此时线程b也到了,leader不为空.直接等待.
    // 如果没有leader:
    public E take() throws InterruptedException {
        final MyReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            //死循环.
            for (; ; ) {
                E first = q.peek();
                if (first == null)
                    available.await();
                else {
                    //获取延时的时间
                    long delay = first.getDelay(NANOSECONDS);
                    //如果delay小等于0,直接取出来
                    if (delay <= 0)
                        return q.poll();
                    first = null; // don't retain ref while waiting
                    //
                    if (leader != null)
                        available.await();
                    else {
                        //获取的线程.
                        Thread thisThread = Thread.currentThread();
                        //赋值给leader
                        leader = thisThread;
                        try {
                            //leader为空,等待.
                            available.awaitNanos(delay);
                        } finally {
                            //
                            if (leader == thisThread)
                                leader = null;
                        }
                    }
                }
            }
        } finally {
            if (leader == null && q.peek() != null)
                available.signal();
            lock.unlock();
        }
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        //单位转换.
        long nanos = unit.toNanos(timeout);
        //加锁.
        final MyReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            for (; ; ) {
                E first = q.peek();
                //空判断
                if (first == null) {
                    if (nanos <= 0)
                        return null;
                    else
                        //获取null也要等待
                        nanos = available.awaitNanos(nanos);
                } else {
                    long delay = first.getDelay(NANOSECONDS);
                    if (delay <= 0)
                        return q.poll();
                    if (nanos <= 0)
                        return null;
                    first = null; // don't retain ref while waiting
                    if (nanos < delay || leader != null)
                        nanos = available.awaitNanos(nanos);
                    else {
                        Thread thisThread = Thread.currentThread();
                        leader = thisThread;
                        try {
                            long timeLeft = available.awaitNanos(delay);
                            nanos -= delay - timeLeft;
                        } finally {
                            if (leader == thisThread)
                                leader = null;
                        }
                    }
                }
            }
        } finally {
            if (leader == null && q.peek() != null)
                available.signal();
            lock.unlock();
        }
    }

    public E peek() {
        final MyReentrantLock lock = this.lock;
        lock.lock();
        try {
            return q.peek();
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        final MyReentrantLock lock = this.lock;
        lock.lock();
        try {
            return q.size();
        } finally {
            lock.unlock();
        }
    }

    private E peekExpired() {
        // assert lock.isHeldByCurrentThread();
        E first = q.peek();
        return (first == null || first.getDelay(NANOSECONDS) > 0) ?
                null : first;
    }

    public int drainTo(Collection<? super E> c) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        final MyReentrantLock lock = this.lock;
        lock.lock();
        try {
            int n = 0;
            for (E e; (e = peekExpired()) != null; ) {
                c.add(e);       // In this order, in case add() throws.
                q.poll();
                ++n;
            }
            return n;
        } finally {
            lock.unlock();
        }
    }

    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        if (maxElements <= 0)
            return 0;
        final MyReentrantLock lock = this.lock;
        lock.lock();
        try {
            int n = 0;
            for (E e; n < maxElements && (e = peekExpired()) != null; ) {
                c.add(e);       // In this order, in case add() throws.
                q.poll();
                ++n;
            }
            return n;
        } finally {
            lock.unlock();
        }
    }

    public void clear() {
        final MyReentrantLock lock = this.lock;
        lock.lock();
        try {
            q.clear();
        } finally {
            lock.unlock();
        }
    }

    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    public Object[] toArray() {
        final MyReentrantLock lock = this.lock;
        lock.lock();
        try {
            return q.toArray();
        } finally {
            lock.unlock();
        }
    }

    public <T> T[] toArray(T[] a) {
        final MyReentrantLock lock = this.lock;
        lock.lock();
        try {
            return q.toArray(a);
        } finally {
            lock.unlock();
        }
    }

    public boolean remove(Object o) {
        final MyReentrantLock lock = this.lock;
        lock.lock();
        try {
            return q.remove(o);
        } finally {
            lock.unlock();
        }
    }

    void removeEQ(Object o) {
        final MyReentrantLock lock = this.lock;
        lock.lock();
        try {
            for (Iterator<E> it = q.iterator(); it.hasNext(); ) {
                if (o == it.next()) {
                    it.remove();
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public Iterator<E> iterator() {
        return new Itr(toArray());
    }

    private class Itr implements Iterator<E> {
        final Object[] array; // Array of all elements
        int cursor;           // index of next element to return
        int lastRet;          // index of last element, or -1 if no such

        Itr(Object[] array) {
            lastRet = -1;
            this.array = array;
        }

        public boolean hasNext() {
            return cursor < array.length;
        }

        @SuppressWarnings("unchecked")
        public E next() {
            if (cursor >= array.length)
                throw new NoSuchElementException();
            lastRet = cursor;
            return (E) array[cursor++];
        }

        public void remove() {
            if (lastRet < 0)
                throw new IllegalStateException();
            removeEQ(array[lastRet]);
            lastRet = -1;
        }
    }
}
