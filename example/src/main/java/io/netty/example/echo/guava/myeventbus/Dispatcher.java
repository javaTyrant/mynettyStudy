package io.netty.example.echo.guava.myeventbus;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static io.netty.example.echo.guava.myeventbus.Preconditions.checkNotNull;

/**
 * 这种设计可以好好学习下,虽然简单,但是要活学活用.
 * @author lufengxiang
 * @since 2021/5/14
 **/
public abstract class Dispatcher {
    static Dispatcher perThreadDispatchQueue() {
        return new PerThreadQueuedDispatcher();
    }

    //静态方法创建对象.
    static Dispatcher legacyAsync() {
        return new LegacyAsyncDispatcher();
    }

    static Dispatcher immediate() {
        return ImmediateDispatcher.INSTANCE;
    }

    //抽象的方法,看看对应的三种实现
    abstract void dispatch(Object event, Iterator<Subscriber> subscribers);

    //每个任务一个线程.
    private static final class PerThreadQueuedDispatcher extends Dispatcher {
        //
        private final ThreadLocal<Queue<Event>> queue =
                ThreadLocal.withInitial(ArrayDeque::new);
        //做什么用的呢?
        private final ThreadLocal<Boolean> dispatching =
                ThreadLocal.withInitial(() -> false);

        @Override
        void dispatch(Object event, Iterator<Subscriber> subscribers) {
            checkNotNull(event);
            checkNotNull(subscribers);
            //
            Queue<Event> queueForThread = queue.get();
            //
            queueForThread.offer(new Event(event, subscribers));

            if (!dispatching.get()) {
                //
                dispatching.set(true);
                try {
                    Event nextEvent;
                    //双重循环.
                    while ((nextEvent = queueForThread.poll()) != null) {
                        while (nextEvent.subscribers.hasNext()) {
                            nextEvent.subscribers.next().dispatchEvent(nextEvent.event);
                        }
                    }
                } finally {
                    dispatching.remove();
                    queue.remove();
                }
            }
        }

        private static final class Event {
            private final Object event;
            private final Iterator<Subscriber> subscribers;

            private Event(Object event, Iterator<Subscriber> subscribers) {
                this.event = event;
                this.subscribers = subscribers;
            }
        }
    }

    //
    private static final class LegacyAsyncDispatcher extends Dispatcher {
        /**
         * Global event queue.
         */
        private final ConcurrentLinkedQueue<EventWithSubscriber> queue =
                new ConcurrentLinkedQueue<>();

        @Override
        void dispatch(Object event, Iterator<Subscriber> subscribers) {
            checkNotNull(event);
            while (subscribers.hasNext()) {
                //加入到队列里.fifo.
                queue.add(new EventWithSubscriber(event, subscribers.next()));
            }
            //
            EventWithSubscriber e;
            //先触发队列里的第一个.
            while ((e = queue.poll()) != null) {
                e.subscriber.dispatchEvent(e.event);
            }
        }

        private static final class EventWithSubscriber {
            private final Object event;
            private final Subscriber subscriber;

            private EventWithSubscriber(Object event, Subscriber subscriber) {
                this.event = event;
                this.subscriber = subscriber;
            }
        }
    }

    //立马分发.
    private static final class ImmediateDispatcher extends Dispatcher {
        private static final ImmediateDispatcher INSTANCE = new ImmediateDispatcher();

        @Override
        void dispatch(Object event, Iterator<Subscriber> subscribers) {
            //参数校验
            checkNotNull(event);
            //如有还有下一个
            while (subscribers.hasNext()) {
                //获取下一个,触发.
                subscribers.next().dispatchEvent(event);
            }
        }
    }

}
