package io.netty.buffer;

import io.netty.util.Recycler;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author lufengxiang
 * @since 2022/2/23
 **/
public class CycliMutiThread7 {
    private static final Recycler<CyclerA> CyclerRecyclerA = new Recycler<CyclerA>() {
        @Override
        protected CyclerA newObject(Handle<CyclerA> handle) {
            return new CyclerA(handle);
        }
    };

    static final class CyclerA {
        private String value;

        public void setValue(String value) {
            this.value = value;
        }

        private Recycler.Handle<CyclerA> handle;

        public CyclerA(Recycler.Handle<CyclerA> handle) {
            this.handle = handle;
        }

        public void recycle() {
            handle.recycle(this);
        }
    }

    private static final Recycler<CyclerB> CyclerRecyclerB = new Recycler<CyclerB>() {
        @Override
        protected CyclerB newObject(Handle<CyclerB> handle) {
            return new CyclerB(handle);
        }
    };

    static final class CyclerB {
        private String value;

        public void setValue(String value) {
            this.value = value;
        }

        private Recycler.Handle<CyclerB> handle;

        public CyclerB(Recycler.Handle<CyclerB> handle) {
            this.handle = handle;
        }

        public void recycle() {
            handle.recycle(this);
        }
    }

    private static final Recycler<CyclerC> CyclerRecyclerC = new Recycler<CyclerC>() {
        @Override
        protected CyclerC newObject(Handle<CyclerC> handle) {
            return new CyclerC(handle);
        }
    };

    static final class CyclerC {
        private String value;

        public void setValue(String value) {
            this.value = value;
        }

        private Recycler.Handle<CyclerC> handle;

        public CyclerC(Recycler.Handle<CyclerC> handle) {
            this.handle = handle;
        }

        public void recycle() {
            handle.recycle(this);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ConcurrentLinkedQueue<CyclerA> qAThread = new ConcurrentLinkedQueue();
        Thread t = Thread.currentThread();

        for (int i = 0; i < 901; ++i) {
            qAThread.add(CyclerRecyclerA.get());
        }

        Thread t1 = new Thread(() -> {
            Thread temp = t;
            CyclerA cyclerA = qAThread.poll();
            cyclerA.setValue("t1");
            cyclerA.recycle();
        });
        t1.start();
        t1.join();

        Thread t2 = new Thread(() -> {
            Thread temp = t;
            CyclerA cyclerA = qAThread.poll();
            cyclerA.setValue("t2");
            cyclerA.recycle();
        });


        t2.start();
        t2.join();


        System.out.println("over");//断点2
    }
}
