package io.netty.util.lutest;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author lufengxiang
 * @since 2022/3/1
 **/
public class Box<T> {
    private T t;

    public void set(T t) {
        this.t = t;
    }

    public T get() {
        return t;
    }

    public <U extends Number> void inspect(U u) {
        System.out.println("T: " + t.getClass().getName());
        System.out.println("U: " + u.getClass().getName());
    }

    public static void main(String[] args) {
        Box<Integer> integerBox = new Box<>();
        integerBox.set(new Integer(10));
        //integerBox.inspect("some text"); // error: this is still String!
        Double d = 2.0;
        BigDecimal decimal = BigDecimal.valueOf(100);
        integerBox.inspect(d);
        integerBox.inspect(decimal);
    }
}
