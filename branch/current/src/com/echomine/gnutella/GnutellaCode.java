package com.echomine.gnutella;

/** Contains all the Message codes for gnutella messages. */
public class GnutellaCode {
    public static final int PING = 0x0;
    public static final int PONG = 0x1;
    public static final int PUSH_REQUEST = 0x40;
    public static final int QUERY = 0x80;
    public static final int QUERY_RESPONSE = 0x81;
    public static final int UNKNOWN = 0xff;
}
