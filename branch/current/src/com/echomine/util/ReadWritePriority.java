package com.echomine.util;

/** ReadWriteSafe locking but with priority */
public class ReadWritePriority extends ReadWriteSafe {
    private int waitingW = 0;
    private boolean writing = false;
    private int readers = 0;

    public synchronized void acquireRead() throws InterruptedException {
        while (writing || waitingW > 0) wait();
        ++readers;
    }

    public synchronized void acquireWrite() throws InterruptedException {
        ++waitingW;
        while (readers > 0 || writing) wait();
        --waitingW;
        writing = true;
    }
}
