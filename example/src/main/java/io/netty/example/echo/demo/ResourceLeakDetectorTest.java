package io.netty.example.echo.demo;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetectorFactory;
import io.netty.util.ResourceLeakHint;
import io.netty.util.ResourceLeakTracker;

/**
 * @author lufengxiang
 * @since 2021/5/25
 **/
public class ResourceLeakDetectorTest {
    private static TrackableObject obj = new TrackableObject();

    public static void testStackTraceA() {
        StackTraceAccessor accessor = new StackTraceAccessor();
        accessor.A();
    }

    public static void testStackTraceB() {
        testStackTraceB_();
    }

    public static void testStackTraceB_() {
        //对象StackTraceAccessor在哪个方法内部创建，方法调用栈就记录到哪个方法
        StackTraceAccessor accessor = new StackTraceAccessor();
        accessor.A();
    }


    public static void testStackTraceC() {
        StackTraceAccessor accessor = new StackTraceAccessor();
        accessor.E();
    }

    public static void main(String[] args) {

        testStackTraceA();

        testStackTraceB();

        testStackTraceC();


        testResourceLeakDetector();
        testResourceLeakDetector2();
    }

    public static void testResourceLeakDetector() {
        System.out.println();
        System.out.println("testResourceLeakDetector() called !!!");

        ResourceLeakDetectorFactory factory = ResourceLeakDetectorFactory.instance();
        ResourceLeakDetector<TrackableObject> detector = factory.newResourceLeakDetector(TrackableObject.class, 0);
        detector.setLevel(ResourceLeakDetector.Level.PARANOID);

        ResourceLeakTracker track = track(detector);
        track.record(obj);
        track.record(obj);
        track.record(obj);
        track.record(obj);
        track.record(obj);
        System.out.println(track);
        track.close(obj);

        System.out.println();
        System.out.println("************************************************");
        System.out.println(track);
        System.out.println();


    }

    private static ResourceLeakTracker track(ResourceLeakDetector<TrackableObject> detector) {

        ResourceLeakTracker<TrackableObject> track = detector.track(obj);
        return track;
    }


    public static void testResourceLeakDetector2() {
        System.out.println();
        System.out.println("testResourceLeakDetector2() called !!!");

        ResourceLeakDetectorFactory factory = ResourceLeakDetectorFactory.instance();
        ResourceLeakDetector<TrackableObject> detector = factory.newResourceLeakDetector(TrackableObject.class, 0);
        detector.setLevel(ResourceLeakDetector.Level.PARANOID);

        ResourceLeakTracker track = track2(detector);
        track.record();
        track.record();
        track.record();
        track.record();
        track.record();

        System.out.println(track);

        System.out.println();
        System.out.println("************************************************");
        System.out.println(track);
        System.out.println();
    }

    private static ResourceLeakTracker track2(ResourceLeakDetector<TrackableObject> detector) {

        TrackableObject obj2 = new TrackableObject();
        ResourceLeakTracker<TrackableObject> track = detector.track(obj2);
        obj2 = null;
        return track;
    }

    public static class TrackableObject implements ResourceLeakHint {
        @Override
        public String toHintString() {
            return "I am a obj can be tracked";
        }
    }

    static class StackTraceAccessor extends Throwable {
        public StackTraceAccessor() {
            super();
        }

        public void A() {
            System.out.println("method A");
            B();
        }

        public void B() {
            System.out.println("method B");
            C();
        }

        public void C() {
            System.out.println("method C");
            D();
        }

        public void D() {
            System.out.println("method D");
            StackTraceElement[] array = getStackTrace();
            for (StackTraceElement element : array) {
                //System.out.println(element.getClassName() + ":  " + element.getMethodName() + ": " + element.getLineNumber());
                System.out.println(element.toString());
            }
        }


        public void E() {
            System.out.println("method E");
            F();
        }

        public void F() {
            System.out.println("method F");
            G();
        }

        public void G() {
            System.out.println("method G");
            StackTraceAccessor newAccessor = new StackTraceAccessor();
            StackTraceElement[] array = newAccessor.getStackTrace();
            //这里打印的StackTraceElement是在方法G中new出来的StackTraceAccessor类型的新对象newAccessor的栈帧
            for (StackTraceElement element : array) {
                System.out.println(element.toString());
            }
        }
    }
}
