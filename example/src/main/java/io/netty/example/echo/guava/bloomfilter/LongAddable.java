package io.netty.example.echo.guava.bloomfilter;

/**
 * @author lufengxiang
 * @since 2021/5/17
 **/
public interface LongAddable {
    void increment();

    void add(long x);

    long sum();
}
