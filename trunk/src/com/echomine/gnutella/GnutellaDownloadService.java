package com.echomine.gnutella;

import com.echomine.gnutella.impl.GivRequestHandler;
import com.echomine.gnutella.impl.GnutellaDirectDownloadHandler;
import com.echomine.net.*;
import com.echomine.util.IOUtil;
import com.echomine.util.IPUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Manages all download related activities.  The class also contains a request handler that handles GIV/push requests.  Since
 * it's a firewalled download, it's considered to be part of the responsibility of the download service.
 */
public class GnutellaDownloadService extends Connection {
    private GnutellaConnectionManager cmanager;
    private GnutellaContext context;
    private DownloadConnectionListener clistener;
    private DownloadFileListener flistener;
    private GivRequestHandler givHandler;

    public GnutellaDownloadService(GnutellaContext context, GnutellaConnectionManager cmanager, GnutellaListenerRouter listenerRouter) {
        this.cmanager = cmanager;
        this.context = context;
        //internal private listeners available only for the GIV Request Handler
        clistener = new DownloadConnectionListener();
        flistener = new DownloadFileListener();
        givHandler = new GivRequestHandler(context, clistener, flistener);
        listenerRouter.addRequestHandler(givHandler);
    }

    /**
     * <p>To get a file, it simply create a handler that connects with the remote client.  It will automatically determine if
     * the remote client should be handled through a firewall (PUSH) request or just a direct connect.</p>
     * @param guid GUID is unique ID for the remote server you're getting the file from
     * @param IP ip of remote connection
     * @param port port the remote is listening on
     * @param filemodel must contain filename and file index (and optionally save location and others)
     * @param cl ConnectionListener that will listen for connection events. null if not listening.
     * @param fl FileListener that will listen for file transfer events. null if not listening.
     * @return GnutellaDownloadInfo an object that encapsulates all the passed in information
     */
    public GnutellaDownloadInfo getFile(GUID guid, String IP, int port, GnutellaFileModel filemodel, ConnectionListener cl,
                                        GnutellaFileListener fl) throws UnknownHostException {
        GnutellaDownloadInfo info = null;
        //determine if IP is private or if port is 0
        //these indicate that a push request is required
        if (port == 0 || IPUtil.isHostIPPrivate(IP)) {
            info = getFileThroughFirewall(guid, filemodel, cl, fl);
        } else {
            GnutellaConnectionModel cmodel = new GnutellaConnectionModel(IP, port, GnutellaConnectionModel.INCOMING);
            info = new GnutellaDownloadInfo(cmodel, filemodel, cl, fl);
            filemodel.setConnectionModel(cmodel);
            GnutellaDirectDownloadHandler handler = new GnutellaDirectDownloadHandler(context, filemodel);
            DirectDownloadConnector connector = new DirectDownloadConnector(handler, guid, filemodel, cl, fl);
            //add listeners
            connector.addConnectionListener(clistener);
            handler.addFileListener(flistener);
            if (info.cl != null)
                connector.addConnectionListener(cl);
            if (info.fl != null)
                handler.addFileListener(fl);
            //connect asynchronously
            connector.aconnect(cmodel);
        }
        return info;
    }

    /**
     * <p>Request to get a file by asking the remote client to connect to us and send us the file.  This is used when you know
     * the remote is behind a firewall and you are not.  Technically this is automatically handled if you use the
     * normal getFile() method. If you use this method, a PUSH request will be sent across the network.</p>
     * <p>Notice that this method doesn't accept an IP and port like getFile() does.  The reason is because
     * the push request message requires OUR IP and port, not the remote IP/port.  Thus, that information is
     * retrieved automatically from the connection manager.</p>
     * @param guid the unique remote client ID that will connect to us
     * @param filemodel contains the file save, file name, file resume, etc info
     * @param cl connection listener for this download
     * @param fl file transfer listener for this download
     * @return instance of GnutellaDownloadInfo that encapsulates all this info
     */
    public GnutellaDownloadInfo getFileThroughFirewall(GUID guid, GnutellaFileModel filemodel, ConnectionListener cl,
                                                       GnutellaFileListener fl) throws UnknownHostException {
        InetAddress ip = context.getInterfaceIP();
        if (ip == null)
            ip = InetAddress.getLocalHost();
        //add the file to the download
        GnutellaFirewalledDownloadInfo info = new
            GnutellaFirewalledDownloadInfo(
                new GnutellaConnectionModel(ip, context.getPort()),
                filemodel, cl, fl, guid);
        givHandler.addRequest(info);
        //send messsage to remote for firewalled download
        MsgPushRequest msg = new MsgPushRequest(guid, ip, context.getPort(),
            filemodel.getFileIndex());
        cmanager.send(msg);
        return info;
    }

