package com.echomine.util;

/**
 * A latch is a lock that acts like a one way switch semaphore.  It starts off as being false, but when it is acquired once,
 * it is set to true and stays true even if you call release.  This is used to for a one-time deal.
 */
public class Latch implements Sync {
    protected boolean state = false;

    public synchronized void reset() {
        state = false;
    }

    public synchronized void acquire() throws InterruptedException {
        while (state == false) wait();
    }

    public synchronized void release() {
        // once released, the state is set
        state = true;
        notifyAll();
    }

    public boolean attempt(long msec) throws InterruptedException {
        // not implemented
        return false;
    }
}
