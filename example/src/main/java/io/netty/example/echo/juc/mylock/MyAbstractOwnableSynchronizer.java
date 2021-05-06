package io.netty.example.echo.juc.mylock;

import java.io.Serializable;

/**
 * @author lufengxiang
 * @since 2021/5/6
 **/
public class MyAbstractOwnableSynchronizer implements Serializable {
    private static final long serialVersionUID = 3737899427754241961L;

    protected MyAbstractOwnableSynchronizer() {

    }

    private transient Thread exclusiveOwnerThread;

    protected final void setExclusiveOwnerThread(Thread thread) {
        exclusiveOwnerThread = thread;
    }

    protected final Thread getExclusiveOwnerThread() {
        return exclusiveOwnerThread;
    }
}
