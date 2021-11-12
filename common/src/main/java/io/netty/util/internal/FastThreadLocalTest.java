package io.netty.util.internal;

import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.concurrent.FastThreadLocalThread;

/**
 * 1.FastThreadLocal 真的一定比 ThreadLocal 快吗？答案是不一定的，只有使用FastThreadLocalThread 类型的线程才会更快，如果是普通线程反而会更慢。
 * 2.FastThreadLocal 会浪费很大的空间吗？虽然 FastThreadLocal 采用的空间换时间的思路，但是在 FastThreadLocal 设计之初就认为不会存在特别多的
 * FastThreadLocal 对象，而且在数据中没有使用的元素只是存放了同一个缺省对象的引用，并不会占用太多内存空间。
 *
 * @author lufengxiang
 * @since 2021/11/8
 **/
public class FastThreadLocalTest {
    //两个threadLocal.
    private static final FastThreadLocal<String> THREAD_NAME_LOCAL = new FastThreadLocal<>();

    private static final FastThreadLocal<TradeOrder> TRADE_THREAD_LOCAL = new FastThreadLocal<>();

    //set的流程:
    //1.
    public static void main(String[] args) {
        for (int i = 0; i < 2; i++) {
            int tradeId = i;
            String threadName = "thread-" + i;
            new FastThreadLocalThread(() -> {
                THREAD_NAME_LOCAL.set(threadName);
                TradeOrder tradeOrder = new TradeOrder(tradeId, tradeId % 2 == 0 ? "已支付" : "未支付");
                TRADE_THREAD_LOCAL.set(tradeOrder);
                System.out.println("threadName: " + THREAD_NAME_LOCAL.get());
                System.out.println("tradeOrder info：" + TRADE_THREAD_LOCAL.get());
            }, threadName).start();
        }

    }

    static class TradeOrder {
        long id;
        String status;

        public TradeOrder(int id, String status) {
            this.id = id;
            this.status = status;
        }

        @Override
        public String toString() {
            return "id=" + id + ", status=" + status;
        }
    }
}
