package io.netty.example.echo.juc;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 手抄线程池代码
 *
 * @author lufengxiang
 * @since 2021/4/22
 **/
@SuppressWarnings("unused")
public class MyThreadPoolExecutor extends AbstractExecutorService {
    //总结:线程池关闭的逻辑.
    //线程的idle状态:无锁的就是idle状态
    public static void main(String[] args) {
        //右移.1的二进制00000000000000000000000000000001
        System.out.println(Integer.toBinaryString(1 << COUNT_BITS));
        System.out.println(Integer.toBinaryString(1 << COUNT_BITS).length());
        System.out.println(Integer.toBinaryString((1 << COUNT_BITS) - 1));
    }
    //线程池中的位运算.高 3 位表示 线程池运行状态 低 29 位表示 线程有效数量

    //COUNT_BITS 表示用多少二进制位表示 线程数量 29
    private static final int COUNT_BITS = Integer.SIZE - 3;
    //核心变量
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
    //容量
    private static final int CAPACITY = (1 << COUNT_BITS) - 1;

    // runState is stored in the high-order bits
    private static final int RUNNING = -1 << COUNT_BITS;
    private static final int SHUTDOWN = 0 << COUNT_BITS;
    private static final int STOP = 1 << COUNT_BITS;
    private static final int TIDYING = 2 << COUNT_BITS;
    private static final int TERMINATED = 3 << COUNT_BITS;

    //核心配置参数
    private final BlockingQueue<Runnable> workQueue;
    //锁
    private final ReentrantLock mainLock = new ReentrantLock();
    //
    private final HashSet<Worker> workers = new HashSet<>();
    //哪些操作会获取锁才能执行.
    private final Condition termination = mainLock.newCondition();
    //最大线程数量.
    private int largestPoolSize;
    private long completedTaskCount;
    private volatile ThreadFactory threadFactory;
    private volatile MyRejectedExecutionHandler handler;
    private volatile long keepAliveTime;
    private volatile boolean allowCoreThreadTimeOut;
    private volatile int corePoolSize;
    private volatile int maximumPoolSize;
    private static final MyRejectedExecutionHandler defaultHandler = new AbortPolicy();
    //
    private final AccessControlContext acc;
    //
    private static final RuntimePermission shutdownPerm = new RuntimePermission("modifyThread");
    //核心构造器

    //几个核心的位操作技巧
    private static int runStateOf(int c) {
        return c & ~CAPACITY;
    }

    //
    private static int workerCountOf(int c) {
        return c & CAPACITY;
    }

    //
    private static int ctlOf(int rs, int wc) {
        return rs | wc;
    }

    //
    private static boolean runStateLessThan(int c, int s) {
        return c < s;
    }

    //
    private static boolean runStateAtLeast(int c, int s) {
        return c >= s;
    }

    //
    private static boolean isRunning(int c) {
        return c < SHUTDOWN;
    }

    //
    private static final boolean ONLY_ONE = true;

