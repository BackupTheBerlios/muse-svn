package com.echomine.gnutella.impl;

import com.echomine.gnutella.GnutellaContext;
import com.echomine.gnutella.GnutellaFileModel;

/**
 * The current way that firewalled downloads are handle is exactly the same way as a direct download.  The only difference is
 * the handshake, but that is already taken care of by the GivRequestHandler.  Thus, for now, firewalled downloads are handled
 * exactly the same way as a direct download.
 */
public class GnutellaFirewalledDownloadHandler extends GnutellaDirectDownloadHandler {
    public GnutellaFirewalledDownloadHandler(GnutellaContext context, GnutellaFileModel model) {
        super(context, model);
    }
}
