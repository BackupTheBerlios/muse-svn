package com.echomine.common;

/** Whenever a message being sent didn't go through, this exception is thrown. */
public class SendMessageFailedException extends Exception {
    public SendMessageFailedException() {
        super();
    }

    public SendMessageFailedException(String msg) {
        super(msg);
    }
}
