package com.echomine.gnutella;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Maintains all data related to messages seen and not seen. It will keep track of all messages and message routing
 */
public class GnutellaMessageModel {
    protected final int MESSAGE_MAX = 10000;
    protected HashMap msgSeen;
    protected HashMap msgOwn;
    protected HashMap msgRoutingTable;
    protected HashMap msgPushRoutingTable;
    protected LinkedList msgLRU;
    protected LinkedList msgOwnLRU;
    protected LinkedList routingLRU;
    protected LinkedList pushRoutingLRU;

    public GnutellaMessageModel() {
        msgSeen = new HashMap(MESSAGE_MAX);
        msgOwn = new HashMap(MESSAGE_MAX);
        msgRoutingTable = new HashMap(MESSAGE_MAX);
        msgPushRoutingTable = new HashMap(MESSAGE_MAX);
        msgLRU = new LinkedList();
        msgOwnLRU = new LinkedList();
        routingLRU = new LinkedList();
        pushRoutingLRU = new LinkedList();
    }

    /**
     * check to see if message exists already.  If not, add it to the hashtable
     * @return true if message has already been seen, false otherwise.
     */
    public synchronized boolean checkAndAddMsgSeen(GnutellaMessage msg) {
        GUID msgID = msg.getHeader().getMsgID();
        synchronized (msgSeen) {
            if (msgSeen.get(msgID) != null) {
                // Seen this msg.
                return true;
            } else {
                msgSeen.put(msgID, msg.getHeader());
                msgLRU.addLast(msgID);
                if (msgLRU.size() > MESSAGE_MAX) {
                    // Too many guids.  Get rid of the oldest one.
                    GUID guid = (GUID) msgLRU.removeFirst();
                    msgSeen.remove(guid);
                }
                return false;
            }
        }
    }

    /** adds the message to the routing table */
    public void addToRoutingTable(GUID msgID, GnutellaConnection sender) {
        synchronized (msgRoutingTable) {
            // Add to routing table.
            msgRoutingTable.put(msgID, sender);
            routingLRU.addLast(msgID);
            if (routingLRU.size() > MESSAGE_MAX) {
                // Too many guids.  Get rid of the oldest one.
                GUID guid = (GUID) routingLRU.removeFirst();
                msgRoutingTable.remove(guid);
            }
        }
    }

    public void addToPushRoutingTable(GUID clientID, GnutellaConnection sender) {
        synchronized (msgPushRoutingTable) {
            // Add the clientID in the search result of the sharing host
            // to routing table.
            msgPushRoutingTable.put(clientID, sender);
            // Push into a FIFO LRU queue to keep track of the age of the object.
            pushRoutingLRU.addLast(clientID);
            if (pushRoutingLRU.size() > MESSAGE_MAX) {
                // Too many guids.  Get rid of the oldest one.
                GUID guid = (GUID) pushRoutingLRU.removeFirst();
                msgPushRoutingTable.remove(guid);
            }
        }
    }

    /** adds messages that are actually sent by us and not sent by other clients */
    public void addToOwnMsg(GnutellaMessage msg) {
        synchronized (msgOwn) {
            msgOwn.put(msg.getHeader().getMsgID(), msg);
            // Push into a FIFO LRU queue to keep track of the age of the object.
            msgOwnLRU.addLast(msg);
            if (msgOwnLRU.size() > MESSAGE_MAX) {
                // Too many guids.  Get rid of the oldest one.
                GnutellaMessage kmsg = (GnutellaMessage) msgOwnLRU.removeFirst();
                msgOwn.remove(kmsg.getHeader().getMsgID());
            }
        }
    }

    public GnutellaConnection getRouting(GUID msgID) {
        synchronized (msgRoutingTable) {
            return (GnutellaConnection) msgRoutingTable.get(msgID);
        }
    }

    public GnutellaConnection getPushRouting(GUID clientID) {
        synchronized (msgPushRoutingTable) {
            return (GnutellaConnection) msgPushRoutingTable.get(clientID);
        }
    }

    public boolean isMsgOwner(GnutellaMessage msg) {
        synchronized (msgOwn) {
            return msgOwn.containsKey(msg.getHeader().getMsgID());
        }
    }
}