    //两个默认值
    public MyThreadPoolExecutor(int corePoolSize,
                                int maximumPoolSize,
                                long keepAliveTime,
                                TimeUnit unit,
                                BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                Executors.defaultThreadFactory(), defaultHandler);
    }

    //一个默认值
    public MyThreadPoolExecutor(int corePoolSize,
                                int maximumPoolSize,
                                long keepAliveTime,
                                TimeUnit unit,
                                BlockingQueue<Runnable> workQueue,
                                ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                threadFactory, defaultHandler);
    }

    //一个默认值
    public MyThreadPoolExecutor(int corePoolSize,
                                int maximumPoolSize,
                                long keepAliveTime,
                                TimeUnit unit,
                                BlockingQueue<Runnable> workQueue,
                                MyRejectedExecutionHandler handler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                Executors.defaultThreadFactory(), handler);
    }

    //核心构造器
    public MyThreadPoolExecutor(int corePoolSize,
                                int maximumPoolSize,
                                long keepAliveTime,
                                TimeUnit unit,
                                BlockingQueue<Runnable> workQueue,
                                ThreadFactory threadFactory,
                                MyRejectedExecutionHandler handler) {
        if (corePoolSize < 0 ||
                maximumPoolSize <= 0 ||
                maximumPoolSize < corePoolSize ||
                keepAliveTime < 0) {
            throw new IllegalArgumentException();
        }
        if (workQueue == null || threadFactory == null || handler == null) {
            throw new NullPointerException();
        }
        this.acc = System.getSecurityManager() == null ? null : AccessController.getContext();
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.workQueue = workQueue;
        this.keepAliveTime = unit.toNanos(keepAliveTime);
        this.threadFactory = threadFactory;
        this.handler = handler;
    }

    public AtomicInteger getCtl() {
        return ctl;
    }

    //execute是如何被调用的.AbstractExecutorService.submit
    //   public <T> Future<T> submit(Callable<T> task) {
    //        task校验
    //        if (task == null) throw new NullPointerException();
    //        封装一下
    //        RunnableFuture<T> ftask = newTaskFor(task);
    //        执行.
    //        execute(ftask);
    //        return ftask;
    //    }
    @Override
    public void execute(Runnable command) {
        if (command == null) {
            throw new NullPointerException();
        }
        int c = ctl.get();
        //线程数量小于核心线程数量
        if (workerCountOf(c) < corePoolSize) {
            //添加worker
            if (addWorker(command, true)) {
                return;
            }
            c = ctl.get();
        }
        if (isRunning(c) && workQueue.offer(command)) {
            int recheck = ctl.get();
            if (!isRunning(recheck) && remove(command)) {
                reject(command);
            } else if (workerCountOf(recheck) == 0) {
                addWorker(null, false);
            }
        } else if (!addWorker(command, false)) {
            reject(command);
        }
    }

    public void reject(Runnable task) {
        handler.rejectedExecution(task, this);
    }

    public BlockingQueue<Runnable> getQueue() {
        return workQueue;
    }

    public boolean remove(Runnable task) {
        boolean removed = workQueue.remove(task);
        tryTerminate();
        return removed;
    }

    //
    private boolean addWorker(Runnable firstTask, boolean core) {
        retry:
        //死循环.
        for (; ; ) {
            //先获取目前的线程数量
            int c = ctl.get();
            //或者状态
            int rs = runStateOf(c);
            //校验:rs < shutdown 或者rs != shutdown 或者firstTask != null 或者 workQueue.isEmpty
            if (rs >= SHUTDOWN && !(rs == SHUTDOWN && firstTask == null && !workQueue.isEmpty())) {
                return false;
            }
            //
            for (; ; ) {
                //再次获取线程数量.别的线程可能会改变这个值
                int wc = workerCountOf(c);
                //校验:如果此时wc是4,别的线程减1了呢.
                if (wc >= CAPACITY || wc >= (core ? corePoolSize : maximumPoolSize))
                    return false;
                //cas增加线程的数量
                if (compareAndIncrementWorkerCount(c)) {
                    break retry;
                }
                //cas失败
                c = ctl.get();
                //如果两边的状态不一致,说明其他线程有改动过.调到最外层的循环.需要再执行一次状态校验
                //如果状态一直只要在内层循环里执行.
                if (runStateOf(c) != rs) {
                    continue retry;
                }
            }
        }
        //此时只是线程数量增加了,实际的线程并没有添加.
        boolean workerStarted = false;
        boolean workerAdded = false;
        Worker w = null;
        try {
            //创建新的worker.里面会new线程.
            w = new Worker(firstTask);
            //
            final Thread t = w.thread;
            if (t != null) {
                //加锁
                final ReentrantLock mainLock = this.mainLock;
                mainLock.lock();
                try {
                    //获取状态
                    int rs = runStateOf(ctl.get());
                    //状态校验
                    if (rs < SHUTDOWN || (rs == SHUTDOWN && firstTask == null)) {
                        if (t.isAlive()) {
                            throw new IllegalThreadStateException();
                        }
                        //添加worker
                        workers.add(w);
                        int s = workers.size();
                        if (s > largestPoolSize) {
                            largestPoolSize = s;
                        }
                        workerAdded = true;
                    }
                } finally {
                    mainLock.unlock();
                }
                if (workerAdded) {
                    //启动线程.调用worker的run方法.
                    t.start();
                    workerStarted = true;
                }
            }
        } finally {
            if (!workerStarted) {
                //失败了怎么处理
                addWorkerFailed(w);
            }
        }
        return workerStarted;
    }

    //什么时候会添加线程失败呢?
    private void addWorkerFailed(Worker w) {
        //加锁
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            if (w != null) {
                //worker移除
                workers.remove(w);
                //减少线程数量
                decrementWorkerCount();
                //尝试终止
                tryTerminate();
            }
        } finally {
            mainLock.unlock();
        }
    }

    //调用链
    final void runWorker(Worker w) {
        //获取当前线程
        Thread wt = Thread.currentThread();
        //获取第一个任务
        Runnable task = w.firstTask;
        //第一个任务置空
        w.firstTask = null;
        //线程解锁why
        w.unlock();
        boolean completedAbruptly = true;
        try {
            //getTask,如果getTask为null.那么就退出while循环.
            while (task != null || (task = getTask()) != null) {
                //加锁
                w.lock();
                //状态校验
                if ((runStateAtLeast(ctl.get(), STOP)
                        || (Thread.interrupted() &&
                        runStateAtLeast(ctl.get(), STOP))) && !wt.isInterrupted()) {
                    wt.interrupt();
                }
                try {
                    beforeExecute(wt, task);
                    Throwable thrown = null;
                    try {
                        //执行任务
                        task.run();
                        //优化jdk的写法
                    } catch (RuntimeException | Error x) {
                        thrown = x;
                        throw x;
                    } catch (Throwable x) {
                        thrown = x;
                        throw new Error(x);
                    } finally {
                        afterExecute(task, thrown);
                    }
                } finally {
                    task = null;
                    //是线程安全的吗?被lock保护.
                    w.completedTasks++;
                    w.unlock();
                }
            }
        } finally {
            //这里面处理空闲的线程.
            processWorkerExit(w, completedAbruptly);
        }
    }


    protected void afterExecute(Runnable task, Throwable thrown) {
    }

    protected void beforeExecute(Thread wt, Runnable task) {
    }

    protected void terminated() {
    }

    //
    private void processWorkerExit(Worker w, boolean completedAbruptly) {
        if (completedAbruptly) {
            decrementWorkerCount();
        }
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            completedTaskCount += w.completedTasks;
            workers.remove(w);
        } finally {
            mainLock.unlock();
        }
        //只打断一个线程.
        tryTerminate();
        int c = ctl.get();
        if (runStateLessThan(c, STOP)) {
            if (!completedAbruptly) {
                int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
                if (min == 0 && !workQueue.isEmpty()) {
                    min = 1;
                }
                if (workerCountOf(c) >= min) {
                    return;
                }
                //
                addWorker(null, false);
            }
        }
    }

    public void tryTerminate() {
        for (; ; ) {
            int c = ctl.get();
            if (isRunning(c) || runStateAtLeast(c, TIDYING) ||
                    (runStateOf(c) == SHUTDOWN && !workQueue.isEmpty())) {
                return;
            }
            //如果工作线程不等于0,打断一个线程why?一个一个清理.
            if (workerCountOf(c) != 0) {
                interruptIdleWorkers(ONLY_ONE);
            }
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
                    try {
                        terminated();
                    } finally {
                        //
                        ctl.set(ctlOf(TERMINATED, 0));
                        //这个是什么操作呢?
                        termination.signalAll();
                    }
                }
                return;
            } finally {
                mainLock.unlock();
            }
        }
    }


    //死循环cas
    private void decrementWorkerCount() {
        do {
        } while (!compareAndDecrementWorkerCount(ctl.get()));
    }

    private Runnable getTask() {
        boolean timedOut = false;
        //死循环get
        for (; ; ) {
            int c = ctl.get();
            int rs = runStateOf(c);
            //
            if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
                decrementWorkerCount();
                return null;
            }
            int wc = workerCountOf(c);
            //如果允许核心线程超时或者线程数大于核心线程数.
            boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;
            //如果当前线程数量大于最大线程数量,或者timed且timedOut
            if ((wc > maximumPoolSize || (timed && timedOut))) {
                //减少线程的数量.
                if (compareAndIncrementWorkerCount(c)) {
                    //如果cas成功.那么就返回null.
                    return null;
                }
                continue;
            }
            try {
                //workQueue.poll说明keepAliveTime内没有获取到任务,也就是线程闲置了keepAliveTime
                Runnable r = timed ? workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
                        workQueue.take();
                if (r != null) {
                    return r;
                }
                //没有取到任务.
                timedOut = true;
            } catch (InterruptedException retry) {
                timedOut = false;
            }
        }
    }

    //队列的大小+正在执行任务的线程数量.
    public long getTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = completedTaskCount;
            for (Worker w : workers) {
                n += w.completedTasks;
                if (w.isLocked()) {
                    ++n;
                }
            }
            return n + workQueue.size();
        } finally {
            mainLock.unlock();
        }
    }

    private boolean compareAndIncrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect + 1);
    }

    private boolean compareAndDecrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect - 1);
    }

    @Override
    public void shutdown() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess();
            advanceRunState(SHUTDOWN);
            interruptIdleWorkers();
            onShutdown();
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
    }

    private void interruptIdleWorkers() {
        interruptIdleWorkers(false);
    }

    private void interruptWorkers() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : workers) {
                w.interruptIfStarted();
            }
        } finally {
            mainLock.unlock();
        }
    }

    //打断空闲的线程
    private void interruptIdleWorkers(boolean onlyOne) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : workers) {
                Thread t = w.thread;
                //这里是会把所有的空闲都打断的,那么如何保证核心线程不被打断呢?调用的地方肯定会判断.如果线程池关闭
                //那么,肯定是都要打断的.
                //如果能获取到锁就说明是空闲的线程,否则这个线程就被占用了.
                if (!t.isInterrupted() && w.tryLock()) {//为什么worker要实现aqs就一目了然了.
                    try {
                        t.interrupt();
                    } catch (SecurityException ignore) {
                    } finally {
                        //worker解锁.
                        w.unlock();
                    }
                }
                //如果只打断一个线程,就break
                if (onlyOne) {
                    break;
                }
            }
        } finally {
            mainLock.unlock();
        }
    }

    void onShutdown() {
    }

    private void advanceRunState(int targetState) {
        for (; ; ) {
            int c = ctl.get();
            if (runStateAtLeast(c, targetState) || ctl.compareAndSet(c, ctlOf(targetState, workerCountOf(c)))) {
                break;
            }
        }
    }

    private void checkShutdownAccess() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(shutdownPerm);
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                //循环检查权限.
                for (Worker w : workers) {
                    security.checkAccess(w.thread);
                }
            } finally {
                mainLock.unlock();
            }
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        List<Runnable> tasks;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess();
            advanceRunState(STOP);
            interruptWorkers();
            tasks = drainQueue();
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
        return tasks;
    }

    private List<Runnable> drainQueue() {
        BlockingQueue<Runnable> q = workQueue;
        ArrayList<Runnable> taskList = new ArrayList<>();
        //Removes all available elements from this queue and adds them to the given collection
        q.drainTo(taskList);
        if (!q.isEmpty()) {
            for (Runnable r : q.toArray(new Runnable[0])) {
                if (q.remove(r)) {
                    taskList.add(r);
                }
            }
        }
        return taskList;
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    //
    final boolean isRunningOrShutDown(boolean shutdownOK) {
        int rs = runStateOf(ctl.get());
        return rs == RUNNING || (rs == SHUTDOWN && shutdownOK);
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    public boolean isTerminating() {
        int c = ctl.get();
        return !isRunning(c) && runStateLessThan(c, TERMINATED);
    }

    boolean isStopped() {
        return runStateAtLeast(ctl.get(), STOP);
    }

    final boolean isRunningOrShutdown(boolean shutdownOK) {
        int rs = runStateOf(ctl.get());
        return rs == RUNNING || (rs == SHUTDOWN && shutdownOK);
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (; ; ) {
                if (runStateAtLeast(ctl.get(), TERMINATED)) {
                    return true;
                }
                if (nanos <= 0) {
                    return false;
                }
                //这里等待signal
                nanos = termination.awaitNanos(nanos);
            }
        } finally {
            mainLock.unlock();
        }
    }

    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    public void setThreadFactory(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    public void setRejectedExecutionHandler(MyRejectedExecutionHandler handler) {
        if (handler == null) {
            throw new NullPointerException();
        }
        this.handler = handler;
    }

    //设置核心线程数:当前肯定有一个核心线程数
    public void setCorePoolSize(int corePoolSize) {
        if (corePoolSize < 0) {
            throw new IllegalArgumentException();
        }
        //差
        int delta = corePoolSize - this.corePoolSize;
        //
        this.corePoolSize = corePoolSize;
        if (workerCountOf(ctl.get()) > corePoolSize) {
            interruptIdleWorkers();
        } else if (delta > 0) {//如果传入的核心线程数大于线程的线程数.那么要知道加几个核心线程
            //取delta和wokQueue.size的最小值.确保workQueue的任务能被核心线程数处理
            int k = Math.min(delta, workQueue.size());
            while (k-- > 0 && addWorker(null, true)) {
                if (workQueue.isEmpty()) {
                    break;
                }
            }
        }
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    //获取线程池的线程数量
    public int getPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            return runStateAtLeast(ctl.get(), TIDYING) ? 0 : workers.size();
        } finally {
            mainLock.unlock();
        }
    }

    public boolean prestartCoreThread() {
        //预先启动核心线程:如果当前的线程数量小于核心线程.那么久addWorker.只启动一个.
        return workerCountOf(ctl.get()) < corePoolSize && addWorker(null, true);
    }

    void ensurePrestart() {
        //获取线程数量
        int wc = workerCountOf(ctl.get());
        //如果小于核心线程或者没有线程,添加一个线程.
        if (wc < corePoolSize) {
            addWorker(null, true);
        } else if (wc == 0) {
            addWorker(null, false);
        }
    }

    public int prestartAllCoreThreads() {
        int n = 0;
        while (addWorker(null, true)) {
            ++n;
        }
        return n;
    }

    public boolean allowsCoreThreadTimeOut() {
        return allowCoreThreadTimeOut;
    }

    public void allowCoreThreadTimeOut(boolean value) {
        if (value && keepAliveTime <= 0) {
            throw new IllegalArgumentException("core threads must have nonzero keep alive times");
        }
        if (value != allowCoreThreadTimeOut) {
            allowCoreThreadTimeOut = value;
            if (value) {
                interruptIdleWorkers();
            }
        }
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        if (maximumPoolSize <= 0 || maximumPoolSize < corePoolSize) {
            throw new IllegalArgumentException();
        }
        this.maximumPoolSize = maximumPoolSize;
        //如果线程数量大于最大线程数.什么时候会出现这种情况?
        if (workerCountOf(ctl.get()) > maximumPoolSize) {
            interruptIdleWorkers();
        }
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public int getLargestPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        try {
            return largestPoolSize;
        } finally {
            mainLock.unlock();
        }
    }

    public void setKeepAliveTime(long time, TimeUnit unit) {
        if (time < 0) {
            throw new IllegalArgumentException();
        }
        if (time == 0 && allowsCoreThreadTimeOut()) {
            throw new IllegalArgumentException("core threads must have nonzero keep alive times");
        }
        long keepAliveTime = unit.toNanos(time);
        long delta = keepAliveTime - this.keepAliveTime;
        if (delta < 0) {
            interruptIdleWorkers();
        }
    }

    public long getKeepAliveTime(TimeUnit unit) {
        return unit.convert(keepAliveTime, TimeUnit.NANOSECONDS);
    }

    public void purge() {
        final BlockingQueue<Runnable> q = workQueue;
        try {
            q.removeIf(r -> r instanceof Future<?> && ((Future<?>) r).isCancelled());
        } catch (ConcurrentModificationException fallThrough) {
            for (Object r : q.toArray()) {
                if (r instanceof Future<?> && ((Future<?>) r).isCancelled()) {
                    q.remove(r);
                }
            }
        }
        tryTerminate();
    }

    protected void finalize() {
        SecurityManager sm = System.getSecurityManager();
        if (sm == null || acc == null) {
            shutdown();
        } else {
            PrivilegedAction<Void> pa = () -> {
                shutdown();
                return null;
            };
            //了解下这个用法
            AccessController.doPrivileged(pa, acc);
        }
    }

    public static class AbortPolicy implements MyRejectedExecutionHandler {
        public AbortPolicy() {

        }

        @Override
        public void rejectedExecution(Runnable r, MyThreadPoolExecutor e) {
            throw new RejectedExecutionException("Task" + r.toString() + "reject from" + e.toString());
        }
    }

    public static class DiscardPolicy implements MyRejectedExecutionHandler {
        public DiscardPolicy() {

        }

        @Override
        public void rejectedExecution(Runnable r, MyThreadPoolExecutor executor) {

        }
    }

    public static class DiscardOldestPolicy implements MyRejectedExecutionHandler {
        public DiscardOldestPolicy() {
        }

        @Override
        public void rejectedExecution(Runnable r, MyThreadPoolExecutor executor) {
            if (!executor.isShutdown()) {
                //删除停留时间最早的
                executor.getQueue().poll();
                //执行当前的
                executor.execute(r);
            }
        }
    }

    public static class CallerRunsPolicy implements MyRejectedExecutionHandler {
        public CallerRunsPolicy() {
        }

        @Override
        public void rejectedExecution(Runnable r, MyThreadPoolExecutor executor) {
            if (!executor.isShutdown()) {
                //不用线程池的线程,用调用者线程执行
                r.run();
            }
        }
    }

    @Override
    public String toString() {
        long ncompleted;
        int nworkers, nactive;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            ncompleted = completedTaskCount;
            nactive = 0;
            nworkers = workers.size();
            for (Worker w : workers) {
                ncompleted += w.completedTasks;
                if (w.isLocked()) {
                    ++nactive;
                }
            }
            int c = ctl.get();
            //判断线程池的状态
            String rs = (runStateLessThan(c, SHUTDOWN) ? "Running" : (runStateAtLeast(c, TERMINATED) ? "terminated" : "Shutting down"));
            return super.toString() + "[" + rs +
                    ",pool size = " + nworkers +
                    ", active threads" + nactive +
                    ",queued tasks = " + workQueue.size() +
                    ", completed tasks = " + ncompleted +
                    "]";
        } finally {
            mainLock.unlock();
        }
    }

    //worker的结构:Worker也是Aqs.
    private final class Worker extends AbstractQueuedSynchronizer implements Runnable {
        //
        private static final long serialVersionUID = 6138294804551838833L;
        //执行线程
        final Thread thread;
        //第一个任务:什么时候赋值的?
        Runnable firstTask;
        //完成的数量
        volatile long completedTasks;

        //构造器赋值.addWorker的时候会加一个.
        Worker(Runnable firstTask) {
            setState(-1);
            this.firstTask = firstTask;
            //new 线程.可以自定义线程工厂.
            this.thread = getThreadFactory().newThread(this);
            System.out.println(this.thread.getName());
        }

        //让内部的线程执行run方法
        @Override
        public void run() {
            //调用runWorker.什么时候调用runworker呢?
            System.out.println("被执行了" + Thread.currentThread().getName());
            runWorker(this);
        }

        @Override
        protected boolean isHeldExclusively() {
            return getState() != 0;
        }

        @Override
        protected boolean tryAcquire(int arg) {
            //如果没有线程占用,设置独占线程
            if (compareAndSetState(0, 1)) {
                //
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        @Override
        protected boolean tryRelease(int arg) {
            //先释放独占内存
            setExclusiveOwnerThread(null);
            //修改状态
            setState(0);
            return true;
        }

        public void lock() {
            acquire(1);
        }

        public boolean tryLock() {
            return tryAcquire(1);
        }

        //解锁,release1.
        //1.runWorker的时候要先解锁
        //2.runWorker跑完一个任务要解锁
        //3.打断空闲线程结束后要解锁.
        public void unlock() {
            release(1);
        }

        public boolean isLocked() {
            return isHeldExclusively();
        }

        void interruptIfStarted() {
            Thread t;
            if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
                try {
                    t.interrupt();
                } catch (SecurityException ignore) {
                }
            }
        }
    }
}
