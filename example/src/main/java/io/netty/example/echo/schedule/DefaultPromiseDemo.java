package io.netty.example.echo.schedule;

import io.netty.util.concurrent.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author lufengxiang
 * @since 2021/4/16
 **/
public class DefaultPromiseDemo {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        EventExecutor executor = new DefaultEventExecutor(executorService);
        DefaultPromise<String> defaultPromise = new DefaultPromise<>(executor);
        //addListener的逻辑
        defaultPromise.addListener(future -> System.out.println("完成了操作" + future.get()));
        executorService.execute(() -> defaultPromise.setSuccess("what?"));
    }
}
