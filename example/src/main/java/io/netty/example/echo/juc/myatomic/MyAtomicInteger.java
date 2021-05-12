package io.netty.example.echo.juc.myatomic;

import sun.misc.Unsafe;

import java.io.Serializable;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

/**
 * 很简单的一个类,就是对unsafe的一次封装.
 *
 * @author lufengxiang
 * @since 2021/5/10
 **/
@SuppressWarnings("unused")
public class MyAtomicInteger extends Number implements Serializable {
    private static final long serialVersionUID = 6214790243416807050L;
    //
    private static final Unsafe unsafe;
    //
    private static final long valueOffset;
    //
    private volatile int value;

    static {
        try {
            unsafe = Unsafe.getUnsafe();
            //获取偏移量
            valueOffset = unsafe.objectFieldOffset(MyAtomicInteger.class.getDeclaredField("value"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public final int get() {
        return value;
    }

    public final void set(int newValue) {
        value = newValue;
    }

    //直接更新
    public final int getAndSet(int newValue) {
        return unsafe.getAndAddInt(this, valueOffset, newValue);
    }

    //cas更新
    public final boolean compareAndSet(int expect, int update) {
        return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
    }

    //现在的实现跟上面是一样的呢?
    public final boolean weakCompareAndSet(int expect, int update) {
        return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
    }

    public final int getAndIncrement() {
        return unsafe.getAndAddInt(this, valueOffset, 1);
    }

    public final int getAndDecrement() {
        return unsafe.getAndAddInt(this, valueOffset, -1);
    }

    public final int getAndAdd(int delta) {
        return unsafe.getAndAddInt(this, valueOffset, delta);
    }

    public final int incrementAndGet() {
        return unsafe.getAndAddInt(this, valueOffset, 1) + 1;
    }

    public final int decrementAndGet() {
        return unsafe.getAndAddInt(this, valueOffset, -1) - 1;
    }

    public final int addAndGet(int delta) {
        return unsafe.getAndAddInt(this, valueOffset, delta) + delta;
    }

    public final int getAndUpdate(IntUnaryOperator updateFunction) {
        int prev, next;
        do {
            prev = get();
            next = updateFunction.applyAsInt(prev);
        } while (!compareAndSet(prev, next));
        return prev;
    }

    public final int updateAndGet(IntUnaryOperator updateFunction) {
        int prev, next;
        do {
            prev = get();
            next = updateFunction.applyAsInt(prev);
        } while (!compareAndSet(prev, next));
        return next;
    }

    public final int getAndAccumulate(int x,
                                      IntBinaryOperator accumulatorFunction) {
        int prev, next;
        do {
            prev = get();
            next = accumulatorFunction.applyAsInt(prev, x);
        } while (!compareAndSet(prev, next));
        return prev;
    }

    public final int accumulateAndGet(int x,
                                      IntBinaryOperator accumulatorFunction) {
        int prev, next;
        do {
            prev = get();
            next = accumulatorFunction.applyAsInt(prev, x);
        } while (!compareAndSet(prev, next));
        return next;
    }

    public String toString() {
        return Integer.toString(get());
    }

    public int intValue() {
        return get();
    }

    public long longValue() {
        return (long) get();
    }

    public float floatValue() {
        return (float) get();
    }

    public double doubleValue() {
        return (double) get();
    }
}
