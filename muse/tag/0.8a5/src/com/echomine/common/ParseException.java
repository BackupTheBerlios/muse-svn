package com.echomine.common;

/** any problem while parsing anything can throw this exception. */
public class ParseException extends Exception {
    public ParseException() {
        super();
    }

    public ParseException(String error) {
        super(error);
    }
}
