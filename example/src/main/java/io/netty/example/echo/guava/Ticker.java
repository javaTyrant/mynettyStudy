package io.netty.example.echo.guava;

/**
 * 可以自己实现
 *
 * @author lufengxiang
 * @since 2021/5/13
 **/
public abstract class Ticker {
    private static final Ticker SYSTEM_TICKER = new Ticker() {
        @Override
        public long read() {
            return Platform.systemNanoTime();
        }
    };

    protected Ticker() {

    }

    public abstract long read();

    public static Ticker systemTicker() {
        return SYSTEM_TICKER;
    }
}
