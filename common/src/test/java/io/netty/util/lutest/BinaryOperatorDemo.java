package io.netty.util.lutest;

import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

/**
 * @author lufengxiang
 * @since 2022/3/1
 **/
public class BinaryOperatorDemo {
    static BiFunction<Integer, Integer, Integer> func = (x1, x2) -> x1 + x2;
    static BiFunction<Integer, Integer, Integer> func1 = Integer::sum;
    static BinaryOperator<Integer> func2 = (x1, x2) -> x1 + x2;

    public static void main(String[] args) {
        System.out.println(func.apply(1, 2));
        System.out.println(func2.apply(23, 3));
    }
}
