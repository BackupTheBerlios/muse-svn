package com.echomine.gnutella;

import com.echomine.common.SendMessageFailedException;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Contains all the necessary objects and information to work with message routing.
 * It also offers methods that simplifies writing message router processing codes in your custom message classes.
 */
public class MessageRouterController {
    private static Log logger = LogFactory.getLog(MessageRouterController.class);
    private static Log plogger = LogFactory.getLog("gnutella/router/packet");
    private GnutellaContext context;
    private IConnectionList clist;
    private GnutellaMessageModel messageModel = new GnutellaMessageModel();

    public MessageRouterController(GnutellaContext context, IConnectionList clist) {
        this.context = context;
        this.clist = clist;
    }

    public GnutellaContext getContext() {
        return context;
    }

    public IConnectionList getConnectionList() {
        return clist;
    }

    public GnutellaMessageModel getMessageModel() {
        return messageModel;
    }

    /**
     * add our own message to the model.. this is a convenience method to the message model's method.
     * this method is usually used when you are sending a message as it will make sure that response
     * messages will be handled correctly.
     */
    public void addToOwnMsg(GnutellaMessage msg) {
        messageModel.addToOwnMsg(msg);
    }

    /**
     * convenience method for GnutellaMessageModel's method.  It will check to see if the message
     * has been seen (ie. duplicate message), and if it has, it will return false.  If it hasn't,
     * it will add the message to the list of seen messages.  This method is used to make sure that
     * duplicate messages aren't routed and processed more than once.
     * @return true is message is seen. false if message is not a duplicate.
     */
    public boolean checkAndAddMsgSeen(GnutellaMessage msg) {
        return messageModel.checkAndAddMsgSeen(msg);
    }

    /**
     * convenience method for GnutellaMessageModel's method. It will check to see
     * if the msg originated from us (ie. we sent the message to begin with).  This is
     * to make sure we don't possibly read message more than once.
     */
    public boolean isMsgOwner(GnutellaMessage msg) {
        return messageModel.isMsgOwner(msg);
    }

    /**
     * routes message back through one connection.  Normally this is used to send
     * a response message back to the connection where the request message originated from.
     * Note that routing messages will decrement the TTL and check first to see if TTL has expired before sending the message.
     * @param connection the connection that the message will be sent through
     * @param msg the message that will be sent
     */
    public void routeMessage(GnutellaConnection connection, GnutellaMessage msg) {
        if (!msg.decTTL()) {
            try {
                connection.send(msg);
                //log message that was routed as a reply
                if (plogger.isDebugEnabled())
                    plogger.debug("[Routed Packet From " + connection.getConnectionModel() + "] " + msg);
            } catch (SendMessageFailedException ex) {
                if (logger.isWarnEnabled())
                    logger.warn("Message Failed to Send back to original connection", ex);
            }
        }
    }

    /**
     * routes message to all connection except the one specified.  Normally this is used to send
     * a messages that does not belong to us or to route messages.
     * Note that routing messages will decrement the TTL and check first to see if TTL has expired before sending the message.
     * @param connection the connection that the message will NOT be sent through
     * @param msg the message that will be sent
     */
    public void routeMessageToAllExcept(GnutellaConnection connection, GnutellaMessage msg) {
        if (!msg.decTTL()) {
            clist.sendMessageToAllExcept(msg, connection);
            //log message that was routed
            if (plogger.isDebugEnabled())
                plogger.debug("[Routed Packet From " + connection.getConnectionModel() + "] " + msg);
        }
    }

    /**
     * convenience method for GnutellaMessageModel's method.  It saves the connection
     * so that when a push request comes through, it'll know where to route it to.  This
     * is mainly used for query responses in such a way where the client asks for a push
     * request after receiving a query response.
     * @param clientID the GUID of the sender who originated the query response.
     * @param sender the connection where the query response was routed from.
     */
    public void enablePushRequestRouting(GUID clientID, GnutellaConnection sender) {
        messageModel.addToPushRoutingTable(clientID, sender);
    }

    /**
     * convenience method for GnutellaMessageModel's method.  It retrieves the connection
     * from which the push request is going to.  If the connection doesn't exist, then it
     * means we didn't route the query responses to begin with so we can safely ignore routing this message.
     * @param clientID the GUID of the sender who sent out the query response
     * @return the connection from which to send the push request to.  Null if query response didn't go through us.
     */
    public GnutellaConnection getPushRequestRoute(GUID clientID) {
        return messageModel.getPushRouting(clientID);
    }

    /**
     * convenience method for GnutellaMessageModel's method.  It saves the connection
     * so that when a message response comes back, it'll know where to route it back to.  This
     * is mainly used for message requests that is expecting some sort of response back.
     * Most of the message requests will be using this method if routing is required.
     * @param msg the message to store for routing
     * @param sender the connection where the message came from.
     */
    public void enableMessageRouting(GnutellaMessage msg, GnutellaConnection sender) {
        messageModel.addToRoutingTable(msg.getHeader().getMsgID(), sender);
    }

    /**
     * convenience method for GnutellaMessageModel's method.  It retrieves the connection
     * from which the message request came from.  If the connection doesn't exist, then it
     * means we didn't route the query responses to begin with so we can safely ignore routing
     * this message.  Normally this is used for message response routing since message responses
     * will usually use the message requests' message GUID for identification.
     * @param msg the msg that contains the client who sent out the query response
     * @return the connection from which to send the push request to.  Null if query response didn't go through us.
     */
    public GnutellaConnection getMessageRouting(GnutellaMessage msg) {
        return messageModel.getRouting(msg.getHeader().getMsgID());
    }
}
