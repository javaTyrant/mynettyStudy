package io.netty.example.echo.juc.test;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author lufengxiang
 * @since 2021/4/30
 **/
public class LockDemo {
    public static void main(String[] args) {
        int a = 1, b = 2, c = 3;
        a = b = c;
        System.out.println(a);
        System.out.println(b);
        System.out.println(c);
    }
}
