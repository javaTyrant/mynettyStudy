package io.netty.example.echo.guava.myeventbus;


import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.netty.example.echo.guava.myeventbus.Preconditions.checkNotNull;


/**
 * @author lufengxiang
 * @since 2021/5/14
 **/
@SuppressWarnings("unused")
public class EventBus {
    /*
      如何设计?初步的功能,我们要实现一个观察者模式,那么jdk是提供了观察者模式的.
      jdk的弊端:复用性不强.
      核心的方法:register和post
     */
    private static final Logger logger = Logger.getLogger(EventBus.class.getName());

    private final String identifier;

    private final Executor executor;

    private final SubscriberExceptionHandler exceptionHandler;

    //负责注册.
    private final SubscriberRegistry subscribers = new SubscriberRegistry(this);
    //负责分发事件
    private final Dispatcher dispatcher;

    public EventBus() {
        this("default");
    }

    public EventBus(String identifier) {
        this(identifier,
                DirectExecutor.INSTANCE,
                Dispatcher.perThreadDispatchQueue(),
                LoggingHandler.INSTANCE);
    }

    public EventBus(
            String identifier,
            Executor executor,
            Dispatcher dispatcher,
            SubscriberExceptionHandler exceptionHandler) {
        this.identifier = checkNotNull(identifier);
        this.executor = checkNotNull(executor);
        this.dispatcher = checkNotNull(dispatcher);
        this.exceptionHandler = checkNotNull(exceptionHandler);

    }

    final Executor executor() {
        return executor;
    }

    public final String identifier() {
        return identifier;
    }

    public void register(Object object) {
        //委托给:SubscriberRegistry
        subscribers.register(object);
    }

    public void unregister(Object object) {
        subscribers.unregister(object);
    }

    public void post(Object event) {
        //迭代器模式.根据event获取订阅者.
        Iterator<Subscriber> eventSubscribers = subscribers.getSubscribers(event);
        //如果还有下一个.
        if (eventSubscribers.hasNext()) {
            dispatcher.dispatch(event, eventSubscribers);
            //什么情况走到这个分支呢?最后一个吗?判断是否是deadevent.
        } else if (!(event instanceof DeadEvent)) {
            // the event had no subscribers and was not itself a DeadEvent
            post(new DeadEvent(this, event));
        }
    }


    void handleSubscriberException(Throwable e, SubscriberExceptionContext context) {
        checkNotNull(e);
        checkNotNull(context);
        try {
            exceptionHandler.handleException(e, context);
        } catch (Throwable e2) {
            // if the handler threw an exception... well, just log it
            logger.log(
                    Level.SEVERE,
                    String.format(Locale.ROOT, "Exception %s thrown while handling exception: %s", e2, e),
                    e2);
        }
    }

    static final class LoggingHandler implements SubscriberExceptionHandler {
        static final LoggingHandler INSTANCE = new LoggingHandler();

        @Override
        public void handleException(Throwable exception, SubscriberExceptionContext context) {
            Logger logger = logger(context);
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, message(context), exception);
            }
        }

        private static Logger logger(SubscriberExceptionContext context) {
            return Logger.getLogger(EventBus.class.getName() + "." + context.getEventBus().identifier());
        }

        private static String message(SubscriberExceptionContext context) {
            Method method = context.getSubscriberMethod();
            return "Exception thrown by subscriber method "
                    + method.getName()
                    + '('
                    + method.getParameterTypes()[0].getName()
                    + ')'
                    + " on subscriber "
                    + context.getSubscriber()
                    + " when dispatching event: "
                    + context.getEvent();
        }
    }
}
