## 什么是NioEventLoopGroup



## 类结构

<img src="C:\Users\lufengxiang\AppData\Roaming\Typora\typora-user-images\image-20210425152552042.png" alt="image-20210425152552042" style="zoom: 33%;" />

## new NioEventLoopGroup()时发生了啥?

1.调用构造器 一参构造器

```java
public NioEventLoopGroup(int nThreads) {
    //传入空执行器
    this(nThreads, (Executor) null);
```

2.二参构造器

```java
public NioEventLoopGroup(int nThreads, Executor executor) {
    //传入一个provider
    this(nThreads, executor, SelectorProvider.provider());
}
```

3.三餐构造器

```java
public NioEventLoopGroup(
        int nThreads, Executor executor, final SelectorProvider selectorProvider) {
    //传入一个默认的选择策略工厂
    this(nThreads, executor, selectorProvider, DefaultSelectStrategyFactory.INSTANCE);
}
```

4.调用四参构造器

```java
public NioEventLoopGroup(int nThreads, Executor executor, final SelectorProvider selectorProvider,
                         final SelectStrategyFactory selectStrategyFactory) {
    //开始调用父类构造器 额外传入一个拒绝执行策略
    super(nThreads, executor, selectorProvider, selectStrategyFactory, RejectedExecutionHandlers.reject());
}
```

**父类:MultithreadEventLoopGroup**

```java
protected MultithreadEventLoopGroup(int nThreads, Executor executor, Object... args) {
    //线程数量默认赋值.
    super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, executor, args);
}
```

**再调用父类:MultithreadEventExecutorGroup**

args就要写死判断了,netty的这个设计是为什么呢?方法参数不要过长吗.

```java
protected MultithreadEventExecutorGroup(int nThreads, Executor executor, Object... args) {
	//这个就是核心的构造器了.
    this(nThreads, executor, DefaultEventExecutorChooserFactory.INSTANCE, args);
}
```

**newChild实现**

```java
  @Override
    protected EventLoop newChild(Executor executor, Object... args) throws Exception {
        EventLoopTaskQueueFactory queueFactory = args.length == 4 ? (EventLoopTaskQueueFactory) args[3] : null;
        return new NioEventLoop(this, executor, (SelectorProvider) args[0],
            ((SelectStrategyFactory) args[1]).newSelectStrategy(), (RejectedExecutionHandler) args[2], queueFactory);
    }
```

```java
NioEventLoop(NioEventLoopGroup parent, 
             Executor executor, 
             SelectorProvider selectorProvider,
             SelectStrategy strategy, 
             RejectedExecutionHandler rejectedExecutionHandler,
             EventLoopTaskQueueFactory queueFactory) {
    super(parent, executor, false, newTaskQueue(queueFactory),newTaskQueue(queueFactory),
            rejectedExecutionHandler);
    this.provider = ObjectUtil.checkNotNull(selectorProvider, "selectorProvider");
    this.selectStrategy = ObjectUtil.checkNotNull(strategy, "selectStrategy");
    final SelectorTuple selectorTuple = openSelector();
    this.selector = selectorTuple.selector;
    this.unwrappedSelector = selectorTuple.unwrappedSelector;
}
```

**继续调用父构造器**

```java
protected SingleThreadEventLoop(EventLoopGroup parent, 
                                Executor executor,
                                boolean addTaskWakesUp, 
                                Queue<Runnable> taskQueue, 
                                Queue<Runnable> tailTaskQueue,
                                RejectedExecutionHandler rejectedExecutionHandler) {
    super(parent, executor, addTaskWakesUp, taskQueue, rejectedExecutionHandler);
    tailTasks = ObjectUtil.checkNotNull(tailTaskQueue, "tailTaskQueue");
}
```

```java
protected SingleThreadEventExecutor(EventExecutorGroup parent, 
                                    Executor executor,
                                    boolean addTaskWakesUp, 
                                    Queue<Runnable> taskQueue,
                                    RejectedExecutionHandler rejectedHandler) {
    super(parent);
    this.addTaskWakesUp = addTaskWakesUp;
    this.maxPendingTasks = DEFAULT_MAX_PENDING_EXECUTOR_TASKS;
    this.executor = ThreadExecutorMap.apply(executor, this);
    this.taskQueue = ObjectUtil.checkNotNull(taskQueue, "taskQueue");
    this.rejectedExecutionHandler = ObjectUtil.checkNotNull(rejectedHandler, "rejectedHandler");
}
```

```java
protected AbstractScheduledEventExecutor(EventExecutorGroup parent) {
    super(parent);
}
```

```java
protected AbstractEventExecutor(EventExecutorGroup parent) {
    this.parent = parent;
}
```



## 连接事件

bossgroup处理连接,

分配给workergroup.

在哪里监听连接的?

监听连接后怎么处理的?

来看源码!



## 读写事件



### Netty优雅解决线程并发问题的方式.

![img](https://img-blog.csdnimg.cn/20190613201957781.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM3OTA5NTA4,size_16,color_FFFFFF,t_70)



## netty中所有的io操作都是异步的.



## netty处理定时任务的实现



## Netty中如何针对写限流



## 业务线程池是如何将数据通过IO线程写入网络中的呢？



## ByteBuf

Netty 的数据处理API 通过两个组件暴露——abstract class ByteBuf 和interface
ByteBufHolder。
下面是一些ByteBuf API 的优点：
它可以被用户自定义的缓冲区类型扩展；
通过内置的复合缓冲区类型实现了透明的零拷贝；
容量可以按需增长（类似于JDK 的StringBuilder）；
在读和写这两种模式之间切换不需要调用ByteBuffer 的flip()方法；
读和写使用了不同的索引；
支持方法的链式调用；
支持引用计数；
支持池化。