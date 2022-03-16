package io.netty.util.lutest;

/**
 * @author lufengxiang
 * @since 2022/3/11
 **/
public class ThreadLocalDemo {
    public static void main(String[] args) {
        ThreadLocal local = new ThreadLocal();
        ThreadLocal local1 = new ThreadLocal();
        new Thread(() -> {
            local.set(1);
            local1.set(2);
        }).start();
    }
}
