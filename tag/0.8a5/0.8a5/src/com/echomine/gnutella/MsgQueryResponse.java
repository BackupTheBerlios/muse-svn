package com.echomine.gnutella;

import com.echomine.common.ParseException;
import com.echomine.util.IPUtil;
import com.echomine.util.ParseUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Vector;

/** A query response that consists of search results. */
public class MsgQueryResponse extends GnutellaMessage {
    private short remotePort;
    private int numRecords;
    private InetAddress remoteHost;
    private int remoteHostSpeed;
    private GUID remoteClientID;
    private Vector records; // MsgResRecord

    public MsgQueryResponse(GnutellaMessageHeader header) {
        super(header);
        numRecords = 0;
        remotePort = 0;
        remoteHost = null;
        remoteHostSpeed = 0;
        this.remoteClientID = null;
        records = new Vector();
    }

    /**
     * This constructor is mainly used to create your own Query Response to a query. It will automatically
     * set the line speed, the host, and the port for you by retrieving the information from the context.
     * If you are replying to a query, the serverID is your own client ID (usually obtained from the GnutellaContext).
     */
    public MsgQueryResponse(GnutellaMessageHeader header, GnutellaContext context) {
        super(header);
        numRecords = 0;
        remotePort = context.getPort();
        try {
            remoteHost = context.getInterfaceIP();
            if (remoteHost == null)
                remoteHost = InetAddress.getLocalHost();
        } catch (UnknownHostException ex) {
        }
        remoteHostSpeed = context.getLineSpeed();
        this.remoteClientID = context.getClientID();
        records = new Vector();
    }

    public int getNumRecords() {
        return numRecords;
    }

    public short getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(short remotePort) {
        this.remotePort = remotePort;
    }

    public InetAddress getRemoteHost() {
        return remoteHost;
    }

    public void setRemoteHost(InetAddress remoteHost) {
        this.remoteHost = remoteHost;
    }

    public int getRemoteHostSpeed() {
        return remoteHostSpeed;
    }

    public void setRemoteHostSpeed(int remoteHostSpeed) {
        this.remoteHostSpeed = remoteHostSpeed;
    }

    public GUID getRemoteClientID() {
        return remoteClientID;
    }

    public void setRemoteClientID(GUID remoteClientID) {
        this.remoteClientID = remoteClientID;
    }

    public void addMsgRecord(MsgResRecord record) {
        records.addElement(record);
        numRecords = records.size();
    }

    public MsgResRecord getMsgRecord(int i) {
        return (MsgResRecord) records.elementAt(i);
    }

    public int getSize() {
        int size = super.getSize() + 4 + 2 + 4 + 4 + remoteClientID.getSize();
        MsgResRecord rec;
        for (int i = 0; i < numRecords; i++) {
            rec = (MsgResRecord) records.elementAt(i);
            size += rec.getSize();
        }
        return size;
    }

    public void copy(MsgQueryResponse b) {
        super.copy(b);
        numRecords = b.numRecords;
        remotePort = b.remotePort;
        remoteHost = b.remoteHost;
        remoteHostSpeed = b.remoteHostSpeed;
        remoteClientID = b.remoteClientID;
        MsgResRecord rec;
        int size = b.getNumRecords();
        for (int i = 0; i < size; i++) {
            rec = new MsgResRecord();
            rec.copy(b.getMsgRecord(i));
            addMsgRecord(rec);
        }
    }

    public int serialize(byte[] outbuf, int offset) throws ParseException {
        offset = super.serialize(outbuf, offset);
        outbuf[offset++] = (byte) numRecords;
        offset = ParseUtil.serializeShortLE(remotePort, outbuf, offset);
        System.arraycopy(remoteHost.getAddress(), 0, outbuf, offset, 4);
        offset += 4;
        offset = ParseUtil.serializeIntLE(remoteHostSpeed, outbuf, offset);
        for (int i = 0; i < numRecords; i++) {
            offset = getMsgRecord(i).serialize(outbuf, offset);
        }
        offset = remoteClientID.serialize(outbuf, offset);
        return offset;
    }

    public int deserialize(byte[] inbuf, int offset, int length) throws ParseException {
        try {
            //validate body length (must be at least 11 bytes
            if (length <= 11)
                throw new ParseException("Invalid Query Response Message");
            //get the end index of the data in the buffer first
            int end = length + offset;
            // Already read the header.
            offset = super.deserialize(inbuf, offset, length);
            byte n = inbuf[offset++];
            //max 256 records since it's only one byte
            numRecords = (n < 0 ? 256 + n : n);
            remotePort = ParseUtil.deserializeShortLE(inbuf, offset);
            offset += 2;
            StringBuffer strBuf = new StringBuffer();
            offset = IPUtil.deserializeIP(inbuf, offset, strBuf);
            remoteHost = InetAddress.getByName(strBuf.toString());
            remoteHostSpeed = ParseUtil.deserializeIntLE(inbuf, offset);
            offset += 4;
            MsgResRecord rec;
            for (int i = 0; i < numRecords; i++) {
                rec = new MsgResRecord();
                offset = rec.deserialize(inbuf, offset);
                records.addElement(rec);
            }
            if (remoteClientID == null)
                remoteClientID = new GUID();
            //the last deserializing must start from the end of the data length
            //The reason is to compensate for those clients that pass in
            //additional data in the Query response (ie. BearShare or Gnotella)
            //If that's the case, then reading the next 16 bytes will NOT result
            //in obtaining the remote client ID.
            try {
                offset = remoteClientID.deserialize(inbuf, end - GUID.sDataLength);
            } catch (ArrayIndexOutOfBoundsException ex) {
                System.out.println("ArrayIndexOutOfBounds: buflen=" + inbuf.length + ",end=" + end + ",length=" + length);
            }
        } catch (UnknownHostException ex) {
            throw new ParseException(ex.getMessage());
        }
        return offset;
    }

    public String getRemoteHostStr() {
        return remoteHost.getHostAddress() + ":" + remotePort;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(256);
        buf.append("[" + header + " " + "NumRecords=" + numRecords + ", " + "RemotePort=" + remotePort + ", " + "RemoteHost=" +
            remoteHost.getHostAddress() + ", " + "RemoteHostSpeed=" + remoteHostSpeed + ", " + "RemoteClientID=" +
            remoteClientID + ", " + "Records=");
        for (int i = 0; i < numRecords; i++) {
            buf.append(getMsgRecord(i)).append(" ");
        }
        buf.append("]");
        return buf.toString();
    }

    /**
     * handles query response messages.
     */
    public void route(GnutellaConnection connection, MessageRouterController controller) {
        //if the msg is for us, then fire it up and no need to send it to others
        if (controller.isMsgOwner(this))
            return;
        // QueryResponse is a response to a Query msg.
        // If I didn't route the query to begin with, just ignore the request.
        // Query response is NOT sent up to the event listener
        GnutellaConnection receiver = controller.getMessageRouting(this);
        if (receiver == null) return;
        //add to the push routing table for future push request usage
        controller.enablePushRequestRouting(getRemoteClientID(), connection);
        // Ok, I did forward the Query msg on behalf of returnHost.
        // The QueryResponse is for returnHost.  Better route it back.
        // if TTL didn't expire, send message
        controller.routeMessage(receiver, this);
    }
}
