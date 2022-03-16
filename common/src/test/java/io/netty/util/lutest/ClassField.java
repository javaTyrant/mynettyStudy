package io.netty.util.lutest;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lufengxiang
 * @since 2022/3/7
 **/
public class ClassField {
    private static final AtomicInteger a = new AtomicInteger(0);

    public static AtomicInteger getA() {
        return a;
    }

    public ClassField() {
        a.incrementAndGet();
    }

    public static void main(String[] args) {
        ClassField a = new ClassField();
        ClassField b = new ClassField();
        System.out.println(b.getA());
    }
}
