package com.echomine.gnutella;

import com.echomine.net.ConnectionListener;

/**
 * This class is specifically designed for communication between the GnutellaShareService and GetRequestHandler.  It contains
 * information that is shared between the two.
 */
public class ShareInfo {
    private int currentUploads;
    private ShareFileController shareList;
    private ConnectionListener cl;
    private GnutellaFileListener fl;

    public ShareInfo(ShareFileController shareList, ConnectionListener cl, GnutellaFileListener fl) {
        this.shareList = shareList;
        this.cl = cl;
        this.fl = fl;
        currentUploads = 0;
    }

    public synchronized int getCurrentUploads() {
        return currentUploads;
    }

    /**
     * increments the current uploads.  It will actually first check to see if it will go over
     * the max number of allowed uploads.  If it won't, then the upload is allowed.
     * @return true is upload is allowed to proceed, false otherwise.
     */
    public synchronized boolean incrementCurrentUploads() {
        if (currentUploads >= shareList.getMaxUploads())
            return false;
        currentUploads++;
        return true;
    }

    public synchronized void decrementCurrentUploads() {
        if (currentUploads >= 0)
            currentUploads--;
    }

    public ShareFileController getShareFileController() {
        return shareList;
    }

    public ConnectionListener getConnectionListener() {
        return cl;
    }

    public GnutellaFileListener getFileListener() {
        return fl;
    }
}
