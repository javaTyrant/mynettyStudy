package io.netty.buffer;

import org.junit.Test;

/**
 * @author lufengxiang
 * @since 2022/2/22
 **/
public class MyByteBufTest {
    @Test
    public void test() {
        //normal
        ByteBuf buffer = ByteBufAllocator.DEFAULT.heapBuffer(10 * 1024 * 1024);
        ByteBuf buffer1 = ByteBufAllocator.DEFAULT.heapBuffer(10 * 1024 * 1024);
    }

    //write 系列方法会改变 writerIndex 位置，当 writerIndex 等于 capacity 的时候，Buffer 置为不可写状态；
    //向不可写 Buffer 写入数据时，Buffer 会尝试扩容，但是扩容后 capacity 最大不能超过 maxCapacity，如果写入的数据超过 maxCapacity，程序会直接抛出异常；
    //read 系列方法会改变 readerIndex 位置，get/set 系列方法不会改变 readerIndex/writerIndex 位置。
    @Test
    public void simpleTest() {
        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(6, 10);
        //
        printByteBufInfo("ByteBufAllocator.buffer(5, 10)", buffer);

        buffer.writeBytes(new byte[]{1, 2});

        printByteBufInfo("write 2 Bytes", buffer);

        buffer.writeInt(100);

        printByteBufInfo("write Int 100", buffer);

        buffer.writeBytes(new byte[]{3, 4, 5});

        printByteBufInfo("write 3 Bytes", buffer);

        byte[] read = new byte[buffer.readableBytes()];

        buffer.readBytes(read);

        printByteBufInfo("readBytes(" + buffer.readableBytes() + ")", buffer);

        printByteBufInfo("BeforeGetAndSet", buffer);

        System.out.println("getInt(2): " + buffer.getInt(2));

        buffer.setByte(1, 0);

        System.out.println("getByte(1): " + buffer.getByte(1));

        printByteBufInfo("AfterGetAndSet", buffer);
    }

    private static void printByteBufInfo(String step, ByteBuf buffer) {

        System.out.println("------" + step + "-----");

        System.out.println("readerIndex(): " + buffer.readerIndex());

        System.out.println("writerIndex(): " + buffer.writerIndex());

        System.out.println("isReadable(): " + buffer.isReadable());

        System.out.println("isWritable(): " + buffer.isWritable());

        System.out.println("readableBytes(): " + buffer.readableBytes());

        System.out.println("writableBytes(): " + buffer.writableBytes());

        System.out.println("maxWritableBytes(): " + buffer.maxWritableBytes());

        System.out.println("capacity(): " + buffer.capacity());

        System.out.println("maxCapacity(): " + buffer.maxCapacity());
    }
}
