package com.echomine.util;

public interface ReadWrite {
    public void acquireRead() throws InterruptedException;

    public void releaseRead();

    public void acquireWrite() throws InterruptedException;

    public void releaseWrite();
}
