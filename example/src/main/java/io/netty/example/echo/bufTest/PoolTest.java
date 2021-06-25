package io.netty.example.echo.bufTest;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;

/**
 * @author lufengxiang
 * @since 2021/6/24
 **/
public class PoolTest {
    public static void main(String[] args) {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        ByteBuf buf = allocator.heapBuffer(9000);
        ByteBuf buf2 = allocator.heapBuffer(8192);
    }
}
