package com.echomine.gnutella;

import com.echomine.common.SendMessageFailedException;
import com.echomine.gnutella.impl.GetRequestHandler;
import com.echomine.gnutella.impl.GnutellaFirewalledUploadHandler;
import com.echomine.net.*;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Collection;
import java.util.Iterator;

/**
 * <p>Controls all access to the sharing of files across the network.  Sharing of files across gnutella is actually fairly
 * simple.  When a query is received, the share service passes the query to the ShareFileController to retrieve a list of
 * files.  Notice that Share Service does not do any work regarding storing/indexing of files being shared.  Those are all
 * delegated to the ShareFileController.</p>
 * <p><b>NOTE:</b> The file transfer listener for the share service MUST NOT be the same as the ones for downloads.  The
 * reason is because the FileModel's save location is used to locate the ACTUAL file to be uploaded.  In download file
 * transfer listeners, the save location is where the file should be saved.  Thus, if in your file transfer starting event
 * handling method, you change the save location, you are essentially changing it for different purposes!  So to avoid
 * confusion, just use DIFFERNT file listeners for the Sharing Service</p>
 */
public class GnutellaShareService extends Connection {
    private GnutellaContext context;
    private GetRequestHandler getRequestHandler;
    private ShareInfo info;
    private ShareFileListener fl;

    public GnutellaShareService(GnutellaContext context, GnutellaConnectionManager cmanager, GnutellaListenerRouter listenerRouter) {
        this.context = context;
        cmanager.addMessageListener(new SearchMessageListener());
        cmanager.addMessageListener(new FirewalledUploadMessageListener());
        ShareConnectionListener cl = new ShareConnectionListener();
        fl = new ShareFileListener();
        info = new ShareInfo(context.getShareFileController(), cl, fl);
        getRequestHandler = new GetRequestHandler(context, info);
        listenerRouter.addRequestHandler(getRequestHandler);
    }

    /**
     * subscribe to listen to all file transfer events that are going on.  You can also listen to one specific transfer, but
     * that is only available to the one who initiates the file transfer.
     */
    public void addFileListener(GnutellaFileListener l) {
        fl.addFileListener(l);
    }

    public void removeFileListener(GnutellaFileListener l) {
        fl.removeFileListener(l);
    }

    public int getCurrentUploads() {
        return info.getCurrentUploads();
    }

    class FirewalledUploadMessageListener implements GnutellaMessageListener {
        public void messageReceived(GnutellaMessageEvent event) {
            //only interested in push requests
            GnutellaMessage msg = event.getMessage();
            if (msg.getType() == GnutellaCode.PUSH_REQUEST) {
                MsgPushRequest push = (MsgPushRequest) msg;
                //check if the file is shared or not
                int fileidx = push.getFileIndex();
                String path = info.getShareFileController().getFilePath(fileidx);
                String filename = info.getShareFileController().getFilename(fileidx);
                //no file exists, just ignore request
                if (path == null || filename == null) return;
                //increment current uploads
                //if max uploads reached, then just ignore
                if (!info.incrementCurrentUploads()) return;
                //start firewalled upload
                //create the proper models to connect to the remote host
                //the file model used by firewalled uploading is a little different than the rest
                //the save location is the REAL path location for the file being uploaded
                //while the filename is the name published to the outside
                GnutellaFileModel filemodel = new GnutellaFileModel(filename, path, fileidx);
                //get the filesize
                File file = new File(path);
                if (!file.exists()) return;
                filemodel.setFilesize(file.length());
                GnutellaConnectionModel cmodel = new GnutellaConnectionModel(push.getRemoteHost(), push.getRemotePort(), GnutellaConnectionModel.OUTGOING);
                filemodel.setConnectionModel(cmodel);
                GnutellaFirewalledUploadHandler handler =
                        new GnutellaFirewalledUploadHandler(context, filemodel, context.getClientID());
                SocketConnector connector = new SocketConnector(handler);
                handler.addFileListener(info.getFileListener());
                //due to an asynchronous connect, the decrement of current uploads must be
                //done inside the listener
                connector.addConnectionListener(
                        new ShareConnectionListener() {
                            public void connectionClosed(ConnectionEvent e) {
                                //decrement the current uploads
                                info.decrementCurrentUploads();
                                super.connectionClosed(e);
                            }
                        });
                connector.aconnect(cmodel);
            }
        }
    }


    class SearchMessageListener implements GnutellaMessageListener {
        public void messageReceived(GnutellaMessageEvent event) {
            //get the query
            GnutellaMessage msg = event.getMessage();
            if (msg.getType() == GnutellaCode.QUERY) {
                MsgQuery query = (MsgQuery) msg;
                //submit the query to the share file controller
                Collection files = info.getShareFileController().getFiles(query);
                //no files match the query
                if (files == null) return;
                //create header that corresponds with the query msg ID
                GnutellaMessageHeader newHeader = new GnutellaMessageHeader(GnutellaCode.QUERY_RESPONSE,
                        msg.getHeader().getMsgID());
                newHeader.setTTL(msg.getHeader().getHopsTaken() + 1); // Will take as many hops to get back.
                MsgQueryResponse response = new MsgQueryResponse(newHeader, context);
                Iterator iter = files.iterator();
                //loop through and add all the files that are shared
                //notice that currently gnutella can only have up to 255
                //response records associated with each response because
                //of the usage of 1 byte to indicate the number of records
                try {
                    while (iter.hasNext()) {
                        response.addMsgRecord((MsgResRecord) iter.next());
                        //if num records has reached 255, create new response
                        if (response.getNumRecords() >= 255 && iter.hasNext()) {
                            //send out the old one first
                            event.getConnection().send(response);
                            //create new response
                            response = new MsgQueryResponse(newHeader, context);
                        }
                    }
                    //send the response
                    event.getConnection().send(response);
                } catch (SendMessageFailedException ex) {
                    //unable to send response message back.
                    //oh well!
                }
            }
        }
    }


    class ShareFileListener extends BaseFileHandler implements GnutellaFileListener {
        /** stub implementation just to get our behavior */
        public FileModel getModel() {
            return null;
        }

        /** stub implementation just to get our behavior */
        public void start() {
        }

        /** stub implementation just to get our behavior */
        public void handle(Socket socket) throws IOException {
        }

        /** stub implementation just to get our behavior */
        public TransferRateThrottler getTransferRateThrottler() {
            return null;
        }

        /** stub implementation just to get our behavior */
        public void shutdown() {
        }

        public void filesizeChanged(FileEvent e) {
            fireFilesizeChanged(e);
        }

        public void fileInfoChanged(FileEvent e) {
            fireFileInfoChanged(e);
        }

        public void fileTransferFinished(FileEvent e) {
            fireFileTransferFinished(e);
        }

        public void fileTransferStarting(FileEvent e) throws TransferVetoException {
            fireFileTransferStartingWithoutVeto(e);
        }
    }


    class ShareConnectionListener implements ConnectionListener {
        public void connectionStarting(ConnectionEvent e) throws ConnectionVetoException {
            fireConnectionStartingWithoutVeto(e);
        }

        public void connectionEstablished(ConnectionEvent e) {
            fireConnectionEstablished(e);
        }

        public void connectionClosed(ConnectionEvent e) {
            fireConnectionClosed(e);
        }
    }
}
