package com.echomine.gnutella;

import javax.swing.event.EventListenerList;
import java.util.HashMap;

/**
 * <p>This Search contains all the necessary method and functions to work with search and retrieving search results.
 * By subscribing to become a search listener, you will listen to ALL search results that comes back to you. This
 * means that if you submit multiple search queries, you will get results that are for all of the queries.</p>
 * <p>In order to listen for search results specific to a search query, you can create a DefaultSearchListener
 * and register it with the Search Service. The default search listener will return results specific to a search.</p>
 */
public class GnutellaSearchService {
    protected EventListenerList listenerList = new EventListenerList();
    protected HashMap queryListeners = new HashMap();
    private GnutellaConnectionManager cmanager;
    private GnutellaContext context;
    private GnutellaListenerRouter listenerRouter;
    private SearchResultMessageListener mlistener;

    public GnutellaSearchService(GnutellaContext context, GnutellaConnectionManager cmanager, GnutellaListenerRouter listenerRouter) {
        this.context = context;
        this.cmanager = cmanager;
        this.listenerRouter = listenerRouter;
        this.mlistener = new SearchResultMessageListener();
        cmanager.addMessageListener(mlistener);
    }

    /**
     * search database based on keyword and minSpeed.  This method accepts an optional search listener
     * that will ONLY receive results specific for the query.  Other query results not for this query will
     * automatically be ignored.  This is a convenience method so that you do not have to write any
     * checking inside your search listener to filter out the results not for the query.  See the comments
     * for GnutellaSearchListener for more information.
     * @see GnutellaSearchListener
     * @param keyword the keyword to search for
     * @param minSpeed the mininum speed for return results. 0 for no minimum.
     * @param l the optional search listener that will listen to results specifically for this query
     * @return MsgQuery the message instance that the method created
     */
    public MsgQuery search(String keyword, short minSpeed, GnutellaSearchListener l) {
        MsgQuery query = new MsgQuery(keyword, minSpeed);
        search(query, l);
        return query;
    }

    /**
     * sends a search that uses an existing message. This method assumes that you already registered
     * a listener to listen for search results.
     * @param query the msg query object
     */
    public void search(MsgQuery query) {
        //send the message
        cmanager.send(query);
    }

    /**
     * sends a search that uses an existing message. This method accepts an optional search listener
     * that will ONLY receive results specific for the query.  Other query results not for this query will
     * automatically be ignored.  This is a convenience method so that you do not have to write any
     * checking inside your search listener to filter out the results not for the query.  See the comments
     * for GnutellaSearchListener for more information.
     * @see GnutellaSearchListener
     * @param query the msg query object
     * @param l the optional search listener that will listen to results specifically for this query
     */
    public void search(MsgQuery query, GnutellaSearchListener l) {
        //store the listener into the table
        if (l != null)
            queryListeners.put(query.getHeader().getMsgID(), l);
        //send the message
        cmanager.send(query);
    }

    /**
     * listens for all search results, no matter which search the results is responding to.
     * This is useful for just listening for all incoming query hits (ie. for debugging purposes).
     * If you are interested in listening only to results specifically for a search query, then
     * you can extend your search listener to look specifically for the query GUID.  You can
     * also use the convenient search() methods that accept message listeners; these listeners
     * will only listen for results specific to a query that you submitted.
     */
    public void addSearchListener(GnutellaSearchListener l) {
        listenerList.add(GnutellaSearchListener.class, l);
    }

    public void removeSearchListener(GnutellaSearchListener l) {
        listenerList.remove(GnutellaSearchListener.class, l);
    }

    /** fires only to the listener that registered to listen to results that replies to a specific query */
    protected void fireSearchResultForListener(MsgQueryResponse msg) {
        //look to see if there is a listener only listening specifically for this result
        GnutellaSearchListener l = (GnutellaSearchListener) queryListeners.get(msg.getHeader().getMsgID());
        if (l == null) return;
        GnutellaSearchEvent evt = new GnutellaSearchEvent(this, msg);
        l.searchResultReceived(evt, msg);
    }

    /** fires it to all the listeners */
    protected void fireSearchResultReceived(MsgQueryResponse msg) {
        Object[] listeners = listenerList.getListenerList();
        GnutellaSearchEvent evt = new GnutellaSearchEvent(this, msg);
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == GnutellaSearchListener.class) {
                // Lazily create the event:
                ((GnutellaSearchListener) listeners[i + 1]).searchResultReceived(evt, msg);
            }
        }
    }

    class SearchResultMessageListener implements GnutellaMessageListener {
        public void messageReceived(GnutellaMessageEvent event) {
            //only look for search result messages for us
            GnutellaMessage msg = event.getMessage();
            if (msg.getType() == GnutellaCode.QUERY_RESPONSE) {
                MsgQueryResponse response = (MsgQueryResponse) msg;
                //fire it to the listener that is waiting for results to a specific query
                fireSearchResultForListener(response);
                //fire it to the rest of the listeners
                fireSearchResultReceived(response);
            }
        }
    }
}
