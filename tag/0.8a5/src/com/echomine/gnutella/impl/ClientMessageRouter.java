package com.echomine.gnutella.impl;

import com.echomine.gnutella.*;

import javax.swing.event.EventListenerList;

/**
 * Receives and routes messages depending on what message is coming in.
 * It first determines the type of message, then sends out the appropriate
 * forwarding or reply message to all connections.  The only time messages that are
 * not sent to all connections are those are replies.  Those messages will
 * only get sent to the returning connection.  Messages that were seen or routed before will get filtered here.
 */
public class ClientMessageRouter implements GnutellaMessageRouter {
    protected MessageRouterController controller;
    protected EventListenerList listenerList = new EventListenerList();

    public ClientMessageRouter(GnutellaContext context, IConnectionList clist) {
        controller = new MessageRouterController(context, clist);
        clist.addMessageListener(this);
    }

    /** add our own message to the model.. this is a convenience method to the message model's method. */
    public void addToOwnMsg(GnutellaMessage msg) {
        controller.addToOwnMsg(msg);
    }

    /** reads each message type and delegates the handling to the message's respective router method. */
    public void messageReceived(GnutellaMessageEvent event) {
        GnutellaMessage gmsg = event.getMessage();
        //drop excessive messages right here right now, no routing
        if (gmsg.getHeader().getTTL() + gmsg.getHeader().getHopsTaken() <= 10)
            gmsg.route(event.getConnection(), controller);
    }
}
