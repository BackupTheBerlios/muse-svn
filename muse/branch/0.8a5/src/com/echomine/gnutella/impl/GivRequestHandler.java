package com.echomine.gnutella.impl;

import com.echomine.gnutella.*;
import com.echomine.net.ConnectionEvent;
import com.echomine.net.ConnectionListener;
import com.echomine.net.ConnectionVetoException;
import com.echomine.util.HTTPResponseHeader;
import org.apache.oro.text.perl.Perl5Util;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;

/**
 * Handles all GIV (push) incoming requests.  It will check to see if the GIV request is a valid request that we actually
 * sent.  If it is, then the firewalled download request continues.  Otherwise, the request is rejected.
 */
public class GivRequestHandler implements ListenerRequestHandler {
    protected static final byte[] forbiddenHeader = new HTTPResponseHeader(404, "Forbidden").toString().getBytes();
    protected static final byte[] invalidRequestHeader = new HTTPResponseHeader(400, "Invalid GIV Request").toString().getBytes();
    private Perl5Util givRequestRE = new Perl5Util();
    private HashMap downloads;
    private GnutellaFileListener fl;
    private ConnectionListener cl;
    private GnutellaContext context;

    /**
     * @param fl a catch-all file listener that gets all file transfer events.  It's basically used for a download service
     * that catches all file transfer events so that it can fire off to others
     * @param cl a catch-all connection listener that gets all connection events.  It's basically used for a download service
     * that catches all connection events so that it can fire off to others
     */
    public GivRequestHandler(GnutellaContext context, ConnectionListener cl, GnutellaFileListener fl) {
        this.fl = fl;
        this.cl = cl;
        this.context = context;
        downloads = new HashMap(10);
    }

    /**
     * handles the main GIV request.  Notice that closing the socket is voluntary
     * as the Listener Router will close the socket for you.
     */
    public void handleRequest(String request, Socket socket) throws IOException {
        //if host is restricted, then simply reject and close connection
        if (context.getRestrictedHostCallback().isHostRestricted(socket.getInetAddress())) {
            socket.getOutputStream().write(forbiddenHeader);
            return;
        }
        //obtain information from the request
        if (!givRequestRE.match("m#^GIV (\\d+):(.+)/(.+)#", request)) {
            socket.getOutputStream().write(invalidRequestHeader);
            return;
        }
        GnutellaFirewalledDownloadInfo info = (GnutellaFirewalledDownloadInfo) downloads.remove(givRequestRE.group(2) + ":/" +
                givRequestRE.group(1) + "/" + givRequestRE.group(3));
        if (info == null) {
            socket.getOutputStream().write(invalidRequestHeader);
            return;
        }
        //hand it over to the firewalled download file handler for processing from here
        GnutellaConnectionModel cmodel = new GnutellaConnectionModel(socket.getInetAddress(), socket.getPort(),
                GnutellaConnectionModel.INCOMING);
        ConnectionEvent cStartingEvent = new ConnectionEvent(cmodel, ConnectionEvent.CONNECTION_STARTING);
        ConnectionEvent cOpenedEvent = new ConnectionEvent(cmodel, ConnectionEvent.CONNECTION_OPENED);
        ConnectionEvent cClosedEvent = new ConnectionEvent(cmodel, ConnectionEvent.CONNECTION_CLOSED);
        ConnectionEvent cVetoEvent = new ConnectionEvent(cmodel, ConnectionEvent.CONNECTION_VETOED);
        info.getFileModel().setConnectionModel(cmodel);
        GnutellaFirewalledDownloadHandler firewallHandler = new GnutellaFirewalledDownloadHandler(context, info.getFileModel());
        try {
            firewallHandler.start();
            firewallHandler.addFileListener(fl);
            if (info.getFileListener() != null)
                firewallHandler.addFileListener(info.getFileListener());
            try {
                if (info.getConnectionListener() != null) {
                    info.getConnectionListener().connectionStarting(cStartingEvent);
                    info.getConnectionListener().connectionEstablished(cOpenedEvent);
                }
            } catch (ConnectionVetoException ex) {
                info.getConnectionListener().connectionClosed(cVetoEvent);
                return;
            }
            try {
                cl.connectionStarting(cStartingEvent);
                cl.connectionEstablished(cOpenedEvent);
            } catch (ConnectionVetoException ex) {
                if (info.getConnectionListener() != null)
                    info.getConnectionListener().connectionClosed(cVetoEvent);
                cl.connectionClosed(cVetoEvent);
                return;
            }
            firewallHandler.handle(socket);
            cl.connectionClosed(cClosedEvent);
            if (info.getConnectionListener() != null)
                info.getConnectionListener().connectionClosed(cClosedEvent);
        } finally {
            firewallHandler.removeFileListener(fl);
            if (info.getFileListener() != null)
                firewallHandler.removeFileListener(info.getFileListener());
        }
    }

    /** can only handle GIV requests */
    public boolean canHandle(String request) {
        if (request.startsWith("GIV")) return true;
        return false;
    }

    /** Add to the list of firewalled download that we are waiting for and allowed to receive. */
    public void addRequest(GnutellaFirewalledDownloadInfo info) {
        //the hash key format is arbitrarily set to: <host ip>:/<file index>/<filename>
        //System.out.println("Added request: " + info.remoteClientID + ":/" + info.filemodel.getFileIndex() + "/" + info.filemodel.getFilename());
        downloads.put(info.getRemoteClientID() + ":/" + info.getFileModel().getFileIndex() + "/" + info.getFileModel().getFilename(), info);
    }

    /** remove a firewalled download request that was originally added here */
    public void removeRequest(GnutellaFirewalledDownloadInfo info) {
        downloads.remove(info);
    }

    /** clears the entire firewalled download requests */
    public void clearRequests() {
        downloads.clear();
    }

    /** checks if the specified download request exists in the queue */
    public boolean requestExists(GnutellaFirewalledDownloadInfo info) {
        if (downloads.containsKey(info.getRemoteClientID() + ":/" + info.getFileModel().getFileIndex() + "/" +
                info.getFileModel().getFilename()))
            return true;
        else
            return false;
    }
}
