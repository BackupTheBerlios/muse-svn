package com.echomine.gnutella;

import com.echomine.net.SocketHandler;
import com.echomine.util.IOUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Vector;

/**
 * The main handler for incoming requests.  It acts as a incoming request router that looks at the request and then dispatches
 * to appropriate handlers to handle the request.
 */
public class GnutellaListenerRouter implements SocketHandler {
    protected static final byte[] INVALID_REQUEST = "HTTP 503 INVALID REQUEST".getBytes();
    protected static final byte[] REQUEST_NOT_SUPPORTED = "HTTP 503 REQUEST NOT SUPPORTED".getBytes();
    protected static final int SOCKETBUF = 1024;
    private Vector requestHandlers;

    public GnutellaListenerRouter() {
        requestHandlers = new Vector(5);
    }

    /**
     * initial handshake and determine if the connection is a particular request.
     * Then it obtains a Handler responsible for the connection and hands it off to that handler.  Since this is run as a
     * thread, the passed in parameter is ignored.
     */
    public void handle(Socket socket) throws IOException {
        //non-buffered stream for safety
        InputStream is = null;
        OutputStream os = null;
        try {
            //set timeout to 1 second
            socket.setSoTimeout(1000);
            os = socket.getOutputStream();
            is = socket.getInputStream();
            //read in an entire request line
            byte[] bytebuf = new byte[SOCKETBUF];
            int bytesread = IOUtil.readToCRLF(is, bytebuf, 0, SOCKETBUF);
            if (bytesread <= 0) {
                //bad request... send back an error and just end the connection
                os.write(INVALID_REQUEST);
                return;
            }
            //valid request (a real text line)
            String request = new String(bytebuf, 0, bytesread);
            //now check if a handler is set to handle this request
            int size = requestHandlers.size();
            ListenerRequestHandler listener = null;
            boolean handlerFound = false;
            for (int i = 0; i < size; i++) {
                listener = (ListenerRequestHandler) requestHandlers.get(i);
                if (listener.canHandle(request)) {
                    handlerFound = true;
                    //request handled, just quit and close this connection
                    break;
                }
            }
            if (handlerFound) {
                socket.setSoTimeout(0);
                listener.handleRequest(request, socket);
            } else {
                os.write(REQUEST_NOT_SUPPORTED);
            }
        } finally {
            IOUtil.closeStream(os);
            IOUtil.closeStream(is);
        }
    }

    /** Nothing to shutdown since no sockets are open and no loops occur in this class. */
    public void shutdown() {
    }

    /** nothing to start since no sockets are open and no loops occur in this class */
    public void start() {
    }

    /**
     * adds a handler for a specific request.  Just make sure that you don't add handlers that handle the same type of
     * request.  Otherwise there may be a chance that you will have the request handled by a handler that you thought should
     * be handled by another one.
     */
    public void addRequestHandler(ListenerRequestHandler handler) {
        requestHandlers.add(handler);
    }

    /** removes a handler for a specified command */
    public void removeRequestHandler(ListenerRequestHandler handler) {
        requestHandlers.remove(handler);
    }
}
