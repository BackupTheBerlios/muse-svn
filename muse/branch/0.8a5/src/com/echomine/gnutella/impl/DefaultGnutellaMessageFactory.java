package com.echomine.gnutella.impl;

import com.echomine.common.ParseException;
import com.echomine.gnutella.*;

/**
 * <p>Default implementation of the GnutellaMessageFactory.  It uses all the provided default supported classes provided by
 * the API for the required method implementations. <p>To work with custom messages, you just need to override the
 * deserialize() method and create the custom GnutellaMessage that corresponds to the particular message you're parsing for.
 * You should also call super.deserialize() at the beginning/end of the method so that it will also parse the core gnutella
 * messages. <p>You also need to override the AbstractMessageRouter class being used to route the messages so that it knows
 * how to route the custom messages.  Otherwise, you will have problems.
 */
public class DefaultGnutellaMessageFactory implements GnutellaMessageFactory {
    public DefaultGnutellaMessageFactory() {
    }

    /**
     *  Determine function type and returns the proper GnutellaMessage instance.
     * This method is overridden to only instantiate the base messages that comes with the gnutella protocol.  Other message
     * factories can implement their own proprietary message parsing.
     * @return the message object. null if message is an unknown message type.
     */
    public GnutellaMessage deserialize(GnutellaMessageHeader header, byte[] data) throws ParseException {
        GnutellaMessage msg = null;
        //depending on the type, return the proper message instance
        switch (header.getFunction()) {
            case GnutellaCode.PING:
                msg = new MsgInit(header);
                break;
            case GnutellaCode.PONG:
                msg = new MsgInitResponse(header);
                break;
            case GnutellaCode.PUSH_REQUEST:
                msg = new MsgPushRequest(header);
                break;
            case GnutellaCode.QUERY:
                msg = new MsgQuery(header);
                break;
            case GnutellaCode.QUERY_RESPONSE:
                msg = new MsgQueryResponse(header);
                break;
            default:
                msg = new MsgUnknown(header);
                break;
        }
        msg.deserialize(data, 0, header.getDataLen());
        return msg;
    }
}
