package io.netty.example.echo.timer;

import io.netty.util.Timeout;

/**
 * 有三个内部类.
 *
 * @author lufengxiang
 * @since 2021/5/19
 **/
public class MyHashedWheelTimer {

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
