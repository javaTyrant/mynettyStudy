package io.netty.example.echo.guava.myeventbus;

import com.google.common.base.MoreObjects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author lufengxiang
 * @since 2021/5/14
 **/
public class DeadEvent {
    private final Object source;
    private final Object event;

    public DeadEvent(Object source, Object event) {
        this.source = checkNotNull(source);
        this.event = checkNotNull(event);
    }

    public Object getSource() {
        return source;
    }

    public Object getEvent() {
        return event;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("source", source).add("event", event).toString();
    }
}
