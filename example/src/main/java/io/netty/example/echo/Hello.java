package io.netty.example.echo;

/**
 * @author lumac
 * @since 2021/9/8
 */
public class Hello {
   static class A{
        void say(){
            System.out.println("a");
        }
    }
    static class B extends A{
        void say(){
            super.say();
            System.out.println("b");
        }
    }

    public static void main(String[] args) {
        B b = new B();
        b.say();
    }
}
