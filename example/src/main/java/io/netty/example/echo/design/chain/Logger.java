package io.netty.example.echo.design.chain;

/**
 * @author lufengxiang
 * @since 2021/5/31
 **/
public abstract class Logger {
    //日志级别
    public static int ERR = 3;
    public static int NOTICE = 5;
    public static int DEBUG = 7;
    //
    protected int mask;
    //下一个
    protected Logger next;

    public Logger setNext(Logger l) {
        next = l;
        return this;
    }

    public final void message(String msg, int priority) {
        if (priority <= mask) {
            writeMessage(msg);
            if (next != null) {
                next.message(msg, priority);
            }
        }
    }

    protected abstract void writeMessage(String msg);
}
