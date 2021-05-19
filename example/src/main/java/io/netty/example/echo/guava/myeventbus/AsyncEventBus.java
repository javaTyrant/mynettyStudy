package io.netty.example.echo.guava.myeventbus;


import java.util.concurrent.Executor;

/**
 * @author lufengxiang
 * @since 2021/5/14
 **/
public class AsyncEventBus extends EventBus {
    //复用父类的构造器,根据需求传参.
    public AsyncEventBus(String identifier, Executor executor) {
        super(identifier, executor, Dispatcher.legacyAsync(), LoggingHandler.INSTANCE);
    }

    public AsyncEventBus(Executor executor, SubscriberExceptionHandler subscriberExceptionHandler) {
        super("default", executor, Dispatcher.legacyAsync(), subscriberExceptionHandler);
    }

    public AsyncEventBus(Executor executor) {
        super("default", executor, Dispatcher.legacyAsync(), LoggingHandler.INSTANCE);
    }
}
