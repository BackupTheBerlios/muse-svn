package com.echomine.gnutella;

import com.echomine.common.ParseException;

/**
 * Any messages that are not known or is proprietary goes under this class.  If you know what the message is, you can always
 * add your own gnutella message
 */
public class MsgUnknown extends GnutellaMessage {
    private byte[] body;

    public MsgUnknown(GnutellaMessageHeader header) {
        super(header);
        body = null;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public byte[] getBody() {
        return body;
    }

    public int getSize() {
        return super.getSize() + body.length;
    }

    public void copy(MsgUnknown b) {
        super.copy(b);
        body = b.body;
    }

    public int serialize(byte[] outbuf, int offset) throws ParseException {
        offset = super.serialize(outbuf, offset);
        System.arraycopy(body, 0, outbuf, offset, body.length);
        offset += body.length;
        return offset;
    }

    public int deserialize(byte[] inbuf, int offset, int length) throws ParseException {
        // Already read the header.
        offset = super.deserialize(inbuf, offset, length);
        body = new byte[header.getDataLen()];
        System.arraycopy(inbuf, offset, body, 0, body.length);
        offset += body.length;
        return offset;
    }

    public String toString() {
        return "[" + header + ", " + new String(body, 0, body.length) + "]";
    }

    /**
     * all unknown messages are NOT routed and are silently dropped.
     */
    public void route(GnutellaConnection connection, MessageRouterController controller) {
        // See if I have seen this msg before.  Drop msg if duplicate.
        // or if it's our own message, ignore as well
        if (controller.checkAndAddMsgSeen(this) || controller.isMsgOwner(this))
            return;
    }
}
