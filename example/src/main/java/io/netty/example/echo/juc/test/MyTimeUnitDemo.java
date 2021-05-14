package io.netty.example.echo.juc.test;

import io.netty.example.echo.juc.MyTimeUnit;

import java.util.concurrent.TimeUnit;

/**
 * @author lufengxiang
 * @since 2021/5/14
 **/
public class MyTimeUnitDemo {
    //原理:
    public static void main(String[] args) throws InterruptedException {
        System.out.println(System.currentTimeMillis());
        MyTimeUnit.SECONDS.sleep(10);
        System.out.println("睡眠结束" + System.currentTimeMillis());
    }
}
