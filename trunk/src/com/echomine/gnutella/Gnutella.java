package com.echomine.gnutella;

import com.echomine.net.ConnectionModel;
import com.echomine.net.PerpetualSocketAcceptor;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * The main gnutella class that you can instantiate for using all default behaviors.  This is a convenience class is meant to
 * act as a wrapper around all the other functionalities (meaning there is really no functional code by itself).  This class
 * is created as a convenience class for developers to use and follows the delegation pattern.
 */
public class Gnutella {
    protected GnutellaConnectionManager cmanager;
    protected GnutellaContext context;
    protected PerpetualSocketAcceptor listenerRouterAcceptor;
    protected GnutellaListenerRouter router;
    protected GnutellaDownloadService downloadService;
    protected GnutellaSearchService searchService;
    protected GnutellaShareService shareService;

    /**
     * This constructor actually accepts a custom message factory that you create so that it can parse your own proprietary
     * messages if that's what you are looking for.
     */
    public Gnutella(GnutellaContext context) {
        this.context = context;
        // create all the services and handlers
        router = new GnutellaListenerRouter();
        cmanager = new GnutellaConnectionManager(context, router);
        downloadService = new GnutellaDownloadService(context, cmanager, router);
        searchService = new GnutellaSearchService(context, cmanager, router);
        shareService = new GnutellaShareService(context, cmanager, router);
    }

    /**
     * Tells gnutella to go into an ONLINE state.  This doesn't mean that it's connected to any other clients yet.  It merely
     * indicates to the services that online state is reached and ready for accepting connections and such.  Basically any
     * service registration gets done when this method is called.
     */
    public void start() {
        //start the connection manager and all the working threads
        cmanager.start();
        try {
            ConnectionModel model;
            if (context.getInterfaceIP() == null)
                model = new ConnectionModel(context.getPort());
            else
                model = new ConnectionModel(context.getInterfaceIP(), context.getPort());
            listenerRouterAcceptor = new PerpetualSocketAcceptor(model);
            listenerRouterAcceptor.aaccept(router);
        } catch (IOException ex) {
        }
    }

    /** convenience wrapper method that simply passes the parameters to connection manager */
    public void connect(String IP, int port) throws UnknownHostException {
        cmanager.connect(IP, port);
    }

    /** convenience wrapper method that simply passes the parameters to connection manager */
    public void connect(GnutellaConnectionModel model) throws UnknownHostException {
        cmanager.connect(model);
    }

    /**
     * disconnect from the gnutella network and go into the OFFLINE state.  This will shutdown all running connections and
     * services (ie. threads and thread workers).
     */
    public void disconnect() {
        //shutdown all connections and thread workers
        cmanager.disconnect();
        //shutdown listener router
        listenerRouterAcceptor.close();
    }

    /** wrapper method to satisfy the protocol abstract method. */
    public void send(GnutellaMessage msg) {
        cmanager.send(msg);
    }

    public GnutellaContext getContext() {
        return context;
    }

    public GnutellaConnectionManager getConnectionManager() {
        return cmanager;
    }

    public GnutellaDownloadService getDownloadService() {
        return downloadService;
    }

    public GnutellaSearchService getSearchService() {
        return searchService;
    }

    public GnutellaShareService getShareService() {
        return shareService;
    }

    public GnutellaListenerRouter getListenerRouter() {
        return router;
    }

    /** convenience method for retrieving the statistics from the connection manager */
    public GnutellaStatistics getStatistics() {
        return cmanager.getStatistics();
    }
}
