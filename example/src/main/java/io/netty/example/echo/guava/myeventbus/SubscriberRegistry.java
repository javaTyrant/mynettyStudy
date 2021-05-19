package io.netty.example.echo.guava.myeventbus;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.*;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.google.common.base.Preconditions.checkArgument;
import static io.netty.example.echo.guava.myeventbus.Preconditions.checkNotNull;

/**
 * @author lufengxiang
 * @since 2021/5/14
 **/
public class SubscriberRegistry {
    private final ConcurrentMap<Class<?>, CopyOnWriteArraySet<Subscriber>> subscribers =
            new ConcurrentHashMap<>();
    //  @Weak
    private final EventBus bus;

    SubscriberRegistry(EventBus bus) {
        this.bus = checkNotNull(bus);
    }


    //注册:核心方法.把订阅者放入eventSubscribers.
    void register(Object listener) {
        //找到对象所有被注解标记的方法.
        Multimap<Class<?>, Subscriber> listenerMethods = findAllSubscribers(listener);
        //Class Subscriber映射.如何遍历Multimap.key,vaule
        for (Map.Entry<Class<?>, Collection<Subscriber>> entry : listenerMethods.asMap().entrySet()) {
            Class<?> eventType = entry.getKey();
            //
            Collection<Subscriber> eventMethodsInListener = entry.getValue();
            //从缓存取.是一个set.
            CopyOnWriteArraySet<Subscriber> eventSubscribers = subscribers.get(eventType);
            //如果空
            if (eventSubscribers == null) {
                //线程安全.因为subscribers是多线程使用的.
                CopyOnWriteArraySet<Subscriber> newSet = new CopyOnWriteArraySet<>();
                //返回不会空的那个.
                eventSubscribers =
                        MoreObjects.firstNonNull(subscribers.putIfAbsent(eventType, newSet), newSet);
            }
            //加入到缓存里.
            eventSubscribers.addAll(eventMethodsInListener);
        }
    }

    //注销操作.
    void unregister(Object listener) {
        //
        Multimap<Class<?>, Subscriber> listenerMethods = findAllSubscribers(listener);

        for (Map.Entry<Class<?>, Collection<Subscriber>> entry : listenerMethods.asMap().entrySet()) {
            Class<?> eventType = entry.getKey();
            Collection<Subscriber> listenerMethodsForType = entry.getValue();

            CopyOnWriteArraySet<Subscriber> currentSubscribers = subscribers.get(eventType);
            if (currentSubscribers == null || !currentSubscribers.removeAll(listenerMethodsForType)) {
                // if removeAll returns true, all we really know is that at least one subscriber was
                // removed... however, barring something very strange we can assume that if at least one
                // subscriber was removed, all subscribers on listener for that event type were... after
                // all, the definition of subscribers on a particular class is totally static
                throw new IllegalArgumentException(
                        "missing event subscriber for an annotated method. Is " + listener + " registered?");
            }

            // don't try to remove the set if it's empty; that can't be done safely without a lock
            // anyway, if the set is empty it'll just be wrapping an array of length 0
        }
    }

    //缓存:
    private static final LoadingCache<Class<?>, ImmutableList<Method>> subscriberMethodsCache =
            //用了guava的cache
            CacheBuilder.newBuilder()
                    .weakKeys()
                    .build(
                            new CacheLoader<Class<?>, ImmutableList<Method>>() {
                                @Override
                                public ImmutableList<Method> load(Class<?> concreteClass) throws Exception {
                                    //
                                    return getAnnotatedMethodsNotCached(concreteClass);
                                }
                            });

    //找到所有的注册者/
    private Multimap<Class<?>, Subscriber> findAllSubscribers(Object listener) {
        //创建一个map,一个key可以对象多个value.
        Multimap<Class<?>, Subscriber> methodsInListener = HashMultimap.create();
        //获取对象的类文件
        Class<?> clazz = listener.getClass();
        //获取对象所有被标记的方法.
        for (Method method : getAnnotatedMethods(clazz)) {
            //
            Class<?>[] parameterTypes = method.getParameterTypes();
            //第一个参数就是类型.b
            Class<?> eventType = parameterTypes[0];
            //create操作.bus listener method.
            methodsInListener.put(eventType, Subscriber.create(bus, listener, method));
        }
        return methodsInListener;
    }

