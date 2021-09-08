package io.netty.example.echo.juc;

/**
 * @author lufengxiang
 * @since 2021/4/29
 **/
public interface MyRejectedExecutionHandler {
    void rejectedExecution(Runnable r, MyThreadPoolExecutor executor);
}
