package com.echomine.gnutella;

import com.echomine.common.ParseException;
import com.echomine.util.ParseUtil;

/** Message header for every gnutella message that is received */
public class GnutellaMessageHeader {
    // Constant
    public static final int sDataLength = GUID.sDataLength + 7;
    // Atributes
    private GUID mMsgID;
    private int mFunction; // message type
    private int mTTL;
    private int mHopsTaken;
    private int mDataLen;
    private long mArrivalTime;

    /**
     * Creates a message header that does not contain a GUID.
     * This constructor is mostly used to handle incoming messages.
     */
    public GnutellaMessageHeader() {
        this(GnutellaCode.UNKNOWN, new GUID(false));
    }

    /**
     * creates a message header that contains a default GUID.
     * This constructor is mostly used for creating a new outgoing message.
     */
    public GnutellaMessageHeader(int msgtype) {
        this(msgtype, new GUID());
    }

    public GnutellaMessageHeader(int msgtype, GUID guid) {
        mMsgID = guid;
        mFunction = msgtype;
        mTTL = 7;
        mHopsTaken = 0;
        mDataLen = 0;
    }

    public GUID getMsgID() {
        return mMsgID;
    }

    public void setMsgID(GUID MsgID) {
        this.mMsgID = MsgID;
    }

    public int getFunction() {
        return mFunction;
    }

    public void setFunction(int Function) {
        this.mFunction = Function;
    }

    public String getFunctionName() {
        switch (mFunction) {
            case GnutellaCode.PING:
                return "Init";
            case GnutellaCode.PONG:
                return "InitResponse";
                //case GnutellaCode.CHAT:
                //	return "Channel Chat";
            case GnutellaCode.PUSH_REQUEST:
                return "PushRequest";
            case GnutellaCode.QUERY:
                return "Query";
            case GnutellaCode.QUERY_RESPONSE:
                return "QueryResponse";
            default:
                return "Unknown";
        }
    }

    public int getTTL() {
        return mTTL;
    }

    public void setTTL(int TTL) {
        this.mTTL = TTL;
    }

    public int getHopsTaken() {
        return mHopsTaken;
    }

    public void setHopsTaken(int HopsTaken) {
        this.mHopsTaken = HopsTaken;
    }

    public int getDataLen() {
        return mDataLen;
    }

    public void setDataLen(int DataLen) {
        this.mDataLen = DataLen;
    }

    public long getArrivalTime() {
        return mArrivalTime;
    }

    public void setArrivalTime(long arrivalTime) {
        this.mArrivalTime = arrivalTime;
    }

    public int getSize() {
        return sDataLength;
    }

    public void copy(GnutellaMessageHeader b) {
        mMsgID = b.mMsgID;
        mFunction = b.mFunction;
        mTTL = b.mTTL;
        mHopsTaken = b.mHopsTaken;
        mDataLen = b.mDataLen;
    }

    public int serialize(byte[] outbuf, int offset) throws ParseException {
        offset = mMsgID.serialize(outbuf, offset);
        outbuf[offset++] = (byte) mFunction;
        outbuf[offset++] = (byte) mTTL;
        outbuf[offset++] = (byte) mHopsTaken;
        offset = ParseUtil.serializeIntLE(mDataLen, outbuf, offset); // Convert to Intel little-endian
        return offset;
    }

    public int deserialize(byte[] inbuf, int offset) throws ParseException {
        offset = mMsgID.deserialize(inbuf, offset);
        mFunction = inbuf[offset++];
        mFunction = (mFunction < 0 ? mFunction + 256 : mFunction);
        mTTL = inbuf[offset++];
        mTTL = (mTTL < 0 ? mTTL + 256 : mTTL);
        mHopsTaken = inbuf[offset++];
        mHopsTaken = (mHopsTaken < 0 ? mHopsTaken + 256 : mHopsTaken);
        mDataLen = ParseUtil.deserializeIntLE(inbuf, offset);
        if (mDataLen < 0) mDataLen = 0;
        offset += 4;
        return offset;
    }

    public String toString() {
        return "[" + "MsgID=" + mMsgID + ", " + "Function=" + mFunction + "(" + getFunctionName() + "), " + "TTL=" + mTTL +
            ", " + "HopsTaken=" + mHopsTaken + ", " + "DataLen=" + mDataLen + "]";
    }
}
