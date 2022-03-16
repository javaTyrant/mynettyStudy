package io.netty.util.lutest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author lufengxiang
 * @since 2022/3/1
 **/
public class GenericsDemo {
    static class BillVo {
        private String orderCode;
        private String billCode;

        public String getBillCode() {
            return billCode;
        }

        public String getOrderCode() {
            return orderCode;
        }

        public BillVo(String orderCode, String billCode) {
            this.orderCode = orderCode;
            this.billCode = billCode;
        }
    }

    public static void main(String[] args) {
        BillVo billVo = new BillVo("a", "a1");
        BillVo billVo1 = new BillVo("a", "a2");
        BillVo billVo3 = new BillVo("b", "b2");
        BillVo billVo4 = new BillVo("b", "b2");
        List<BillVo> list = Arrays.asList(billVo, billVo1, billVo3, billVo4);
        //收集器.
        Collector<String, ?, List<String>> toList = Collectors.toList();
        Collector<BillVo, ?, List<String>> mapping = Collectors.mapping(BillVo::getBillCode, toList);
        //
        Map<String, List<String>> map = list.stream()
                .collect(Collectors.groupingBy(BillVo::getOrderCode, mapping));
        BiConsumer<?, BillVo> accumulator = mapping.accumulator();
        Supplier<?> supplier = mapping.supplier();
        System.out.println(map);
        List<String> collect = list.stream().map(BillVo::getBillCode).collect(toList);
        System.out.println(collect);
        Integer sum = Stream.of(1, 2, 3, 4).reduce(0, Integer::sum);
    }
}
