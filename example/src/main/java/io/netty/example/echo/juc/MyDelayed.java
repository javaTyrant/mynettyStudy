package io.netty.example.echo.juc;

import java.util.concurrent.TimeUnit;

/**
 * @author lumac
 * @since 2021/5/9
 */
public interface MyDelayed {
    /**
     * @param unit
     * @return
     */
    long getDelay(TimeUnit unit);

}
