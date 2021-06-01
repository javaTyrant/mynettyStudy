package io.netty.example.echo.design.chain;

/**
 * @author lufengxiang
 * @since 2021/5/31
 **/
public class StdoutLogger extends Logger {
    public StdoutLogger(int mask) {
        this.mask = mask;
    }

    protected void writeMessage(String msg) {
        System.out.println("Writting to stdout: " + msg);
    }
}
