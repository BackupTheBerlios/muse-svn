package com.echomine.gnutella;

import com.echomine.common.ParseException;

/** Base class for representing all Gnutella messages. */
public abstract class GnutellaMessage {
    protected GnutellaMessageHeader header;

    public GnutellaMessage(GnutellaMessageHeader header) {
        this.header = header;
    }

    /** @return the message type (such as ping or pong) */
    public int getType() {
        return header.getFunction();
    }

    /**
     * convenience method for decrementing TTL for the message
     * @return true is expired, false otherwise
     */
    public boolean decTTL() {
        GnutellaMessageHeader header = getHeader();
        int ttl = header.getTTL() - 1;
        if (ttl <= 0) {
            // Expired
            return true;
        }
        if (ttl > 7)
            ttl = 7;
        header.setTTL(ttl);
        header.setHopsTaken(header.getHopsTaken() + 1);
        return false;
    }

    public void setHeader(GnutellaMessageHeader header) {
        this.header = header;
    }

    public GnutellaMessageHeader getHeader() {
        return header;
    }

    /** this default base method simply serializes the header of the message. */
    public byte[] serialize() throws ParseException {
        computeDataLen();
        int lenToSend = getSize();
        byte[] buf = new byte[lenToSend];
        serialize(buf, 0);
        return buf;
    }

    /** this default base method simply serializes the header of the message. */
    public int serialize(byte[] outbuf, int offset) throws ParseException {
        return header.serialize(outbuf, offset);
    }

    /**
     * this default base method does not deserialize any body as it assumes there is no
     * body to parse.  The header is already deserialized at this point and is thus
     * not important as part of the deserialization process.
     */
    public int deserialize(byte[] inbuf, int offset, int length) throws ParseException {
        // Already read the header.  Nothing else to read.
        return offset;
    }

    public int getSize() {
        return header.getSize();
    }

    public void copy(GnutellaMessage b) {
        header.copy(b.getHeader());
    }

    public void computeDataLen() {
        header.setDataLen(getSize() - header.getSize());
    }

    public String toString() {
        return "[" + header + "]";
    }

    /**
     * <p>routes the message.</p>
     * <p>The method has quite a few responsibilities.  It needs to determine if the message is
     * valid and is targeted to us.  It also determines if the message should be routed or not.
     * Decrementing the message's TTL is also a must if the message needs to be routed.
     */
    public abstract void route(GnutellaConnection connection, MessageRouterController controller);
}
