package com.echomine.xmpp;

import java.io.Reader;
import java.io.StringReader;

import com.echomine.XMPPTestCase;

/**
 * Tests the iq privacy packet and its associated functions
 */
public class IQPrivacyTest extends XMPPTestCase {
    public void testMarshallPrivacyListRequest() throws Exception {
        String xml = "<query xmlns='jabber:iq:privacy'/>";
        StringReader rdr = new StringReader(xml);
        IQPrivacyPacket packet = new IQPrivacyPacket();
        marshallObject(packet, IQPrivacyPacket.class);
        compare(rdr);
    }

    /**
     * When setting the default name to null after setting name to something
     * else, the marshalled xml must not include an empty "default" element.
     */
    public void testMarshallPrivacyListAfterSecondTimeSettingToNull() throws Exception {
        String xml = "<query xmlns='jabber:iq:privacy'/>";
        StringReader rdr = new StringReader(xml);
        IQPrivacyPacket packet = new IQPrivacyPacket();
        packet.setDefaultName("test");
        packet.setDefaultName(null);
        packet.setActiveName("test");
        packet.setActiveName(null);
        marshallObject(packet, IQPrivacyPacket.class);
        compare(rdr);
    }

    /**
     * Tests the setting for removing a default name
     */
    public void testMarshallDefaultNameRemove() throws Exception {
        String xml = "<query xmlns='jabber:iq:privacy'><default/></query>";
        StringReader rdr = new StringReader(xml);
        IQPrivacyPacket packet = new IQPrivacyPacket();
        packet.setDefaultName("");
        assertNull(packet.getDefaultName());
        marshallObject(packet, IQPrivacyPacket.class);
        compare(rdr);
    }

    /**
     * Tests the setting for removing an active name
     */
    public void testMarshallActiveNameRemove() throws Exception {
        String xml = "<query xmlns='jabber:iq:privacy'><active/></query>";
        StringReader rdr = new StringReader(xml);
        IQPrivacyPacket packet = new IQPrivacyPacket();
        packet.setActiveName("");
        assertNull(packet.getDefaultName());
        marshallObject(packet, IQPrivacyPacket.class);
        compare(rdr);
    }

    public void testMarshallListRequest() throws Exception {
        String xml = "<query xmlns='jabber:iq:privacy'><list name='public'/></query>";
        StringReader rdr = new StringReader(xml);
        IQPrivacyPacket packet = new IQPrivacyPacket();
        packet.addPrivacyList(new PrivacyList("public"));
        assertNull(packet.getDefaultName());
        marshallObject(packet, IQPrivacyPacket.class);
        compare(rdr);
    }

    public void testMarshallDenyIQRequest() throws Exception {
        String inRes = "com/echomine/xmpp/data/PrivacySetDenyIQ.xml";
        Reader rdr = getResourceAsReader(inRes);
        IQPrivacyPacket packet = new IQPrivacyPacket();
        PrivacyItem item = new PrivacyItem();
        item.setDenyIQ(true);
        item.setOrder(6);
        PrivacyList list = new PrivacyList("iq-global-example");
        list.addItem(item);
        packet.addPrivacyList(list);
        marshallObject(packet, IQPrivacyPacket.class);
        compare(rdr);
    }

    public void testUnmarshallPrivacyListWithItemResult() throws Exception {
        String inRes = "com/echomine/xmpp/data/PrivacyListWithItemResult.xml";
        Reader rdr = getResourceAsReader(inRes);
        IQPrivacyPacket packet = (IQPrivacyPacket) unmarshallObject(rdr, IQPrivacyPacket.class);
        assertNull(packet.getActiveName());
        assertNull(packet.getDefaultName());
        assertEquals(1, packet.getPrivacyLists().size());
        PrivacyList list = packet.getPrivacyList(0);
        assertEquals(2, list.getItems().size());
        PrivacyItem item = list.getItem(0);
        assertEquals(PrivacyItem.TYPE_JID, item.getType());
        assertEquals("tybalt@example.com", item.getValue());
        assertFalse(item.isAllow());
        assertEquals(1, item.getOrder());
        item = list.getItem(1);
        assertTrue(item.isAllow());
        assertNull(item.getType());
        assertNull(item.getValue());
        assertEquals(2, item.getOrder());
    }

    public void testUnmarshallPrivacyListResult() throws Exception {
        String inRes = "com/echomine/xmpp/data/PrivacyListResult.xml";
        Reader rdr = getResourceAsReader(inRes);
        IQPrivacyPacket packet = (IQPrivacyPacket) unmarshallObject(rdr, IQPrivacyPacket.class);
        assertEquals("private", packet.getActiveName());
        assertEquals("public", packet.getDefaultName());
        assertEquals(3, packet.getPrivacyLists().size());
        assertEquals("public", ((PrivacyList) packet.getPrivacyLists().get(0)).getName());
        assertEquals("private", ((PrivacyList) packet.getPrivacyLists().get(1)).getName());
        assertEquals("special", ((PrivacyList) packet.getPrivacyLists().get(2)).getName());
    }

    
}
