package com.echomine.gnutella;

import com.echomine.common.SendMessageFailedException;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** Essentially a PING message. */
public class MsgInit extends GnutellaMessage {
    public MsgInit() {
        this(new GnutellaMessageHeader(GnutellaCode.PING));
    }

    public MsgInit(GnutellaMessageHeader header) {
        super(header);
    }

    /**
     * handles ping messages.
     */
    public void route(GnutellaConnection connection, MessageRouterController controller) {
        // See if I have seen this Init before.  Drop msg if duplicate.
        if (controller.checkAndAddMsgSeen(this) || controller.isMsgOwner(this))
            return;
        // Add the Init msg to the routing table so that I know where
        // to route the InitResponse back.
        controller.enableMessageRouting(this, connection);
        // if TTL didn't expire, send message
        controller.routeMessageToAllExcept(connection, this);
        // Construct InitResponse msg.  Copy the original Init's GUID.
        GnutellaMessageHeader newHeader = new GnutellaMessageHeader(GnutellaCode.PONG, getHeader().getMsgID());
        MsgInitResponse response = new MsgInitResponse(newHeader);
        newHeader.setTTL(getHeader().getHopsTaken() + 1); // Will take as many hops to get back.
        response.setPort(controller.getContext().getPort());
        try {
            InetAddress ip = controller.getContext().getInterfaceIP();
            if (ip == null)
                ip = InetAddress.getLocalHost();
            response.setIP(ip);
            response.setFileCount(controller.getContext().getShareFileController().getFileCount());
            response.setTotalSize(controller.getContext().getShareFileController().getTotalSize());
            connection.send(response);
        } catch (UnknownHostException ex) {
        } catch (SendMessageFailedException ex) {
        }
    }
}
