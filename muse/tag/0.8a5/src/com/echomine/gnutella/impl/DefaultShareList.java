package com.echomine.gnutella.impl;

import com.echomine.gnutella.MsgQuery;
import com.echomine.gnutella.ShareFileController;

import java.util.Collection;

/**
 * The default share list is used where no files are shared.  Thus, if you don't want files shared (the default behavior for
 * GnutellaShareService), then use this provided class.
 */
public class DefaultShareList implements ShareFileController {
    public String getFilePath(int fileidx) {
        //no such file exists
        return null;
    }

    public String getFilename(int fileidx) {
        return null;
    }

    public Collection getFiles(MsgQuery query) {
        return null;
    }

    public int getFileCount() {
        return 0;
    }

    public int getTotalSize() {
        return 0;
    }

    public int getMaxUploads() {
        //not sharing, so 0 max uploads
        return 0;
    }
}
