package com.echomine.gnutella;

import com.echomine.util.HexDec;
import com.echomine.util.RandomGUID;

import java.io.Serializable;


/**
 * Generates unique GUIDs since all messages must contain a unique ID for identification.
 * The class actually uses another GUID generator and then wraps that data with some
 * Gnutella-specific functionalities.
 * @see com.echomine.util.RandomGUID
 */
public class GUID implements Serializable {
    public static final int sDataLength = 16;
    private String mStr;
    private byte[] mBytes = new byte[sDataLength];
    private int mHashCode = 0;

    /**
     * Default constructor will create a GUID that will automatically
     * generate a GUID for you.  If you plan to deserialize right after
     * instantiation, use the other constructor that allows you the
     * option of not generating the GUID.
     */
    public GUID() {
        this(true);
    }

    /**
     * This construct will create a random GUID depending on whether the
     * boolean is true or not.  If generate is false, then this class will
     * not generate a GUID.  Thus, care must be taken when accessing some of the
     * information inside while the GUID is not generated as this could possibly
     * lead to NullPointerExceptions.  Usually, the only time when you don't want
     * to generate a GUID is when you are fairly certain that the next step is to
     * call deserialize, which will essentially obtain the GUID from the buffer.
     *
     */
    public GUID(boolean generate) {
        if (generate) {
            RandomGUID guid = new RandomGUID();
            mBytes = guid.getGUIDBytes();
            //according to Gnutella specs, byte 8 should be 0xff and byte 15 0x00
            //so the GUID needed to be modified
            mBytes[8] = (byte) 0xff;
            mBytes[15] = (byte) 0x00;
            mStr = HexDec.convertBytesToHexString(mBytes);
            computeHashCode();
        }
    }

    /**
     * Obtains our own generated hash code
     */
    public int hashCode() {
        return mHashCode;
    }

    /**
     * Compares the two GUID strings to make sure they are equal
     */
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof GUID)) return false;
        GUID guid = (GUID) obj;
        if (mStr.equals(guid.mStr)) return true;
        return false;
    }

    /**
     * Returns the size of the GUID (basically 16 bytes)
     */
    public int getSize() {
        return sDataLength;
    }

    /**
     * serializes the GUID into the byte buffer at the specified offset
     */
    public int serialize(byte[] outbuf, int offset) {
        // Copy my content to output buffer.
        System.arraycopy(mBytes, 0, outbuf, offset, sDataLength);
        // return new offset
        return offset + sDataLength;
    }

    /**
     * Deserializes the GUID from the byte buffer at the specified offset
     */
    public int deserialize(byte[] inbuf, int offset) {
        // Copy input buffer to my content.
        System.arraycopy(inbuf, offset, mBytes, 0, sDataLength);
        mStr = HexDec.convertBytesToHexString(mBytes);
        computeHashCode();
        // return new offset
        return offset + sDataLength;
    }

    /**
     * @return the 32 character string with no dashes
     */
    public String toString() {
        return mStr;
    }

    /**
     * @return the 32 character string with no dashes
     */
    public String getHexString() {
        return mStr;
    }

    /**
     * This retrieves a copy (not the original as it cannot be modified)
     * of the GUID bytes.
     * @return a copy of the GUID bytes
     */
    public byte[] getGUIDBytes() {
        byte[] temp = new byte[mBytes.length];
        System.arraycopy(mBytes, 0, temp, 0, temp.length);
        return temp;
    }

    /**
     * Computes the hash code for the given GUID
     */
    private void computeHashCode() {
        int hashedValue;
        int value;
        int v1, v2, v3, v4;
        v1 = (((int) mBytes[0]) < 0 ? ((int) mBytes[0]) + 256 : ((int) mBytes[0]));
        v2 = (((int) mBytes[1]) < 0 ? ((int) mBytes[1]) + 256 : ((int) mBytes[1]));
        v3 = (((int) mBytes[2]) < 0 ? ((int) mBytes[2]) + 256 : ((int) mBytes[2]));
        v4 = (((int) mBytes[3]) < 0 ? ((int) mBytes[3]) + 256 : ((int) mBytes[3]));
        hashedValue = (v1 << 24) | (v2 << 16) | (v3 << 8) | (v4);
        for (int i = 4; i < sDataLength; i += 4) {
            v1 = (((int) mBytes[i + 0]) < 0 ? ((int) mBytes[i + 0]) + 256 : ((int) mBytes[i + 0]));
            v2 = (((int) mBytes[i + 1]) < 0 ? ((int) mBytes[i + 1]) + 256 : ((int) mBytes[i + 1]));
            v3 = (((int) mBytes[i + 2]) < 0 ? ((int) mBytes[i + 2]) + 256 : ((int) mBytes[i + 2]));
            v4 = (((int) mBytes[i + 3]) < 0 ? ((int) mBytes[i + 3]) + 256 : ((int) mBytes[i + 3]));
            value = (v1 << 24) | (v2 << 16) | (v3 << 8) | (v4);
            hashedValue ^= value;
        }
        mHashCode = hashedValue;
    }
}
