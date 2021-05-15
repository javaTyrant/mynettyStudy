package io.netty.example.echo.guava.myeventbus;

import java.util.concurrent.Executor;

/**
 * @author lumac
 * @since 2021/5/15
 */
public enum DirectExecutor implements Executor {
    INSTANCE;

    @Override
    public void execute(Runnable command) {
        command.run();
    }

    @Override
    public String toString() {
        return "direct executor";
    }
}
