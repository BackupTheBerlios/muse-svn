package com.echomine.util;

/**
 * read-write lock thread-safe implementation.  However, this class does not
 * resolve the issue of giving writers priority in writing.  Use ReadWritePriority for that instead.
 *  @see ReadWritePriority
 */
public class ReadWriteSafe implements ReadWrite {
    private boolean writing = false;
    private int readers = 0;

    public synchronized void acquireRead() throws InterruptedException {
        while (writing) wait();
        ++readers;
    }

    public synchronized void releaseRead() {
        --readers;
        if (readers == 0) notify();
    }

    public synchronized void acquireWrite() throws InterruptedException {
        while (readers > 0 || writing) wait();
        writing = true;
    }

    public synchronized void releaseWrite() {
        writing = false;
        notifyAll();
    }
}
