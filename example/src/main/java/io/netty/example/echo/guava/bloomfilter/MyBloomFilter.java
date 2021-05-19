package io.netty.example.echo.guava.bloomfilter;

import com.google.common.hash.Funnel;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * A Bloom filter for instances of T.
 * A Bloom filter offers an approximate containment test with one-sided error:
 * if it claims that an element is contained in it, this might be in error,
 * but if it claims that an element is not contained in it, then this is definitely true.
 * 如果布隆过滤器说:它拥有某个元素,它可能是错误的,如果它说没有某个元素,那么一定是对的.
 * 底层的数据结构是什么呢,如果加入两个元素,数据结构是如何变化的.
 * 提供了怎样的扩展性呢?
 *
 * @author lufengxiang
 * @since 2021/5/17
 **/
public class MyBloomFilter<T> implements Predicate<T>, Serializable {
    interface Strategy extends Serializable {
        <T> boolean put(T object, Funnel<? super T> funnel, int numHashFunctions, BloomFilterStrategies.LockFreeBitArray bits);

        <T> boolean mightContain(
                T object, Funnel<? super T> funnel, int numHashFunctions, BloomFilterStrategies.LockFreeBitArray bits);

        int ordinal();
    }

    @Override
    public boolean test(T t) {
        return false;
    }

    public static void main(String[] args) {
        Integer a = 97;
        Integer b = 97;
        System.out.println(a == b);
    }
}
