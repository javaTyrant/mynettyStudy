package io.netty.example.echo.guava.myeventbus;

import java.lang.reflect.Method;

import static io.netty.example.echo.guava.myeventbus.Preconditions.checkNotNull;

/**
 * @author lufengxiang
 * @since 2021/5/14
 **/
public class SubscriberExceptionContext {
    private final EventBus eventBus;
    private final Object event;
    private final Object subscriber;
    private final Method subscriberMethod;

    SubscriberExceptionContext(
            EventBus eventBus, Object event, Object subscriber, Method subscriberMethod) {
        this.eventBus = checkNotNull(eventBus);
        this.event = checkNotNull(event);
        this.subscriber = checkNotNull(subscriber);
        this.subscriberMethod = checkNotNull(subscriberMethod);
    }


    public Object getEvent() {
        return event;
    }

    public EventBus getEventBus() {
        return eventBus;
    }


    public Object getSubscriber() {
        return subscriber;
    }

    public Method getSubscriberMethod() {
        return subscriberMethod;
    }
}
