package com.echomine.gnutella;

import com.echomine.net.ConnectionListener;
import com.echomine.net.TransferRateThrottler;

/** Represents a download request that contains all the necessary download information to process the download request. */
public class GnutellaDownloadInfo {
    public static final int NORMAL_DOWNLOAD = 1;
    public static final int FIREWALLED_DOWNLOAD = 2;
    GnutellaConnectionModel cmodel;
    GnutellaFileModel filemodel;
    ConnectionListener cl;
    GnutellaFileListener fl;
    int type;

    public GnutellaDownloadInfo(GnutellaConnectionModel cmodel, GnutellaFileModel filemodel, ConnectionListener cl,
                                GnutellaFileListener fl) {
        this.cmodel = cmodel;
        this.filemodel = filemodel;
        this.cl = cl;
        this.fl = fl;
        this.type = NORMAL_DOWNLOAD;
    }

    public GnutellaConnectionModel getConnectionModel() {
        return cmodel;
    }

    public void setConnectionModel(GnutellaConnectionModel cmodel) {
        this.cmodel = cmodel;
    }

    public GnutellaFileModel getFileModel() {
        return filemodel;
    }

    public void setFileModel(GnutellaFileModel filemodel) {
        this.filemodel = filemodel;
    }

    public String getFilename() {
        return filemodel.getFilename();
    }

    public int getFileIndex() {
        return filemodel.getFileIndex();
    }

    public ConnectionListener getConnectionListener() {
        return cl;
    }

    public void setConnectionListener(ConnectionListener cl) {
        this.cl = cl;
    }

    public GnutellaFileListener getFileListener() {
        return fl;
    }

    public void setFileListener(GnutellaFileListener fl) {
        this.fl = fl;
    }

    public TransferRateThrottler getTransferRateThrottler() {
        return filemodel.getThrottler();
    }

    public void setTransferRateThrottler(TransferRateThrottler throttler) {
        filemodel.setThrottler(throttler);
    }

    /** comparison override */
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof GnutellaDownloadInfo)) return false;
        GnutellaDownloadInfo info = (GnutellaDownloadInfo) obj;
        if (filemodel.getFilename().equals(info.filemodel.getFilename()) &&
            filemodel.getFilesize() == info.filemodel.getFilesize() &&
            filemodel.getFileIndex() == info.filemodel.getFileIndex() && cmodel.equals(info.cmodel))
            return true;
        return false;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
