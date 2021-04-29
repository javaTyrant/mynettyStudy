## 从BlockingQueue的poll说起

```java
E poll(long timeout, TimeUnit unit)
    throws InterruptedException;
```

ArrayBlockingQueue的实现.

```java
public E poll(long timeout, TimeUnit unit) throws InterruptedException {
    //计算等待的时间
    long nanos = unit.toNanos(timeout);
    //获取到锁
    final ReentrantLock lock = this.lock;
    //上锁
    lock.lockInterruptibly();
    try {
        //如果count == 0.Number of elements in the queue
        while (count == 0) {//队里没有值
            if (nanos <= 0)
                return null;
            //等待nanos
            nanos = notEmpty.awaitNanos(nanos);
        }
        //如果有元素直接dequeue.
        return dequeue();
    } finally {
        lock.unlock();
    }
}
```

出队列的方法

```java
//加锁了的,这个方法已经被保护了. 
private E dequeue() {
        // assert lock.getHoldCount() == 1;
        // assert items[takeIndex] != null;
    	//数组复制一下
        final Object[] items = this.items;
        @SuppressWarnings("unchecked")
    	//取takeIndex
        E x = (E) items[takeIndex];
    	//置空
        items[takeIndex] = null;
    	//判断是否越界.
        if (++takeIndex == items.length)
            //重置takeIndex
            takeIndex = 0;
    	//元素减1
        count--;
    	//修改itrs
        if (itrs != null)
            itrs.elementDequeued();
    	//发送未满的通知
        notFull.signal();
    	//返回队列元素.
        return x;
    }
```

等待方法原理.这个方法还是蛮复杂的.

```java
        public final long awaitNanos(long nanosTimeout)
                throws InterruptedException {
            //先判断线程有没有被打断
            if (Thread.interrupted())
                throw new InterruptedException();
            //添加等待的节点
            Node node = addConditionWaiter();
            //
            int savedState = fullyRelease(node);
            //deadline
            final long deadline = System.nanoTime() + nanosTimeout;
            //
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
                //netty里似曾相识.
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return deadline - System.nanoTime();
        }
```

