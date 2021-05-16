package io.netty.example.echo.guava.myeventbus;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * @author lumac
 * @since 2021/5/16
 */
public class Demo {
    public static void main(String[] args) {
        //熟悉.
        Multimap<Integer, String> map = HashMultimap.create();
        map.put(1, "lu");
        map.put(1, "jack");
        System.out.println(map.asMap().entrySet());
        EventBus bus = new EventBus();
        bus.register(new WaitToDo());
        bus.post(new Event("lu"));
    }

    static class Event {
        String name;

        public Event(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    static class WaitToDo {
        @Subscribe
        public void doSome(Event event) {
            System.out.println(event.getName() + "say:hello");
        }

        @Subscribe
        public void doSome2(Event event) {
            System.out.println(event.getName() + "say:hello2");
        }
    }
}
