package io.netty.example.echo.guava.myeventbus;

import com.google.common.eventbus.AllowConcurrentEvents;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * 订阅者
 *
 * @author lufengxiang
 * @since 2021/5/14
 **/
public class Subscriber {
    //
    static Subscriber create(EventBus bus, Object listener, Method method) {
        //判断是否线程安全.
        return isDeclaredThreadSafe(method)
                ? new Subscriber(bus, listener, method)
                : new Subscriber.SynchronizedSubscriber(bus, listener, method);
    }

    //属于哪个evetnbus
    private final EventBus bus;
    //对象
    final Object target;
    //方法
    private final Method method;
    //执行器
    private final Executor executor;

    private Subscriber(EventBus bus, Object target, Method method) {
        this.bus = bus;
        this.target = checkNotNull(target);
        this.method = method;
        method.setAccessible(true);

        this.executor = bus.executor();

    }


    final void dispatchEvent(final Object event) {
        executor.execute(
                () -> {
                    try {
                        invokeSubscriberMethod(event);
                    } catch (InvocationTargetException e) {
                        //异常.
                        bus.handleSubscriberException(e.getCause(), context(event));
                    }
                });
    }
    //方法是属于注册者的,所以由注册者触发.OOP.
    void invokeSubscriberMethod(Object event) throws InvocationTargetException {
        try {
            //方法:对象-参数.
            method.invoke(target, checkNotNull(event));
        } catch (IllegalArgumentException e) {
            throw new Error("Method rejected target/argument: " + event, e);
        } catch (IllegalAccessException e) {
            throw new Error("Method became inaccessible: " + event, e);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Error) {
                throw (Error) e.getCause();
            }
            throw e;
        }
    }

    private SubscriberExceptionContext context(Object event) {
        //封装event.属于那个eventbus.
        return new SubscriberExceptionContext(bus, event, target, method);
    }

    @Override
    public final int hashCode() {
        return (31 + method.hashCode()) * 31 + System.identityHashCode(target);
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof Subscriber) {
            Subscriber that = (Subscriber) obj;
            // Use == so that different equal instances will still receive events.
            // We only guard against the case that the same object is registered
            // multiple times
            return target == that.target && method.equals(that.method);
        }
        return false;
    }

    /**
     * Checks whether {@code method} is thread-safe, as indicated by the presence of the {@link
     * com.google.common.eventbus.AllowConcurrentEvents} annotation.
     */
    private static boolean isDeclaredThreadSafe(Method method) {
        //判断是否有AllowConcurrentEvents注解.
        return method.getAnnotation(AllowConcurrentEvents.class) != null;
    }

    //加锁的
    static final class SynchronizedSubscriber extends Subscriber {

        private SynchronizedSubscriber(EventBus bus, Object target, Method method) {
            super(bus, target, method);
        }

        @Override
        void invokeSubscriberMethod(Object event) throws InvocationTargetException {
            synchronized (this) {
                super.invokeSubscriberMethod(event);
            }
        }
    }
}