package io.netty.example.echo.design.chain;

/**
 * @author lufengxiang
 * @since 2021/5/31
 **/
public class StderrLogger extends Logger {
    public StderrLogger(int mask) {
        this.mask = mask;
    }

    protected void writeMessage(String msg) {
        System.out.println("Sending to stderr: " + msg);
    }
}
