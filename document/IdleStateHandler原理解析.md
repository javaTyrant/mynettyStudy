## 如何优雅的管理超时.

IdleStateHandler心跳检测主要是通过向线程任务队列中添加**定时任务**，判断channelRead()方法或write()方法是否调用空闲超时，如果超时则触发超时事件执行自定义userEventTrigger()方法；

Netty通过IdleStateHandler实现最常见的心跳机制**不是一种双向心跳**的PING-PONG模式，而是客户端发送心跳数据包，服务端接收心跳但不回复，因为如果服务端同时有上千个连接，**心跳的回复需要消耗大量网络资源**；如果服务端一段时间内内有收到客户端的心跳数据包则**认为客户端已经下线**，将通道关闭避免资源的浪费；在这种心跳模式下服务端可以感知客户端的存活情况，无论是宕机的正常下线还是网络问题的非正常下线，服务端都能感知到，而客户端不能感知到服务端的非正常下线；

要想实现客户端感知服务端的存活情况，需要进行双向的心跳；Netty中的channelInactive()方法是通过Socket连接关闭时挥手数据包触发的，因此可以通过channelInactive()方法感知正常的下线情况，但是因为网络异常等非正常下线则无法感知；

IdleStateHandler的调用链

1.channelActive 初始化 把任务放入队列中

2.doStartThread.SingleThreadEventExecutor.this.run();

3.SingleThreadEventExecutor.runAllTasks(long)

4.AbstractEventExecutor.safeExecute

5.io.netty.util.concurrent.PromiseTask.runTask

6.PromiseTask.runTask

7.PromiseTask.((Runnable) task).run();

8.AbstractIdleTask.run(ctx);

9.IdleStateHandler.ReaderIdleTimeoutTask.run

## 细节:

