package com.echomine.gnutella;

import com.echomine.common.ParseException;
import com.echomine.util.ParseUtil;

/** a search query object. */
public class MsgQuery extends GnutellaMessage {

    private short minSpeed = 32767; // 15th bit set to "1" to indicate support
    private String searchString;
    private String extendedQueryBlock = ""; // Either for HUGE or metadata XML

    public MsgQuery() {
        this(new GnutellaMessageHeader(GnutellaCode.QUERY));
    }

    public MsgQuery(GnutellaMessageHeader header) {
        super(header);
        minSpeed = 0;
        searchString = "";
    }

    public MsgQuery(String searchString) {
        this(searchString, (short) 0);
    }

    public MsgQuery(String searchString, short minSpeed) {
        super(new GnutellaMessageHeader(GnutellaCode.QUERY));
        this.minSpeed = minSpeed;
        this.searchString = searchString;
    }

    /** sets whether the source is firewalled so that the query can be filtered to have better results */
    public void setSourceFirewalled(boolean sourceBehindFW) {
        if (sourceBehindFW) {
            minSpeed |= (1 << 14);
        } else {
            minSpeed &= ~(1 << 14);
        }
    }

    /** whether source should accept metadata or not */
    public void setAcceptMetadata(boolean acceptMD) {
        if (acceptMD) {
            minSpeed |= (1 << 13);
        } else {
            minSpeed &= ~(1 << 13);
        }
    }

    public void setMinSpeed(short minSpeed) {
        this.minSpeed = minSpeed;
    }

    public short getMinSpeed() {
        return minSpeed;
    }

    /** @return whether the query should return results from firewalled clients */
    public boolean isSourceFirewalled() {
        if ((minSpeed & (1 << 14)) != 0)
            return true;
        else
            return false;
    }

    /** @return whether the query should return results who accepts metadata */
    public boolean acceptsMetadata() {
        if ((minSpeed & (1 << 13)) != 0)
            return true;
        else
            return false;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public String getSearchString() {
        return searchString;
    }

    public int getSize() {
        return super.getSize() + 2 + searchString.length() + extendedQueryBlock.length() + 2; // plus 2 for the ending 0 + seperating 0.
    }

    public void copy(MsgQuery b) {
        super.copy(b);
        minSpeed = b.minSpeed;
        searchString = b.searchString;
    }

    /**
     * sets the query extension block.  As this is contributed code, I do not know
     * the format of the extension block.  If you know it, please edit this comment
     * and submit me the patch to be included in the source code.  -- ckchris
     */
    public void setExtensionBlock(String extBlock) {
        extendedQueryBlock = (extBlock == null) ? "" : extBlock;
    }

    /** @return the extension block contents.  empty string if there isn't any */
    public String getExtensionBlock() {
        return extendedQueryBlock;
    }

    public int serialize(byte[] outbuf, int offset) throws ParseException {
        offset = super.serialize(outbuf, offset);
        offset = ParseUtil.serializeShortLE(minSpeed, outbuf, offset);
        offset = ParseUtil.serializeString(searchString, outbuf, offset);
        outbuf[offset++] = 0;
        if (extendedQueryBlock != null && extendedQueryBlock.length() > 0)
            offset = ParseUtil.serializeString(extendedQueryBlock, outbuf, offset);
        outbuf[offset++] = 0;
        return offset;
    }

    public int deserialize(byte[] inbuf, int offset, int length) throws ParseException {
        //validate body length (must at least be 2 bytes)
        if (length <= 2)
            throw new ParseException("Invalid Query Message");
        // Already read the header.
        offset = super.deserialize(inbuf, offset, length);
        minSpeed = ParseUtil.deserializeShortLE(inbuf, offset);
        offset += 2;
        StringBuffer buf = new StringBuffer();
        offset = ParseUtil.deserializeString(inbuf, offset, buf);
        if (offset < inbuf.length && inbuf[offset] == 0)
            offset++; // skip terminating 0.
        searchString = buf.toString();

        if (offset < inbuf.length && inbuf[offset] == 0)
            return offset; // no extension block
        buf = new StringBuffer();
        offset = ParseUtil.deserializeString(inbuf, offset, buf);
        if (offset < inbuf.length && inbuf[offset] == 0)
            offset++; // skip terminating 0.
        extendedQueryBlock = buf.toString();
        return offset;
    }

    public String toString() {
        return "[" + header + " " + "MinSpeed=" + minSpeed + ", " + "SearchString=" + searchString + ", Extended=" + extendedQueryBlock + "]";
    }

    /**
     * handles query messages.  All query messages are sent to higher-level listeners.
     */
    public void route(GnutellaConnection connection, MessageRouterController controller) {
        // See if I have seen this Query before.  Drop msg if duplicate.
        // if it's my own query msg, ignore it as well
        if (controller.checkAndAddMsgSeen(this) || controller.isMsgOwner(this))
            return;
        // Add the Query msg to the routing table so that I know where
        // to route the QueryResponse back.
        controller.enableMessageRouting(this, connection);
        // Forward the Query msg to other connected hosts except the one who sent it.
        // if TTL didn't expire, send message
        controller.routeMessageToAllExcept(connection, this);
    }
}
