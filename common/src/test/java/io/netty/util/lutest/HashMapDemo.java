package io.netty.util.lutest;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lufengxiang
 * @since 2022/3/2
 **/
public class HashMapDemo {
    //a=i++，这个运算的意思是先把i的值赋予a，然后在执行i=i+1；
    //a=++i，这个的意思是先执行i=i+1，然后在把i的值赋予a；
    public static void main(String[] args) {
        Map<Integer, Integer> map = new HashMap<>();
        int a = 1;
        int b = 1;
        int d = ++a;
        System.out.println(d);
        for (int i = 0; i < 16; i++) {
            map.put(i, i);
        }
    }

    //寻找峰值
    public int findPeakElement(int[] nums) {
        return 0;
    }
}
