package com.echomine.gnutella.impl;

import alt.java.net.Socket;
import com.echomine.gnutella.*;
import com.echomine.net.ConnectionEvent;
import com.echomine.net.ConnectionListener;
import com.echomine.net.ConnectionVetoException;
import com.echomine.util.HTTPResponseHeader;
import org.apache.oro.text.perl.Perl5Util;

import java.io.IOException;

/**
 * Handles all incoming GET requests.  Once a get request is obtained, the handler will make sure the file is shared.  If
 * users want to share files, they will need to implement their own sharing algorithm.  Look at the
 * ShareFileController on how to share files.
 */
public class GetRequestHandler implements ListenerRequestHandler {
    protected static final byte[] forbiddenHeader = new HTTPResponseHeader(404, "Forbidden").toString().getBytes();
    protected static final byte[] invalidRequestHeader = new HTTPResponseHeader(400, "Invalid GET Request").toString().getBytes();
    protected static final byte[] fileNotSharedRequestHeader = new HTTPResponseHeader(400, "File Not Shared").toString().getBytes();
    protected HTTPResponseHeader busyHeader = new HTTPResponseHeader(503, "Server Busy");
    private Perl5Util getRequestRE = new Perl5Util();
    private GnutellaFileListener fl;
    private ConnectionListener cl;
    private ShareInfo info;
    private GnutellaContext context;

    public GetRequestHandler(GnutellaContext context, ShareInfo info) {
        this.cl = info.getConnectionListener();
        this.fl = info.getFileListener();
        this.info = info;
        this.context = context;
    }

    /**
     * checks that the file requested exists and is shared.  If it is, then it
     * creates a upload file handler and hands it over to the file handler to do
     * its job.  Note that since this is an upload request, the first GET request
     * is already read by us.  This means the file handlers should only read the
     * headers and not look for the GET request line as it's already been parsed
     * here.  Handlers can access the request information through the FileModel.
     */
    public void handleRequest(String request, Socket socket) throws IOException {
        //if host is restricted, then simply reject and close connection
        if (context.getRestrictedHostCallback().isHostRestricted(socket.getInetAddress())) {
            socket.getOutputStream().write(forbiddenHeader);
            return;
        }
        //check first to make sure max uploads is not reached
        if (!info.incrementCurrentUploads()) {
            //over limit
            //send server busy
            busyHeader.setHeader("Server", context.getSupportedFeatureHeaders().getHeader("User-Agent"));
            socket.getOutputStream().write(busyHeader.toString().getBytes());
            return;
        }
        //gnutella get request format:
        //GET /get/<file idx>/<filename>/ HTTP/1.0
        //the last uri slash is optional
        if (!getRequestRE.match("m#^GET\\s+/get/(\\d+)/(.+)/?\\s+(.+)#", request)) {
            socket.getOutputStream().write(invalidRequestHeader);
            return;
        }
        int fileidx = Integer.parseInt(getRequestRE.group(1));
        //validate that the requested file is shared
        //by getting the real file path
        String path = info.getShareFileController().getFilePath(fileidx);
        String filename = getRequestRE.group(2);
        if (path == null || filename == null) {
            //file isn't share
            socket.getOutputStream().write(fileNotSharedRequestHeader);
            return;
        }
        //hand it over to the firewalled download file handler for processing from here
        GnutellaConnectionModel cmodel = new GnutellaConnectionModel(socket.getInetAddress(), socket.getPort(),
                GnutellaConnectionModel.OUTGOING);
        ConnectionEvent cStartingEvent = new ConnectionEvent(cmodel, ConnectionEvent.CONNECTION_STARTING);
        ConnectionEvent cOpenedEvent = new ConnectionEvent(cmodel, ConnectionEvent.CONNECTION_OPENED);
        ConnectionEvent cClosedEvent = new ConnectionEvent(cmodel, ConnectionEvent.CONNECTION_CLOSED);
        ConnectionEvent cVetoEvent = new ConnectionEvent(cmodel, ConnectionEvent.CONNECTION_VETOED);
        GnutellaFileModel filemodel = new GnutellaFileModel(filename, path, fileidx, cmodel);
        GnutellaDirectUploadHandler uploadHandler =
                new GnutellaDirectUploadHandler(context, filemodel);
        uploadHandler.addFileListener(fl);
        //simulate client connection events
        try {
            uploadHandler.start();
            if (cl != null) {
                cl.connectionStarting(cStartingEvent);
                cl.connectionEstablished(cOpenedEvent);
            }
            uploadHandler.handle(socket);
            if (cl != null)
                cl.connectionClosed(cClosedEvent);
        } catch (ConnectionVetoException ex) {
            if (cl != null)
                cl.connectionClosed(cVetoEvent);
        }
        info.decrementCurrentUploads();
    }

    /** Only handle GET requests */
    public boolean canHandle(String request) {
        if (request.startsWith("GET")) return true;
        return false;
    }
}
