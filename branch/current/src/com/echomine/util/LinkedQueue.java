package com.echomine.util;

/** A thread-safe queue */
public class LinkedQueue {
    protected Node head = new Node(null);
    protected Node last = head;
    protected final Object pollLock = new Object();
    protected final Object putLock = new Object();

    public void put(Object x) {
        Node node = new Node(x);
        synchronized(putLock) {
            synchronized(last) {
                last.next = node;
                last = node;
            }
        }
    }

    public Object poll() {
        synchronized(pollLock) {
            synchronized(head) {
                Object x = null;
                Node first = head.next;
                if (first != null) {
                    x = first.object;
                    first.object = null;
                    head = first;
                }
                return x;
            }
        }
    }

    static class Node {
        Object object;
        Node next = null;

        Node(Object x) { object = x; }
    }
}
