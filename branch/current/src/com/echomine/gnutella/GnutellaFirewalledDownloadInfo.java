package com.echomine.gnutella;

import com.echomine.net.ConnectionListener;

/** The Extended DownloadInfo adds a field for remote Client ID for use when firewalled downloads are being used. */
public class GnutellaFirewalledDownloadInfo extends GnutellaDownloadInfo {
    GUID remoteClientID;

    public GnutellaFirewalledDownloadInfo(GnutellaConnectionModel cmodel, GnutellaFileModel filemodel, ConnectionListener cl,
                                          GnutellaFileListener fl, GUID remoteClientID) {
        super(cmodel, filemodel, cl, fl);
        this.remoteClientID = remoteClientID;
        this.type = FIREWALLED_DOWNLOAD;
    }

    public GUID getRemoteClientID() {
        return remoteClientID;
    }

    public void setRemoteClientID(GUID remoteClientID) {
        this.remoteClientID = remoteClientID;
    }
}
