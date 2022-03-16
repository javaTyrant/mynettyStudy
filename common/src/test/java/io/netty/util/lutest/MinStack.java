package io.netty.util.lutest;

/**
 * 设计一个支持 push ，pop ，top 操作，并能在常数时间内检索到最小元素的栈。
 *
 * @author lufengxiang
 * @since 2022/3/9
 **/
public class MinStack {
    //
    private final Node head;
    //
    private Integer size;

    public MinStack() {
        head = new Node();
        size = 0;
    }

    //主要的逻辑.
    public void push(int val) {
        //
        int curMin = Integer.MAX_VALUE;
        //
        if (size > 0) {
            Node node = head.next;
            curMin = node.min;
        }
        Node node = new Node(val, Math.min(val, curMin));
        node.next = head.next;
        head.next = node;
        size++;
    }

    public void pop() {
        head.next = head.next.next;
        size--;
    }

    public int top() {
        return head.next.val;
    }

    public int getMin() {
        return head.next.min;
    }

    //通过链表来实现栈；以下是链表的节点的定义： 存储数据和当前的最小值
    public static class Node {
        public Integer val;
        public Integer min;
        public Node next;

        public Node(Integer val, Integer min) {
            this.val = val;
            this.min = min;
        }

        public Node() {
        }
    }

    public static void main(String[] args) {
        MinStack stack = new MinStack();
        stack.push(3);
        stack.push(-3);
        stack.push(-8);
        stack.pop();
        System.out.println(stack.getMin());
    }
}
