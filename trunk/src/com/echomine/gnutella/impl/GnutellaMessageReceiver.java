package com.echomine.gnutella.impl;

import com.echomine.common.ParseException;
import com.echomine.gnutella.*;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * default message receiver object.  It simply asks the message factory to return an appropriate instance of a
 * gnutella message for use.  If a message is unknown, it is simply ignored.  To process unknown/custom messages,
 * you will need to create your own GnutellaMessageFactory and work with the appropriate message types yourself.
 */
public class GnutellaMessageReceiver extends AbstractMessageReceiver {
    private static Log logger = LogFactory.getLog(GnutellaMessageReceiver.class);
    private static Log pingilogger = LogFactory.getLog("gnutella/msg/incoming/ping");
    private static Log pongilogger = LogFactory.getLog("gnutella/msg/incoming/pong");
    private static Log pushilogger = LogFactory.getLog("gnutella/msg/incoming/push");
    private static Log queryilogger = LogFactory.getLog("gnutella/msg/incoming/query");
    private static Log hitilogger = LogFactory.getLog("gnutella/msg/incoming/hit");
    private static Log unknownilogger = LogFactory.getLog("gnutella/msg/incoming/unknown");
    private GnutellaMessageFactory factory;

    public GnutellaMessageReceiver(GnutellaMessageFactory factory) {
        this.factory = factory;
    }

    /** A default concrete implementation of the message receiver */
    public void receive(GnutellaConnection connection, GnutellaMessageHeader header, byte[] data) {
        try {
            GnutellaMessage msg = factory.deserialize(header, data);
            //unknown message does not get fired and is ignored
            if (msg != null) {
                logIncomingMessage(msg);
                //obtains the PONG message and records its stats before message received
                //is called so that the stats are updated properly
                if (msg.getType() == GnutellaCode.PONG) {
                    AbstractGnutellaConnection aconn = (AbstractGnutellaConnection) connection;
                    aconn.parseStatResponse((MsgInitResponse) msg);
                }
                //fire message received
                fireMessageReceived(connection, msg);
            }
        } catch (ParseException ex) {
            if (logger.isWarnEnabled())
                logger.warn("Parse Exception occurred while deserializing incoming message", ex);
        }
    }

    protected void logIncomingMessage(GnutellaMessage msg) {
        switch (msg.getType()) {
            case GnutellaCode.PING:
                if (pingilogger.isDebugEnabled())
                    pingilogger.debug(msg);
                break;
            case GnutellaCode.PONG:
                if (pongilogger.isDebugEnabled())
                    pongilogger.debug(msg);
                break;
            case GnutellaCode.PUSH_REQUEST:
                if (pushilogger.isDebugEnabled())
                    pushilogger.debug(msg);
                break;
            case GnutellaCode.QUERY:
                if (queryilogger.isDebugEnabled())
                    queryilogger.debug(msg);
                break;
            case GnutellaCode.QUERY_RESPONSE:
                if (hitilogger.isDebugEnabled())
                    hitilogger.debug(msg);
                break;
            default:
                if (unknownilogger.isDebugEnabled())
                    unknownilogger.debug(msg);
        }
    }
}
