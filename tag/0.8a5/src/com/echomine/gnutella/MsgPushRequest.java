package com.echomine.gnutella;

import com.echomine.common.ParseException;
import com.echomine.util.IPUtil;
import com.echomine.util.ParseUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Message class that represents a Push Request message. Essentially a push request is basically a
 * firewalled download request.
 */
public class MsgPushRequest extends GnutellaMessage {
    private static final int sDataLength = 26;
    private GUID remoteClientID;
    private int fileIndex;
    private InetAddress remoteHost;
    private short remotePort;

    public MsgPushRequest(GnutellaMessageHeader header) {
        super(header);
        remoteClientID = new GUID();
        fileIndex = 0;
        remoteHost = null;
        remotePort = 0;
    }

    public MsgPushRequest(GUID guid, InetAddress host, short port, int fileIndex) {
        super(new GnutellaMessageHeader(GnutellaCode.PUSH_REQUEST));
        this.fileIndex = fileIndex;
        this.remoteClientID = guid;
        this.remoteHost = host;
        this.remotePort = port;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public void setFileIndex(int fileIndex) {
        this.fileIndex = fileIndex;
    }

    public InetAddress getRemoteHost() {
        return remoteHost;
    }

    public void setRemoteHost(InetAddress remoteHost) {
        this.remoteHost = remoteHost;
    }

    public short getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(short remotePort) {
        this.remotePort = remotePort;
    }

    public GUID getRemoteClientID() {
        return remoteClientID;
    }

    public void setRemoteClientID(GUID remoteClientID) {
        this.remoteClientID = remoteClientID;
    }

    public int getSize() {
        return super.getSize() + remoteClientID.getSize() + 4 + 4 + 2;
    }

    public void copy(MsgPushRequest b) {
        super.copy(b);
        remoteClientID = b.remoteClientID;
        fileIndex = b.fileIndex;
        remoteHost = b.remoteHost;
        remotePort = b.remotePort;
    }

    public int serialize(byte[] outbuf, int offset) throws ParseException {
        offset = super.serialize(outbuf, offset);
        offset = remoteClientID.serialize(outbuf, offset);
        offset = ParseUtil.serializeIntLE(fileIndex, outbuf, offset); // Convert to Intel little-endian
        System.arraycopy(remoteHost.getAddress(), 0, outbuf, offset, 4);
        offset += 4;
        offset = ParseUtil.serializeShortLE(remotePort, outbuf, offset);
        return offset;
    }

    public int deserialize(byte[] inbuf, int offset, int length) throws ParseException {
        try {
            if (length != sDataLength)
                throw new ParseException("Invalid Push Message");
            // Already read the header.
            offset = super.deserialize(inbuf, offset, length);
            offset = remoteClientID.deserialize(inbuf, offset);
            fileIndex = ParseUtil.deserializeIntLE(inbuf, offset);
            offset += 4;
            StringBuffer strBuf = new StringBuffer();
            offset = IPUtil.deserializeIP(inbuf, offset, strBuf);
            remoteHost = InetAddress.getByName(strBuf.toString());
            remotePort = ParseUtil.deserializeShortLE(inbuf, offset);
            offset += 2;
        } catch (UnknownHostException ex) {
            throw new ParseException(ex.getMessage());
        }
        return offset;
    }

    public String toString() {
        return "[" + header + " " + "RemoteClientID=" + remoteClientID + ", " + "FileIndex=" + fileIndex + ", " +
                "RemoteHost=" + remoteHost.getHostAddress() + ", " + "RemotePort=" + remotePort + "]";
    }

    /**
     * handles push requests.
     */
    public void route(GnutellaConnection connection, MessageRouterController controller) {
        //check if the push request is actually for us
        if (controller.getContext().getClientID().equals(getRemoteClientID()))
            return;
        //if not for us, then check if we were the one who routed the query response
        GnutellaConnection receiver = controller.getPushRequestRoute(getRemoteClientID());
        if (receiver == null) return;
        // Ok, I did forward the QueryResponse msg on behalf of returnHost.
        // The PushRequest is for the returnHost.  Better route it back.
        // if TTL didn't expire, send message
        controller.routeMessage(receiver, this);
    }
}
