package io.netty.example.echo.guava.bloomfilter;

import com.google.common.base.Supplier;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author lufengxiang
 * @since 2021/5/17
 **/
final class LongAddables {
    private static final Supplier<LongAddable> SUPPLIER;

    static {
        Supplier<LongAddable> supplier;
        try {
            new LongAdder(); // trigger static initialization of the LongAdder class, which may fail
            supplier =
                    new Supplier<LongAddable>() {
                        @Override
                        public LongAddable get() {
                            return new LongAdder();
                        }
                    };
        } catch (Throwable t) { // we really want to catch *everything*
            supplier =
                    new Supplier<LongAddable>() {
                        @Override
                        public LongAddable get() {
                            return new LongAddables.PureJavaLongAddable();
                        }
                    };
        }
        SUPPLIER = supplier;
    }

    public static LongAddable create() {
        return SUPPLIER.get();
    }

    private static final class PureJavaLongAddable extends AtomicLong implements LongAddable {
        @Override
        public void increment() {
            getAndIncrement();
        }

        @Override
        public void add(long x) {
            getAndAdd(x);
        }

        @Override
        public long sum() {
            return get();
        }
    }
}
