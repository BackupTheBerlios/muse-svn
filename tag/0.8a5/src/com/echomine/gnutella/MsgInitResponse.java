package com.echomine.gnutella;

import com.echomine.common.ParseException;
import com.echomine.util.IPUtil;
import com.echomine.util.ParseUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** Essentially a PONG message */
public class MsgInitResponse extends GnutellaMessage {
    private static final int sDataLength = 14;
    private short port;
    private InetAddress IP;
    private int fileCount;
    private int totalSize;

    public MsgInitResponse(GnutellaMessageHeader header) {
        super(header);
        port = 0;
        IP = null;
        fileCount = 0;
        totalSize = 0;
    }

    public void setPort(short port) {
        this.port = port;
    }

    public short getPort() {
        return port;
    }

    public void setIP(InetAddress IP) {
        this.IP = IP;
    }

    public InetAddress getIP() {
        return IP;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    public int getFileCount() {
        return fileCount;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public int getSize() {
        return super.getSize() + sDataLength;
    }

    public void copy(MsgInitResponse b) {
        super.copy(b);
        this.port = b.port;
        this.IP = b.IP;
        this.fileCount = b.fileCount;
        this.totalSize = b.totalSize;
    }

    public int serialize(byte[] outbuf, int offset) throws ParseException {
        offset = super.serialize(outbuf, offset);
        offset = ParseUtil.serializeShortLE(port, outbuf, offset); // Convert to Intel little-endian
        System.arraycopy(IP.getAddress(), 0, outbuf, offset, 4);
        offset += 4;
        offset = ParseUtil.serializeIntLE(fileCount, outbuf, offset); // Convert to Intel little-endian
        offset = ParseUtil.serializeIntLE(totalSize, outbuf, offset); // Convert to Intel little-endian
        return offset;
    }

    public int deserialize(byte[] inbuf, int offset, int length) throws ParseException {
        try {
            //validate the body length
            if (length != sDataLength)
                throw new ParseException("Invalid Pong Message");
            offset = super.deserialize(inbuf, offset, length);
            // Already read the header.
            port = ParseUtil.deserializeShortLE(inbuf, offset);
            offset += 2;
            StringBuffer strBuf = new StringBuffer();
            offset = IPUtil.deserializeIP(inbuf, offset, strBuf);
            IP = InetAddress.getByName(strBuf.toString());
            fileCount = ParseUtil.deserializeIntLE(inbuf, offset);
            offset += 4;
            totalSize = ParseUtil.deserializeIntLE(inbuf, offset);
            offset += 4;
        } catch (UnknownHostException ex) {
            throw new ParseException(ex.getMessage());
        }
        return offset;
    }

    public String toString() {
        return "[" + header + " " + "Port=" + port + ", " + "IP=" + IP.getHostAddress() + ", " + "FileCount=" + fileCount +
                ", " + "TotalSize=" + totalSize + "]";
    }

    /**
     * handle ping response messages.
     */
    public void route(GnutellaConnection connection, MessageRouterController controller) {
        // InitResponse is a response to an Init msg.
        // If init message was not received by us, then ignore it.
        GnutellaConnection receiver = controller.getMessageRouting(this);
        if (receiver == null) return;
        // if TTL didn't expire, send message
        controller.routeMessage(receiver, this);
    }
}
