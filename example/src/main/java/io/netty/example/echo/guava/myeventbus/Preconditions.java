package io.netty.example.echo.guava.myeventbus;

/**
 * @author lumac
 * @since 2021/5/15
 */
public class Preconditions {
    public static <T extends Object> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }
}
