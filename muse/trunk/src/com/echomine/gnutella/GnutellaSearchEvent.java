package com.echomine.gnutella;

import java.util.EventObject;

/** An event object representing a search */
public class GnutellaSearchEvent extends EventObject {
    private MsgQueryResponse msg;

    public GnutellaSearchEvent(GnutellaSearchService searchService, MsgQueryResponse msg) {
        super(searchService);
        this.msg = msg;
    }

    public GnutellaSearchService getSearchService() {
        return (GnutellaSearchService) getSource();
    }

    public MsgQueryResponse getMsgQueryResponse() {
        return msg;
    }
}
