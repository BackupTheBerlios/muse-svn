package com.echomine.gnutella.impl;

import alt.java.net.Socket;
import com.echomine.common.ParseException;
import com.echomine.gnutella.*;
import com.echomine.net.ConnectionModel;
import com.echomine.net.ConnectionThrottler;
import com.echomine.net.HandshakeFailedException;
import com.echomine.util.HTTPHeader;
import com.echomine.util.HTTPResponseHeader;
import com.echomine.util.IOUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * The generic version protocol implementation of gnutella.  The default doesn't do any handshaking, thus it is essentially a
 * connection handler since the subclasses will take care of the handshaking.  Most of the Gnutella protocol implementations
 * should subclass from this.
 */
abstract public class AbstractGnutellaProtocol implements GnutellaProtocolSocketHandler {
    private static Log logger = LogFactory.getLog(AbstractGnutellaProtocol.class);
    private static Log hlogger = LogFactory.getLog("gnutella/protocol/handshake");
    private static Log pingologger = LogFactory.getLog("gnutella/msg/outgoing/ping");
    private static Log pongologger = LogFactory.getLog("gnutella/msg/outgoing/pong");
    private static Log pushologger = LogFactory.getLog("gnutella/msg/outgoing/push");
    private static Log queryologger = LogFactory.getLog("gnutella/msg/outgoing/query");
    private static Log hitologger = LogFactory.getLog("gnutella/msg/outgoing/hit");
    private static Log unknownologger = LogFactory.getLog("gnutella/msg/outgoing/unknown");
    protected final static int SOCKETBUF = 8192;
    boolean shutdown;
    RawDataReceivable receiver;
    GnutellaConnection connection;
    IncomingReaderThread reader;
    HTTPHeader remoteVendorFeatures;
    HTTPHeader remoteSupportedFeatures;
    protected MessageRequestQueue queue = new MessageRequestQueue();
    Socket socket;

    /**
     * @param connection GnutellaConnection, cannot be null
     * @param receiver cannot be null
     */
    public AbstractGnutellaProtocol(GnutellaConnection connection, RawDataReceivable receiver) {
        if (connection == null || receiver == null)
            throw new IllegalArgumentException("GnutellaConnection and RawDataReceivable cannot be null");
        this.receiver = receiver;
        this.connection = connection;
        //handler is initially shutdown, handle will reset before it starts
        this.shutdown = false;
    }

    /**
     * The handshake only exists to save a copy of the socket so that when
     * shutdown is called, it can force the close on the connection.
     * All subclasses must call this parent's method first
     */
    public void handshake(Socket socket) throws HandshakeFailedException {
        this.socket = socket;
        if (hlogger.isInfoEnabled())
            hlogger.info("Initiating handshake with " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
    }

    public void handle(Socket socket) throws IOException {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            bis = new BufferedInputStream(socket.getInputStream(), SOCKETBUF);
            bos = new BufferedOutputStream(socket.getOutputStream(), SOCKETBUF);
            //start the reader thread
            reader = new IncomingReaderThread(bis);
            reader.start();
            GnutellaMessage msg;
            byte[] serializedData;
            ConnectionModel cmodel = connection.getConnectionModel();
            ConnectionThrottler throttler = cmodel.getThrottler();
            while (!shutdown) {
                //check if queue has anything to send
                msg = queue.waitForMessage();
                //write out the message
                if (msg != null) {
                    try {
                        logOutgoingMessage(msg);
                        serializedData = msg.serialize();
                        bos.write(serializedData);
                        bos.flush();
                        cmodel.incrementBytesTransferred(serializedData.length);
                    } catch (ParseException ex) {
                        if (logger.isWarnEnabled())
                            logger.warn("parse error occurred while serializing outgoing message", ex);
                    }
                }
                //throttle
                if (throttler != null && !shutdown)
                    throttler.throttle(cmodel);
                else
                    Thread.currentThread().yield();
            }
        } finally {
            shutdown();
            //somehow the stream/socket error occurred
            //disconnect from server
            IOUtil.closeStream(bos);
            IOUtil.closeStream(bis);
        }
    }

    public void shutdown() {
        //socket will be closed automatically once shutdown flag is set
        shutdown = true;
        //clear all the message and then interrupt the queue
        queue.shutdown();
        //forcefully close the socket
        IOUtil.closeSocket(socket);
    }

    /**
     * resets some of the connection stuff
     */
    public void start() {
        shutdown = false;
        queue.clear();
        queue.start();
    }

    /** queues up the data and wait for thread to send out the data */
    public void send(GnutellaMessage msg) {
        queue.addMessage(msg);
    }

    public class IncomingReaderThread extends Thread {
        private InputStream is;

        public IncomingReaderThread(InputStream is) {
            this.is = is;
        }

