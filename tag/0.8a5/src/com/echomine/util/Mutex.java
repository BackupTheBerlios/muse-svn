package com.echomine.util;

/** a Mutex is a semaphore that only permits one thread to have a lock. */
public class Mutex implements Sync {
    private Semaphore s = new Semaphore(1);

    public void acquire() throws InterruptedException {
        s.acquire();
    }

    public void release() {
        s.release();
    }

    public boolean attempt(long ms) throws InterruptedException {
        return s.attempt(ms);
    }
}
