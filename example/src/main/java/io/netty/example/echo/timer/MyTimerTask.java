package io.netty.example.echo.timer;

/**
 * @author lufengxiang
 * @since 2021/5/18
 **/
public interface MyTimerTask {
    
    void run(MyTimeout timeout) throws Exception;
}
