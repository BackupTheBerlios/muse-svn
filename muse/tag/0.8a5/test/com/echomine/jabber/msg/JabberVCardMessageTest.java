package com.echomine.jabber.msg;

import com.echomine.common.ParseException;
import junit.framework.TestCase;

/**
 * tests vcard message
 */
public class JabberVCardMessageTest extends TestCase {
    protected void setUp() throws Exception {
    }

    /**
     * Tests a bug where when the JID is null, null pointer
     * exception is thrown.  This should not be the case.
     */
    public void testJIDNullDoesNotThrowNPE() throws ParseException {
        JabberVCardMessage msg = new JabberVCardMessage();
        msg.setJID(null);
        //the following should not thrown a NPE
        msg.encode();
    }
}
