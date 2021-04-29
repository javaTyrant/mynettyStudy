package io.netty.example.echo.juc;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;

/**
 * @author lufengxiang
 * @since 2021/4/21
 **/
public class AllocDemo {
    public static void main(String[] args) {
        // 获取非池化ByteBuf分配器
        ByteBufAllocator alloc = new PooledByteBufAllocator();

        // 分配一个直接内存缓冲ByteBuf
        ByteBuf directBuf = alloc.buffer(128);

        // 将字节数组写入该ByeBuf
        directBuf.writeBytes("这是写到直接内存的字符串".getBytes());

        // 将字节缓冲重新读取出来
        byte[] bytes = new byte[directBuf.readableBytes()];
        directBuf.readBytes(bytes);
        System.out.println(new String(bytes));

        // 使用完之后,要释放掉
        directBuf.release();
    }
}