        public void run() {
            GnutellaMessageHeader header;
            byte[] header_data = new byte[23];
            byte[] data = new byte[64];
            int headerCount = 0;
            int dataLength = 0;
            int amountRead = 0;
            int currentRead = 0;
            while (!shutdown) {
                try {
                    //since socket is set to timeout at 1 second,
                    //if no data is read in 1 second, an exception is thrown
                    //NOTE: Stupid Sun Java 1.3 does not throw InterruptedIOException
                    //but instead return -1 if remote connection closes
                    //check for any incoming headers
                    if (shutdown || (headerCount = is.read(header_data, 0, 23)) == -1) {
                        shutdown();
                        break;
                    }
                    if (headerCount == 23) {
                        //deserialize the header
                        header = new GnutellaMessageHeader();
                        header.deserialize(header_data, 0);
                        //get the length
                        dataLength = header.getDataLen();
                        //If dataLength goes above 32k (will never be 0
                        //as header message will automatically set length to 0),
                        //then bad client!
                        //drop connection when client is sending bad messages
                        if (dataLength > 32768) {
                            shutdown();
                            break;
                        }
                        //if length is greater than the current buffer, then increase it
                        if (dataLength > data.length)
                            data = new byte[dataLength];
                        amountRead = 0;
                        currentRead = 0;
                        ConnectionModel cmodel = connection.getConnectionModel();
                        ConnectionThrottler throttler = cmodel.getThrottler();
                        while (amountRead < dataLength && currentRead != -1) {
                            if (shutdown) break;
                            if ((currentRead = is.read(data, amountRead, dataLength - amountRead)) != -1) {
                                amountRead += currentRead;
                            }
                            cmodel.incrementBytesTransferred(amountRead);
                            Thread.currentThread().yield();
                        }
                        //read the entire length, now send it back to Protocol handler for processing
                        if (!shutdown)
                            receiver.receive(connection, header, data);
                        //throttle
                        if (throttler != null && !shutdown)
                            throttler.throttle(connection.getConnectionModel());
                        else
                            Thread.currentThread().yield();
                    }
                } catch (Exception ex) {
                    shutdown();
                }
            }
        }
    }

    /** sets the supported features. Mostly used for outgoing connections when the feature headers are not known yet */
    protected void setSupportedFeatureHeaders(HTTPHeader features) {
        this.remoteSupportedFeatures = features;
    }

    /** sets the vendor features. Mostly used for outgoing connections when feature headers are not known yet */
    protected void setVendorFeatureHeaders(HTTPHeader features) {
        this.remoteVendorFeatures = features;
    }

    /** retrieves the remote supported features. Can be null */
    public HTTPHeader getSupportedFeatureHeaders() {
        return remoteSupportedFeatures;
    }

    /** retrieves the remote vendor specific features. Can be null */
    public HTTPHeader getVendorFeatureHeaders() {
        return remoteVendorFeatures;
    }

    /** retrieves the connection object used by the protocol */
    public GnutellaConnection getConnection() {
        return connection;
    }

    /**
     * A convenience method to log the handshake headers that are received
     * from the remote client for debugging purposes.
     */
    protected void logHandshakeHeaders() {
        if (hlogger.isInfoEnabled()) {
            HTTPHeader headers = getSupportedFeatureHeaders();
            if (headers != null) {
                if (headers instanceof HTTPResponseHeader) {
                    HTTPResponseHeader respHeader = (HTTPResponseHeader) headers;
                    hlogger.info(socket.getInetAddress().getHostAddress() + " Status: " + respHeader.getStatusMessage() + " (" + respHeader.getStatusCode() + ")");
                }
                if (hlogger.isDebugEnabled()) {
                    Iterator iter = headers.getHeaderNames().iterator();
                    String header, value;
                    while (iter.hasNext()) {
                        header = (String) iter.next();
                        value = headers.getHeader(header);
                        hlogger.debug("Header " + header + ": " + value);
                    }
                }
            }
        }
    }

    /**
     * Logs the outgoing message to the logger. It will log the message
     * to the appropriate log category.
     * @param msg the gnutella message to log
     */
    protected void logOutgoingMessage(GnutellaMessage msg) {
        switch (msg.getType()) {
            case GnutellaCode.PING:
                if (pingologger.isDebugEnabled())
                    pingologger.debug(msg);
                break;
            case GnutellaCode.PONG:
                if (pongologger.isDebugEnabled())
                    pongologger.debug(msg);
                break;
            case GnutellaCode.PUSH_REQUEST:
                if (pushologger.isDebugEnabled())
                    pushologger.debug(msg);
                break;
            case GnutellaCode.QUERY:
                if (queryologger.isDebugEnabled())
                    queryologger.debug(msg);
                break;
            case GnutellaCode.QUERY_RESPONSE:
                if (hitologger.isDebugEnabled())
                    hitologger.debug(msg);
                break;
            default:
                if (unknownologger.isDebugEnabled())
                    unknownologger.debug(msg);
        }
    }
}
