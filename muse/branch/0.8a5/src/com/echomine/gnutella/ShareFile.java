package com.echomine.gnutella;

/**
 * contains immutable information specific for each shared file.  You may extend this class add more of your own shared
 * file-specific information.
 */
public class ShareFile {
    private int fileidx;
    private String filename;
    private long filesize;
    private String extensionBlock;

    /**
     * @param fileidx the index associated with this file
     * @param filename the name of this file (NO directory path, just the name)
     * @param filesize the filesize associated with this file
     * @param extensionBlock optional paratemeter that you can add additional info about the file.  Leave null or empty if not used
     */
    public ShareFile(int fileidx, String filename, long filesize, String extensionBlock) {
        this.fileidx = fileidx;
        this.filename = filename;
        this.filesize = filesize;
        this.extensionBlock = extensionBlock;
    }

    public int getFileidx() {
        return fileidx;
    }

    public String getFilename() {
        return filename;
    }

    public long getFilesize() {
        return filesize;
    }

    public String getExtensionblock() {
        return extensionBlock;
    }
}
