package io.netty.example.echo.juc;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * @author lumac
 * @since 2021/5/9
 */
public interface MyDelayed extends Comparable<Delayed> {
    /**
     * @param unit
     * @return
     */
    long getDelay(TimeUnit unit);

}
