package com.echomine.gnutella;

import junit.framework.TestCase;
import com.echomine.gnutella.*;

public class GnutellaMessageModelTest extends TestCase {
    public GnutellaMessageModelTest(String name) {
        super(name);
    }

    public void testAddOwnMessage() throws Exception {
        GnutellaMessageModel model = new GnutellaMessageModel();
        MsgInit msg = new MsgInit();
        model.addToOwnMsg(msg);
        assertTrue("The message should be equal/true", model.isMsgOwner(msg));
        MsgInitResponse pong = new MsgInitResponse(new GnutellaMessageHeader(GnutellaCode.PONG, msg.getHeader().getMsgID()));
        assertTrue("The message should be equal/true", model.isMsgOwner(pong));
    }
}
