package com.echomine.gnutella.impl;

import com.echomine.gnutella.GnutellaMessage;

import java.util.LinkedList;

/**
 * this class stores messages that are to be sent out.  If the message is waiting for a reply message, it will put it into the
 * outstanding queue.  Everything is synchronized for multithreading safety. The queue must be "start()ed" first
 * and shutdown() afterwards.
 */
public class MessageRequestQueue {
    private LinkedList msgQueue;
    private boolean shutdown;

    public MessageRequestQueue() {
        msgQueue = new LinkedList();
        shutdown = false;
    }

    /**
     * adds a message to send to the end of the queue. If the queue is shutdown,
     * then the message won't get added.
     */
    public void addMessage(GnutellaMessage msg) {
        if (!shutdown) {
            synchronized (msgQueue) {
                msgQueue.addLast(msg);
                //notify threads that's waiting for messages
                msgQueue.notify();
            }
        }
    }

    /**
     * this will go into a wait state, waiting for any incoming messages.
     * Once a message comes in and is retrieved, it will check to see if the message requires a reply.
     * If it is, the message is then put into the outstanding message queue, waiting for a reply to come back.
     * @return the message or null if no message
     */
    public GnutellaMessage waitForMessage() {
        GnutellaMessage msg = null;
        try {
            if (!shutdown) {
                synchronized (msgQueue) {
                    //wait until there is a new request
                    //or until we get interrupted
                    if ((msgQueue.size() == 0) && !shutdown)
                        msgQueue.wait();
                    if ((msgQueue.size() > 0) && !shutdown)
                        msg = (GnutellaMessage) msgQueue.removeFirst();
                }
            }
        } catch (InterruptedException ex) {
        }
        return msg;
    }

    /** wake up all the waiting threads possibly because someone is doing some shutdown work */
    public void shutdown() {
        shutdown = true;
        synchronized (msgQueue) {
            msgQueue.notifyAll();
        }
    }

    /** clear all the messages in the queues */
    public void clear() {
        synchronized (msgQueue) {
            msgQueue.clear();
            msgQueue.notifyAll();
        }
    }

    /** resets all states back to the default */
    public void start() {
        shutdown = false;
    }
}
