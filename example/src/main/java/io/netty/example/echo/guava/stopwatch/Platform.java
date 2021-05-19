package io.netty.example.echo.guava.stopwatch;

import java.util.Locale;

/**
 * @author lufengxiang
 * @since 2021/5/13
 **/
public class Platform {
    public static long systemNanoTime() {
        return System.nanoTime();
    }

    static String formatCompact4Digits(double value) {
        return String.format(Locale.ROOT, "%.4g", value);
    }

    public static void main(String[] args) {
        System.out.println(formatCompact4Digits(5.67988888));
    }

}
