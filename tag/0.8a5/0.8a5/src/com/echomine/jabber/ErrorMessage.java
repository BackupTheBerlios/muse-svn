package com.echomine.jabber;

/**
 * Contains the error messages and error codes associated with a specific error.  The naming convention may be misleading, but
 * this class does not extend JabberMessage.  It is usually used by a JabberMessage when parsing error messages.  Pretty much
 * this class is more of a helper class than a message class.
 */
public class ErrorMessage {
    private int code;
    private String msg;

    public ErrorMessage(String msg) {
        this(0, msg);
    }

    public ErrorMessage(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public String getMessage() {
        return msg;
    }

    public int getCode() {
        return code;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(100);
        if (code != 0)
            buf.append(code).append(" ");
        buf.append(msg);
        return buf.toString();
    }
}
