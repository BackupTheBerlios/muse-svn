package com.echomine.gnutella;

import com.echomine.gnutella.impl.DefaultGnutellaMessageFactory;
import com.echomine.gnutella.impl.DefaultShareList;
import com.echomine.util.HTTPHeader;

import java.net.InetAddress;

/** Context context information to connect to Gnutella and setup of the services. */
public class GnutellaContext {
    private InetAddress interfaceIP;
    private short port = 0;
    private int lineSpeed = 0;
    protected GUID clientID = new GUID();
    private HTTPHeader supportedHeaders = new HTTPHeader();
    private HTTPHeader vendorHeaders = new HTTPHeader();
    private GnutellaMessageFactory messageFactory;
    private ShareFileController shareFileController;
    private IRestrictedHostCallback restrictedHostCallback;

    /** uses the default factory, and the port.  This will bind to all the interfaces when listening to incoming connections */
    public GnutellaContext(short port, int lineSpeed) {
        this(null, port, lineSpeed);
    }

    /**
     * this constructor will bind the gnutella incoming connections listener to a specific interface IP.
     * A null interface IP indicates that you want to bind to all interfaces.
     */
    public GnutellaContext(InetAddress interfaceIP, short port, int lineSpeed) {
        this.interfaceIP = interfaceIP;
        this.port = port;
        this.lineSpeed = lineSpeed;
        supportedHeaders.setHeader("User-Agent", "Echomine Muse");
    }

    public short getPort() {
        return port;
    }

    public void setPort(short port) {
        this.port = port;
    }

    /**
     * sets the line speed.  Gnutella normally uses Kbps as the unit for line speed.  Thus, 28.8 modem speed would be
     * equivalent to 28.  T1 would be considered 1500.
     */
    public void setLineSpeed(int lineSpeed) {
        this.lineSpeed = lineSpeed;
    }

    public int getLineSpeed() {
        return lineSpeed;
    }

    public GUID getClientID() {
        return clientID;
    }

    public InetAddress getInterfaceIP() {
        return interfaceIP;
    }

    public void setIntefaceIP(InetAddress interfaceIP) {
        this.interfaceIP = interfaceIP;
    }

    /**
     * a convenience method that lets you set a header for a supported feature.  If the name of the header already
     * exists, it will be replaced by the new value.
     * @param headerName name of the header
     * @param headerValue value of the header. set to null to remove the header name
     */
    public void setSupportedFeatureHeader(String headerName, String headerValue) {
        supportedHeaders.setHeader(headerName, headerValue);
    }

    /**
     * this retrieves a list of headers that are supported by the client.  It contains a list
     * of headers that are supported by Muse by default and is only
     * used by protocols that support headers (ie. Gnutella 0.6).  The headers define a set of global features
     * that the developer support in their software. Developers can add more supported features or replace
     * existing ones with whatever they like.  All the headers set in here will get sent to remote clients
     * @return supported headers or null if no headers exist
     */
    public HTTPHeader getSupportedFeatureHeaders() {
        if (supportedHeaders.isEmpty()) return null;
        return supportedHeaders;
    }

    /**
     * a convenience method that lets you set a header for a vendor feature.  If the name of the header already
     * exists, it will be replaced by the new value.
     * @param headerName name of the header
     * @param headerValue value of the header. set to null to remove the header name
     */
    public void setVendorFeatureHeader(String headerName, String headerValue) {
        vendorHeaders.setHeader(headerName, headerValue);
    }

    /**
     * Some protocols allows the use of vendor-specific headers during protocol negotiation (ie. Gnutella 0.6).
     * This is here for those vendor specific headers that only applies to each vendor (or gnutella client).
     * By default, Muse does not currently have any vendor specific features so it is empty.
     * @return the headers or null if no headers exists
     */
    public HTTPHeader getVendorFeatureHeaders() {
        if (vendorHeaders.isEmpty()) return null;
        return vendorHeaders;
    }

    /**
     * Retrieves a GnutellaMessageFactory instance.  This can be overridden
     * when you want to parse custom messages.  There should be one and only one
     * instance that is used globally.  If this method is called prior to
     * setting the message factory, a default message factory will be used in
     * its place.
     */
    public GnutellaMessageFactory getMessageFactory() {
        if (messageFactory == null)
            messageFactory = new DefaultGnutellaMessageFactory();
        return messageFactory;
    }

    /**
     * sets a custom message factory.  This should be set at the beginning before
     * gnutella starts.  The message factory can be changed in the middle if
     * so desired, but not recommended.
     */
    public void setMessageFactory(GnutellaMessageFactory factory) {
        this.messageFactory = factory;
    }

    /**
     * retrieves the share file controller related to this context.
     * If the instance is null when retrieved, a default one will be
     * created that will broadcast zero files.
     */
    public ShareFileController getShareFileController() {
        if (shareFileController == null)
            shareFileController = new DefaultShareList();
        return shareFileController;
    }

    /**
     * Sets the share file controller.  The controller can be replaced at any
     * time, although it is not recommended for you to do such a thing.
     */
    public void setShareFileController(ShareFileController shareFileController) {
        this.shareFileController = shareFileController;
    }

    /**
     * Sets the global restricted host callback.
     * If the callback is null, then there will be no host restriction.
     */
    public void setRestrictedHostCallback(IRestrictedHostCallback callback) {
        this.restrictedHostCallback = callback;
    }

    /**
     * Retrieve the current restricted host callback.
     * @return callback or null
     */
    public IRestrictedHostCallback getRestrictedHostCallback() {
        return restrictedHostCallback;
    }
}
