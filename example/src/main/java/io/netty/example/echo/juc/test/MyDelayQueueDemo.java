package io.netty.example.echo.juc.test;

import io.netty.example.echo.juc.*;

import java.util.concurrent.*;
import java.util.function.Function;

/**
 * @author lumac
 * @since 2021/5/9
 */
@SuppressWarnings("unused")
public class MyDelayQueueDemo {
    public static final int SIZE = 10;

    //主要看下put和take的顺序.put的时候
    public static void main(String[] args) throws InterruptedException {
        MyDelayQueueDemo test = new MyDelayQueueDemo();
        //初始化线程池
        BlockingQueue<Runnable> arrayBlockingQueue = new ArrayBlockingQueue<>(10);
        MyThreadPoolExecutor threadPool = new MyThreadPoolExecutor
                (5, 10, 10, TimeUnit.MILLISECONDS,
                        arrayBlockingQueue, Executors.defaultThreadFactory(),
                        new MyThreadPoolExecutor.AbortPolicy());

        MyDelayQueue<DelayedTask> delayTaskQueue = new MyDelayQueue<>();
        //模拟SIZE个延迟任务
        for (byte i = 0; i < SIZE; i++) {
            Long runAt = System.currentTimeMillis() + 1000 * i;
            String name = "Zhang_" + i;
            byte age = (byte) (10 + i);
            String gender = (i % 2 == 0 ? "male" : "female");
            //Student student = new StudentBuilder(name, age, gender).height(150 + i).province("ZheJiang").build();
            Student student = new Student();
            student.setAge(age);
            student.setGender(gender);
            student.setName(name);
            student.setHeight(150 + i);
            delayTaskQueue.put(new DelayedTask<>(student, 1, function -> test.print(student), runAt));
        }
        Student s = new Student("小鲁", 18, 180, "male");
        delayTaskQueue.put(new DelayedTask<>(s, 1, function -> test.print(s), System.currentTimeMillis() + 10000000L));
        Thread.sleep(100);
        //因为poll是没有等待的,会一直返回null,所以取的时候要死循环取.
        while (true) {
            if (delayTaskQueue.size() == 0) {
                break;
            }
            try {
                //从延迟队列中取值,如果没有对象过期则取到null
                DelayedTask delayedTask = delayTaskQueue.poll();
                if (delayedTask != null) {
                    threadPool.execute(delayedTask);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        threadPool.shutdown();
    }


    public String print(Student object) {
        System.out.println(Thread.currentThread().getName());
        String str = ">>>junit log>>>" + object.getClass().getSimpleName() + ":" + object.toString();
        System.out.println(str);
        return str;
    }

    static class Student {
        private String name;
        private int age;
        private int height;
        private String gender;

        public Student() {
        }

        public Student(String name, int age, int height, String gender) {
            this.name = name;
            this.age = age;
            this.height = height;
            this.gender = gender;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        @Override
        public String toString() {
            return "Student{" +
                    "name='" + name + '\'' +
                    ", age=" + age +
                    ", height=" + height +
                    ", gender='" + gender + '\'' +
                    '}';
        }
    }

    //任务要实现两个方法.
    private static class DelayedTask<T> implements Delayed, Runnable {

        /**
         * 任务参数
         */
        private final T taskParam;

        /**
         * 任务类型
         */
        private final Integer type;

        /**
         * 任务函数
         */
        private final Function<T, String> function;

        /**
         * 任务执行时刻
         */
        private final Long runAt;

        public T getTaskParam() {
            return taskParam;
        }

        public Integer getType() {
            return type;
        }

        public Function<T, String> getFunction() {
            return function;
        }

        public Long getRunAt() {
            return runAt;
        }

        DelayedTask(T taskParam, Integer type, Function<T, String> function, Long runAt) {
            this.taskParam = taskParam;
            this.type = type;
            this.function = function;
            this.runAt = runAt;
        }

        @Override
        public void run() {
            if (taskParam != null) {
                function.apply(taskParam);
            }
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(this.runAt - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            DelayedTask object = (DelayedTask) o;
            return this.runAt.compareTo(object.getRunAt());
        }
    }
}
