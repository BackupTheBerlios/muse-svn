package com.echomine.gnutella;

import com.echomine.net.FileModel;
import com.echomine.net.TransferRateThrottler;

/**
 * The basic model of the FileTransfer.  It extends from FileModel and adds appropriate fields
 * specific to Gnutella.
 */
public class GnutellaFileModel extends FileModel {
    private int lineSpeed;
    private int index;
    private GnutellaConnectionModel connectionModel;

    /**
     * Useful constructor for creating a filemodel to pass to getFile() for file transferring.
     * @param filename the name of the file to retrieve
     * @param idx index associated with the file
     */
    public GnutellaFileModel(String filename, int idx) {
        this(filename, 0, idx);
    }

    /**
     * @param filename the name of the file to retrieve
     * @param resumeOffset the bytes at which to resume the file
     * @param idx index associated with the file
     */
    public GnutellaFileModel(String filename, long resumeOffset, int idx) {
        this(filename, "", resumeOffset, idx);
    }

    /**
     * Useful constructor for creating a filemodel to pass to getFile()
     * for file transferring.  This is especially good when you are not
     * passing in any file transfer listeners and must set the save location.
     * @param filename the name of the file to retrieve
     * @param saveLocation the location to save the file
     * @param idx index associated with the file
     */
    public GnutellaFileModel(String filename, String saveLocation, int idx) {
        this(filename, saveLocation, 0, idx);
    }

    /**
     * @param filename the name of the file to retrieve
     * @param saveLocation the location to save the file
     * @param resumeOffset the bytes at which to resume the file
     * @param index index associated with the file
     */
    public GnutellaFileModel(String filename, String saveLocation, long resumeOffset, int index) {
        this(filename, saveLocation, resumeOffset, index, null);
    }

    /**
     * @param filename the name of the file to retrieve
     * @param saveLocation the location to save the file
     * @param resumeOffset the bytes at which to resume the file
     * @param index the index associated with the file
     * @param throttler the throttler to use, null if not needed
     */
    public GnutellaFileModel(String filename, String saveLocation, long resumeOffset, int index, TransferRateThrottler throttler) {
        super(filename, saveLocation, resumeOffset, throttler);
        this.index = index;
    }

    public GnutellaFileModel(String filename, String saveLocation, int idx, GnutellaConnectionModel cModel) {
        this(filename, saveLocation, 0, idx, null);
        connectionModel = cModel;
    }

    public int getLineSpeed() {
        return lineSpeed;
    }

    public void setLineSpeed(int lineSpeed) {
        this.lineSpeed = lineSpeed;
    }

    public int getFileIndex() {
        return index;
    }

    public void setFileIndex(int index) {
        this.index = index;
    }

    /**
     * @return the connection model associated with this model
     */
    public GnutellaConnectionModel getConnectionModel() {
        return connectionModel;
    }

    public void setConnectionModel(GnutellaConnectionModel cmodel) {
        this.connectionModel = cmodel;
    }
}
