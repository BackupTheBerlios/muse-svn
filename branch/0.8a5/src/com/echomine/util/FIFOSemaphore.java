package com.echomine.util;

/**
 * this is a semaphore that added priority to the wait list.  Everyone literally wait in line.  Whoever asks for
 * it first will get it next.
 */
public class FIFOSemaphore extends Semaphore {
    protected final WaitQueue queue = new WaitQueue();

    public FIFOSemaphore(long initial) {
        super(initial);
    }

    public void acquire() throws InterruptedException {
        if (Thread.interrupted()) throw new InterruptedException();
        WaitNode node = null;
        synchronized(this) {
            if (permits > 0) { //no need to queue
                --permits;
                return;
            } else {
                node = new WaitNode();
                queue.enq(node);
            }
        }
        node.wait();
    }

    public void release() {
        for ( ; ; ) { //retry until success
            WaitNode node = queue.deq();
            if (node == null) { //queue is empty
                ++permits;
                return;
            } else if (node.doNotify())
                return;
            //else node was already released due to interruption or time-out, so must retry
        }
    }

    protected static class WaitNode {
        boolean released = false;
        FIFOSemaphore.WaitNode next = null;

        synchronized void doWait() throws InterruptedException {
            try {
                while (!released)
                    wait();
            } catch (InterruptedException ex) {
                if (!released) { //interrupted before notified
                    //suppress future notifications
                    released = true;
                    throw ex;
                } else { //interrupted after notified
                    //ignore exception but propagate status
                    Thread.currentThread().interrupt();
                }
            }
        }

        synchronized boolean doNotify() {
            if (released) //was interrupted or timed out
                    return false;
            else {
                released = true;
                notify();
                return true;
            }
        }

        synchronized boolean doTimedWait(long msecs) throws InterruptedException {
            //not implemented
            return false;
        }
    }


    protected static class WaitQueue {
        protected WaitNode head = null;
        protected WaitNode last = null;

        protected void enq(WaitNode node) {
            if (last == null)
                head = last = node;
            else {
                last.next = node;
                last = node;
            }
        }

        protected WaitNode deq() {
            WaitNode node = head;
            if (node != null) {
                head = node.next;
                if (head == null) last = null;
                node.next = null;
            }
            return node;
        }
    }
}
