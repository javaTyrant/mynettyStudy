package io.netty.example.echo.design.chain;

/**
 * @author lufengxiang
 * @since 2021/5/31
 **/
public class EmailLogger extends Logger {

    public EmailLogger(int mask) {
        this.mask = mask;
    }

    protected void writeMessage(String msg) {
        System.out.println("Sending via email: " + msg);
    }
}
