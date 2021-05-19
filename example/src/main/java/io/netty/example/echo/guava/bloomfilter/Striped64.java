package io.netty.example.echo.guava.bloomfilter;

import java.util.Random;

/**
 * @author lufengxiang
 * @since 2021/5/17
 **/
abstract class Striped64 extends Number {
    static final class Cell {
        volatile long p0, p1, p2, p3, p4, p5, p6;
        volatile long value;
        volatile long q0, q1, q2, q3, q4, q5, q6;

        Cell(long x) {
            value = x;
        }

        final boolean cas(long cmp, long val) {
            return UNSAFE.compareAndSwapLong(this, valueOffset, cmp, val);
        }

        // Unsafe mechanics
        private static final sun.misc.Unsafe UNSAFE;
        private static final long valueOffset;

        static {
            try {
                UNSAFE = getUnsafe();
                Class<?> ak = Striped64.Cell.class;
                valueOffset = UNSAFE.objectFieldOffset(ak.getDeclaredField("value"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /**
     * ThreadLocal holding a single-slot int array holding hash code. Unlike the JDK8 version of this
     * class, we use a suboptimal int[] representation to avoid introducing a new type that can impede
     * class-unloading when ThreadLocals are not removed.
     */
    static final ThreadLocal<int[]> threadHashCode = new ThreadLocal<>();

    /**
     * Generator of new random hash codes
     */
    static final Random rng = new Random();

    /**
     * Number of CPUS, to place bound on table size
     */
    static final int NCPU = Runtime.getRuntime().availableProcessors();

    /**
     * Table of cells. When non-null, size is a power of 2.
     */
    transient volatile Striped64.Cell[] cells;

    /**
     * Base value, used mainly when there is no contention, but also as a fallback during table
     * initialization races. Updated via CAS.
     */
    transient volatile long base;

    /**
     * Spinlock (locked via CAS) used when resizing and/or creating Cells.
     */
    transient volatile int busy;

    /**
     * Package-private default constructor
     */
    Striped64() {
    }

    /**
     * CASes the base field.
     */
    final boolean casBase(long cmp, long val) {
        return UNSAFE.compareAndSwapLong(this, baseOffset, cmp, val);
    }

    /**
     * CASes the busy field from 0 to 1 to acquire lock.
     */
    final boolean casBusy() {
        return UNSAFE.compareAndSwapInt(this, busyOffset, 0, 1);
    }

    /**
     * Computes the function of current and new value. Subclasses should open-code this update
     * function for most uses, but the virtualized form is needed within retryUpdate.
     *
     * @param currentValue the current value (of either base or a cell)
     * @param newValue     the argument from a user update call
     * @return result of the update function
     */
    abstract long fn(long currentValue, long newValue);

    /**
     * Handles cases of updates involving initialization, resizing, creating new Cells, and/or
     * contention. See above for explanation. This method suffers the usual non-modularity problems of
     * optimistic retry code, relying on rechecked sets of reads.
     *
     * @param x              the value
     * @param hc             the hash code holder
     * @param wasUncontended false if CAS failed before call
     */
    final void retryUpdate(long x, int[] hc, boolean wasUncontended) {
        int h;
        if (hc == null) {
            threadHashCode.set(hc = new int[1]); // Initialize randomly
            int r = rng.nextInt(); // Avoid zero to allow xorShift rehash
            h = hc[0] = (r == 0) ? 1 : r;
        } else h = hc[0];
        boolean collide = false; // True if last slot nonempty
        for (; ; ) {
            Striped64.Cell[] as;
            Striped64.Cell a;
            int n;
            long v;
            if ((as = cells) != null && (n = as.length) > 0) {
                if ((a = as[(n - 1) & h]) == null) {
                    if (busy == 0) { // Try to attach new Cell
                        Striped64.Cell r = new Striped64.Cell(x); // Optimistically create
                        if (busy == 0 && casBusy()) {
                            boolean created = false;
                            try { // Recheck under lock
                                Striped64.Cell[] rs;
                                int m, j;
                                if ((rs = cells) != null && (m = rs.length) > 0 && rs[j = (m - 1) & h] == null) {
                                    rs[j] = r;
                                    created = true;
                                }
                            } finally {
                                busy = 0;
                            }
                            if (created) break;
                            continue; // Slot is now non-empty
                        }
                    }
                    collide = false;
                } else if (!wasUncontended) // CAS already known to fail
                    wasUncontended = true; // Continue after rehash
                else if (a.cas(v = a.value, fn(v, x))) break;
                else if (n >= NCPU || cells != as) collide = false; // At max size or stale
                else if (!collide) collide = true;
                else if (busy == 0 && casBusy()) {
                    try {
                        if (cells == as) { // Expand table unless stale
                            Striped64.Cell[] rs = new Striped64.Cell[n << 1];
                            for (int i = 0; i < n; ++i) rs[i] = as[i];
                            cells = rs;
                        }
                    } finally {
                        busy = 0;
                    }
                    collide = false;
                    continue; // Retry with expanded table
                }
                h ^= h << 13; // Rehash
                h ^= h >>> 17;
                h ^= h << 5;
                hc[0] = h; // Record index for next time
            } else if (busy == 0 && cells == as && casBusy()) {
                boolean init = false;
                try { // Initialize table
                    if (cells == as) {
                        Striped64.Cell[] rs = new Striped64.Cell[2];
                        rs[h & 1] = new Striped64.Cell(x);
                        cells = rs;
                        init = true;
                    }
                } finally {
                    busy = 0;
                }
                if (init) break;
            } else if (casBase(v = base, fn(v, x))) break; // Fall back on using base
        }
    }

    /**
     * Sets base and all cells to the given value.
     */
    final void internalReset(long initialValue) {
        Striped64.Cell[] as = cells;
        base = initialValue;
        if (as != null) {
            int n = as.length;
            for (int i = 0; i < n; ++i) {
                Striped64.Cell a = as[i];
                if (a != null) a.value = initialValue;
            }
        }
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long baseOffset;
    private static final long busyOffset;

    static {
        try {
            UNSAFE = getUnsafe();
            Class<?> sk = Striped64.class;
            baseOffset = UNSAFE.objectFieldOffset(sk.getDeclaredField("base"));
            busyOffset = UNSAFE.objectFieldOffset(sk.getDeclaredField("busy"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    /**
     * Returns a sun.misc.Unsafe. Suitable for use in a 3rd party package. Replace with a simple call
     * to Unsafe.getUnsafe when integrating into a jdk.
     *
     * @return a sun.misc.Unsafe
     */
    private static sun.misc.Unsafe getUnsafe() {
        try {
            return sun.misc.Unsafe.getUnsafe();
        } catch (SecurityException tryReflectionInstead) {
        }
        try {
            return java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedExceptionAction<sun.misc.Unsafe>() {
                        @Override
                        public sun.misc.Unsafe run() throws Exception {
                            Class<sun.misc.Unsafe> k = sun.misc.Unsafe.class;
                            for (java.lang.reflect.Field f : k.getDeclaredFields()) {
                                f.setAccessible(true);
                                Object x = f.get(null);
                                if (k.isInstance(x)) return k.cast(x);
                            }
                            throw new NoSuchFieldError("the Unsafe");
                        }
                    });
        } catch (java.security.PrivilegedActionException e) {
            throw new RuntimeException("Could not initialize intrinsics", e.getCause());
        }
    }
}