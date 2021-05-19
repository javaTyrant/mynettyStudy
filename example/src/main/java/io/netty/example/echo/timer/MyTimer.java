package io.netty.example.echo.timer;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author lufengxiang
 * @since 2021/5/18
 **/
public interface MyTimer {

    MyTimeout newTimeOut(MyTimerTask task, long delay, TimeUnit unit);

    Set<MyTimeout> stop();
}
