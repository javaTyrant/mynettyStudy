package io.netty.example.echo.timer;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 有三个内部类.
 *
 * @author lufengxiang
 * @since 2021/5/19
 **/
public class MyHashedWheelTimer {
    static final InternalLogger logger =
            InternalLoggerFactory.getInstance(MyHashedWheelTimer.class);
    //原子Int
    private static final AtomicInteger INSTANCE_COUNTER = new AtomicInteger();

    //是不是有太多的实例.
    private static final AtomicBoolean WARNED_TOO_MANY_INSTANCES = new AtomicBoolean();

    //最多可以有多少个实例
    private static final int INSTANCE_COUNT_LIMIT = 64;

    private static final class HashedWheelBucket {

    }

    private static final class HashedWheelTimeout implements MyTimeout {
        @Override
        public MyTimer timer() {
            return null;
        }

        @Override
        public MyTimerTask task() {
            return null;
        }

        @Override
        public boolean isExpired() {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean cancel() {
            return false;
        }
    }

    private final class Worker implements Runnable {
        @Override
        public void run() {

        }
    }
}
