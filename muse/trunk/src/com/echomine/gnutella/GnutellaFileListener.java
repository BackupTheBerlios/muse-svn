package com.echomine.gnutella;

import com.echomine.net.FileListener;

/**
 * Currently exactly the same as FileListener, but exists only for future use. The abstract file handler that
 * is passed into the event can be type casted into a GnutellaFileHandler and all of its subclasses.
 */
public interface GnutellaFileListener extends FileListener {
}
