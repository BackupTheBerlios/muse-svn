package com.echomine.gnutella;

import java.util.EventListener;

/**
 * <p>Listener that listens to events.  If you simply add a listener to the Search Service, you will listen for all search
 * results for all search queries you submitted.  If you want to differentiate which results are for which queries, you would
 * need to have the search query's GUID (or Message ID) since all results are tied to a query through the Message
 * GUID.</p><p>For simplicity, the Search Service contains convenience methods where if you submit a search query along with a
 * search listener, that listener automatically defaults to returning results specific for that query only; in that case, you
 * do not need to do the filtering inside your search listener as it's already been done.</p><p>If you're thinking about
 * storing the message GUID and using it as a key inside a Hashtable, make sure that you use the GUID.getHashCode() as the
 * key, not the GUID object instance itself.  Otherwise, you will have trouble retrieving objects from the hashtable.</p>
 */
public interface GnutellaSearchListener extends EventListener {
    void searchResultReceived(GnutellaSearchEvent event, MsgQueryResponse msg);
}
