package io.netty.example.echo.guava.bloomfilter;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * @author lufengxiang
 * @since 2021/5/17
 **/
@SuppressWarnings("unused")
final class LongAdder extends Striped64 implements Serializable, LongAddable {
    private static final long serialVersionUID = 7249069246863182397L;

    /**
     * Version of plus for use in retryUpdate
     */
    @Override
    final long fn(long v, long x) {
        return v + x;
    }

    /**
     * Creates a new adder with initial sum of zero.
     */
    public LongAdder() {
    }

    /**
     * Adds the given value.
     *
     * @param x the value to add
     */
    @Override
    public void add(long x) {
        Striped64.Cell[] as;
        long b, v;
        int[] hc;
        Striped64.Cell a;
        int n;
        if ((as = cells) != null || !casBase(b = base, b + x)) {
            boolean uncontended = true;
            if ((hc = threadHashCode.get()) == null
                    || as == null
                    || (n = as.length) < 1
                    || (a = as[(n - 1) & hc[0]]) == null
                    || !(uncontended = a.cas(v = a.value, v + x))) retryUpdate(x, hc, uncontended);
        }
    }

    /**
     * Equivalent to {@code add(1)}.
     */
    @Override
    public void increment() {
        add(1L);
    }

    /**
     * Equivalent to {@code add(-1)}.
     */
    public void decrement() {
        add(-1L);
    }

    /**
     * Returns the current sum. The returned value is <em>NOT</em> an atomic snapshot; invocation in
     * the absence of concurrent updates returns an accurate result, but concurrent updates that occur
     * while the sum is being calculated might not be incorporated.
     *
     * @return the sum
     */
    @Override
    public long sum() {
        long sum = base;
        Striped64.Cell[] as = cells;
        if (as != null) {
            int n = as.length;
            for (Cell a : as) {
                if (a != null) sum += a.value;
            }
        }
        return sum;
    }

    /**
     * Resets variables maintaining the sum to zero. This method may be a useful alternative to
     * creating a new adder, but is only effective if there are no concurrent updates. Because this
     * method is intrinsically racy, it should only be used when it is known that no threads are
     * concurrently updating.
     */
    public void reset() {
        internalReset(0L);
    }

    /**
     * Equivalent in effect to {@link #sum} followed by {@link #reset}. This method may apply for
     * example during quiescent points between multithreaded computations. If there are updates
     * concurrent with this method, the returned value is <em>not</em> guaranteed to be the final
     * value occurring before the reset.
     *
     * @return the sum
     */
    public long sumThenReset() {
        long sum = base;
        Striped64.Cell[] as = cells;
        base = 0L;
        if (as != null) {
            int n = as.length;
            for (Cell a : as) {
                if (a != null) {
                    sum += a.value;
                    a.value = 0L;
                }
            }
        }
        return sum;
    }

    /**
     * Returns the String representation of the {@link #sum}.
     *
     * @return the String representation of the {@link #sum}
     */
    @Override
    public String toString() {
        return Long.toString(sum());
    }

    /**
     * Equivalent to {@link #sum}.
     *
     * @return the sum
     */
    @Override
    public long longValue() {
        return sum();
    }

    /**
     * Returns the {@link #sum} as an {@code int} after a narrowing primitive conversion.
     */
    @Override
    public int intValue() {
        return (int) sum();
    }

    /**
     * Returns the {@link #sum} as a {@code float} after a widening primitive conversion.
     */
    @Override
    public float floatValue() {
        return (float) sum();
    }

    /**
     * Returns the {@link #sum} as a {@code double} after a widening primitive conversion.
     */
    @Override
    public double doubleValue() {
        return (double) sum();
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeLong(sum());
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        busy = 0;
        cells = null;
        base = s.readLong();
    }
}
