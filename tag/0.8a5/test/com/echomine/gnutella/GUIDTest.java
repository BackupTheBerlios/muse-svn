package com.echomine.gnutella;

import junit.framework.TestCase;
import com.echomine.gnutella.GUID;
import com.echomine.util.HexDec;

public class GUIDTest extends TestCase {
    public GUIDTest(String name) {
        super(name);
    }

    public void testGUIDDeserialization() throws Exception {
        GUID guid = new GUID();
        byte[] bguid = guid.getGUIDBytes();
        GUID newguid = new GUID(false);
        newguid.deserialize(bguid, 0);
        assertEquals("GUID deserialization failure: Deserialized GUID=" + newguid.getHexString() + ", Expected GUID=" + guid.getHexString(), guid.getHexString(), newguid.getHexString());
    }

    public void testGUIDSerialization() throws Exception {
        GUID guid = new GUID();
        byte[] bguid = new byte[guid.getSize()];
        guid.serialize(bguid, 0);
        String hexGUID = HexDec.convertBytesToHexString(bguid);
        assertEquals("GUID Serialization failure: Serialized GUID=" + hexGUID + ", Expected GUID=" + guid.getHexString(), guid.getHexString(), hexGUID);
    }

    public void testGUIDHashCode() throws Exception {
        GUID guid = new GUID();
        byte[] bguid = guid.getGUIDBytes();
        GUID newguid = new GUID(false);
        newguid.deserialize(bguid, 0);
        int hash1 = guid.hashCode();
        int hash2 = newguid.hashCode();
        //must also check whether two objects are equal (as hashtable does a
        //hashcode AND equals check)
        assertTrue((hash1 == hash2) && guid.equals(newguid));
    }

    /**
     * Test contract requirement that Gnutella GUID byte 8 should be 0xff
     * and byte 15 should be 0x00
     */
    public void testGnutellaGUIDConformance() throws Exception {
        GUID guid = new GUID();
        byte[] bguid = guid.getGUIDBytes();
        if (bguid[8] != (byte) 0xff)
            fail("The 8th byte of GUID should be 0xff");
        if (bguid[15] != (byte) 0x00)
            fail("The 15th byte of GUID should be 0x00");
    }
}
