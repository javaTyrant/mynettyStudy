package io.netty.example.echo.juc;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @author lufengxiang
 * @since 2021/5/6
 **/
public class UnsafeUtil {
    //反射获取unsafe.
    public static Unsafe createUnsafe() {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