    /** add to listen for all file transfer events for all downloads. */
    public void addFileListener(GnutellaFileListener l) {
        flistener.addFileListener(l);
    }

    /** remove from listening to all file transfer events for all downloads. */
    public void removeFileListener(GnutellaFileListener l) {
        flistener.removeFileListener(l);
    }

    /**
     * default connection listener that simply fires all the events to
     * the event listeners registered with the download service
     */
    class DownloadConnectionListener implements ConnectionListener {
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


    /**
     * default connection listener that simply fires all the events to
     * the event listeners registered with the download service
     */
    class DownloadFileListener extends BaseFileHandler implements GnutellaFileListener {
        /** stub implementation */
        public FileModel getModel() {
            return null;
        }

        /** stub implementation */
        public void start() {
        }

        /** stub implementation */
        public void handle(Socket socket) throws IOException {
        }

        /** stub implementation */
        public TransferRateThrottler getTransferRateThrottler() {
            return null;
        }

        /** stub implementation */
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

    /**
     * The direct download connector simply adds a push request if connnection
     * fails, it will retry with a push request
     */
    class DirectDownloadConnector extends SocketConnector {
        GUID guid;
        GnutellaFileModel filemodel;
        ConnectionListener cl;
        GnutellaFileListener fl;

        public DirectDownloadConnector(SocketHandler socketHandler, GUID guid,
                                       GnutellaFileModel filemodel,
                                       ConnectionListener cl, GnutellaFileListener fl) {
            super(socketHandler);
            this.guid = guid;
            this.filemodel = filemodel;
            this.cl = cl;
            this.fl = fl;
        }

        public void connect(SocketHandler socketHandler, ConnectionModel connectionModel) throws ConnectionFailedException {
            try {
                super.connect(socketHandler, connectionModel);
            } catch (ConnectionFailedException ex) {
                try {
                    getFileThroughFirewall(guid, filemodel, cl, fl);
                } catch (UnknownHostException ex1) {
                }
                throw ex;
            }
        }

        public void aconnect(final SocketHandler socketHandler, final ConnectionModel connectionModel) {
            Thread thread = new Thread(
                new Runnable() {
                    public void run() {
                        Socket socket = null;
                        try {
                            ConnectionEvent event = new ConnectionEvent(connectionModel, ConnectionEvent.CONNECTION_STARTING);
                            ConnectionEvent vetoEvent = new ConnectionEvent(connectionModel, ConnectionEvent.CONNECTION_VETOED);
                            socketHandler.start();
                            DirectDownloadConnector.this.fireConnectionStarting(event, vetoEvent);
                            socket = new Socket(connectionModel.getHost(), connectionModel.getPort());
                            try {
                                event = new ConnectionEvent(connectionModel, ConnectionEvent.CONNECTION_OPENED);
                                DirectDownloadConnector.this.fireConnectionEstablished(event);
                                socketHandler.handle(socket);
                                event = new ConnectionEvent(connectionModel, ConnectionEvent.CONNECTION_CLOSED);
                                DirectDownloadConnector.this.fireConnectionClosed(event);
                            } catch (IOException ex) {
                                event = new ConnectionEvent(connectionModel, ConnectionEvent.CONNECTION_ERRORED, "Error while handling connection: " + ex.getMessage());
                                DirectDownloadConnector.this.fireConnectionClosed(event);
                            } finally {
                                IOUtil.closeSocket(socket);
                            }
                        } catch (IOException ex) {
                            try {
                                getFileThroughFirewall(guid, filemodel, cl, fl);
                            } catch (UnknownHostException ex1) {
                            }
                            ConnectionEvent event = new ConnectionEvent(connectionModel, ConnectionEvent.CONNECTION_ERRORED, "Error..." + ex.getMessage());
                            DirectDownloadConnector.this.fireConnectionClosed(event);
                        } catch (ConnectionVetoException ex) {
                            //do nothing, connection closed event already fired
                        }
                    }
                });
            thread.start();
        }
    }
}
