package com.echomine.gnutella;

import com.echomine.common.ParseException;
import com.echomine.util.ParseUtil;

/**
 * represents one query response file record.  Each query response will contains multiple file records, which is contained
 * in one of these objects.
 */
public class MsgResRecord {
    private int fileIndex = 0;
    private String filename = "";
    private int filesize = 0;
    private String extensionBlock = "";

    public MsgResRecord() {
    }

    public MsgResRecord(int fileIndex, int filesize, String fileName) {
        this.fileIndex = fileIndex;
        this.filesize = filesize;
        this.filename = fileName;
    }

    public MsgResRecord(int fileIndex, int filesize, String fileName, String extensionBlock) {
        this.fileIndex = fileIndex;
        this.filesize = filesize;
        this.filename = fileName;
        this.extensionBlock = extensionBlock;
    }

    public void setFileIndex(int fileIndex) {
        this.fileIndex = fileIndex;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public int getFilesize() {
        return filesize;
    }

    public void setFilesize(int filesize) {
        this.filesize = filesize;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getExtensionblock() {
        return extensionBlock;
    }

    public void setExtensionblock(String extensionBlock) {
        this.extensionBlock = extensionBlock;
    }

    public int getSize() {
        return 8 + filename.length() + ((extensionBlock == null)? 0 : extensionBlock.length()) + 2;  // plus 2 for 2 ending 0's
    }

    public void copy(MsgResRecord b) {
        this.fileIndex = b.fileIndex;
        this.filesize = b.filesize;
        this.filename = b.filename;
    }

    public int serialize(byte[] outbuf, int offset) throws ParseException {
        offset = ParseUtil.serializeIntLE(fileIndex, outbuf, offset); // Convert to Intel little-endian
        offset = ParseUtil.serializeIntLE(filesize, outbuf, offset); // Convert to Intel little-endian
        offset = ParseUtil.serializeString(filename, outbuf, offset);
        outbuf[offset++] = 0;
        if (extensionBlock != null && extensionBlock.length() > 0)
            offset = ParseUtil.serializeString(extensionBlock, outbuf, offset);
        outbuf[offset++] = 0;
        return offset;
    }

    public int deserialize(byte[] inbuf, int offset) throws ParseException {
        fileIndex = ParseUtil.deserializeIntLE(inbuf, offset);
        offset += 4;
        filesize = ParseUtil.deserializeIntLE(inbuf, offset);
        offset += 4;
        StringBuffer buf = new StringBuffer();
        offset = ParseUtil.deserializeString(inbuf, offset, buf);
        filename = buf.toString();
        // Skip the next 2 terminating 0.
        if (inbuf[offset] == 0 && offset < inbuf.length)
            offset++; // skip delimiter 0.
        buf = new StringBuffer();
        offset = ParseUtil.deserializeString(inbuf, offset, buf);
        //parse the extension block out of the data if there is any
        extensionBlock = buf.toString();
        if (inbuf[offset] == 0 && offset < inbuf.length)
            offset++; // skip terminating 0.
        return offset;
    }

    public String toString() {
        return "[" + "FileIndex=" + fileIndex + ", " + "FileSize=" + filesize + ", " + "Filename=" + filename + ", " + "ExtensionBlock=" + extensionBlock + "]";
    }
}
