package com.echomine.gnutella;

import java.io.IOException;
import java.net.Socket;

/**
 * Implementors of this interface will be handling the main bulk of the request.  Once the process is handled it essentially
 * means all data are transferred.  Implementors should not have to close the socket explicitly as that is handled in the
 * ListenerRouter (the caller of this object) itself already.
 */
public interface ListenerRequestHandler {
    /** handles the main request and contains the request line the remote client gave */
    void handleRequest(String request, Socket socket) throws IOException;

    /**
     * check if this request can be handled by this handler.
     * @return true is request can be handled by this handler.  False otherwise.
     */
    boolean canHandle(String request);
}
