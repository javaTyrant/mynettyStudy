package io.netty.example.echo.guava.myeventbus;

/**
 * @author lufengxiang
 * @since 2021/5/14
 **/
public interface SubscriberExceptionHandler {
    void handleException(Throwable exception, SubscriberExceptionContext context);
}