    Iterator<Subscriber> getSubscribers(Object event) {
        ImmutableSet<Class<?>> eventTypes = flattenHierarchy(event.getClass());

        List<Iterator<Subscriber>> subscriberIterators =
                Lists.newArrayListWithCapacity(eventTypes.size());

        for (Class<?> eventType : eventTypes) {
            CopyOnWriteArraySet<Subscriber> eventSubscribers = subscribers.get(eventType);
            if (eventSubscribers != null) {
                // eager no-copy snapshot
                subscriberIterators.add(eventSubscribers.iterator());
            }
        }

        return Iterators.concat(subscriberIterators.iterator());
    }

    private static final LoadingCache<Class<?>, ImmutableSet<Class<?>>> flattenHierarchyCache =
            CacheBuilder.newBuilder()
                    .weakKeys()
                    .build(
                            new CacheLoader<Class<?>, ImmutableSet<Class<?>>>() {
                                // <Class<?>> is actually needed to compile
                                @SuppressWarnings("RedundantTypeArguments")
                                @Override
                                public ImmutableSet<Class<?>> load(Class<?> concreteClass) {
                                    return ImmutableSet.<Class<?>>copyOf(
                                            TypeToken.of(concreteClass).getTypes().rawTypes());
                                }
                            });

    @VisibleForTesting
    static ImmutableSet<Class<?>> flattenHierarchy(Class<?> concreteClass) {
        try {
            return flattenHierarchyCache.getUnchecked(concreteClass);
        } catch (UncheckedExecutionException e) {
            throw Throwables.propagate(e.getCause());
        }
    }

    //
    private static ImmutableList<Method> getAnnotatedMethods(Class<?> clazz) {
        //先从缓存里获取.
        return subscriberMethodsCache.getUnchecked(clazz);
    }

    //
    private static ImmutableList<Method> getAnnotatedMethodsNotCached(Class<?> clazz) {
        //
        Set<? extends Class<?>> supertypes = TypeToken.of(clazz).getTypes().rawTypes();
        //
        Map<SubscriberRegistry.MethodIdentifier, Method> identifiers = Maps.newHashMap();
        //
        for (Class<?> supertype : supertypes) {
            //
            for (Method method : supertype.getDeclaredMethods()) {
                //
                if (method.isAnnotationPresent(Subscribe.class) && !method.isSynthetic()) {
                    // TODO(cgdecker): Should check for a generic parameter type and error out
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    //
                    checkArgument(
                            parameterTypes.length == 1,
                            "Method %s has @Subscribe annotation but has %s parameters."
                                    + "Subscriber methods must have exactly 1 parameter.",
                            method,
                            parameterTypes.length);
                    //封装一下
                    SubscriberRegistry.MethodIdentifier ident = new SubscriberRegistry.MethodIdentifier(method);
                    //没有在添加进去.
                    if (!identifiers.containsKey(ident)) {
                        identifiers.put(ident, method);
                    }
                }
            }
        }
        return ImmutableList.copyOf(identifiers.values());
    }


    Set<Subscriber> getSubscribersForTesting(Class<?> eventType) {
        return MoreObjects.firstNonNull(subscribers.get(eventType), ImmutableSet.<Subscriber>of());
    }

    //
    private static final class MethodIdentifier {
        //方法名
        private final String name;
        //方法参数
        private final List<Class<?>> parameterTypes;

        //name和参数
        MethodIdentifier(Method method) {
            this.name = method.getName();
            this.parameterTypes = Arrays.asList(method.getParameterTypes());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(name, parameterTypes);
        }

        @Override
        public boolean equals( Object o) {
            if (o instanceof SubscriberRegistry.MethodIdentifier) {
                SubscriberRegistry.MethodIdentifier ident = (SubscriberRegistry.MethodIdentifier) o;
                return name.equals(ident.name) && parameterTypes.equals(ident.parameterTypes);
            }
            return false;
        }
    }
}

