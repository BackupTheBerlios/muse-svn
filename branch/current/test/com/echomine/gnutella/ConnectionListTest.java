package com.echomine.gnutella;

import com.echomine.gnutella.GnutellaConnectionModel;
import com.echomine.gnutella.IRestrictedHostCallback;
import com.echomine.gnutella.impl.ConnectionListImpl;
import com.echomine.net.ConnectionVetoException;
import junit.framework.TestCase;

import java.net.InetAddress;

/**
 * Tests Connection List implementations
 */
public class ConnectionListTest extends TestCase {
    public ConnectionListTest(String name) {
        super(name);
    }

    /**
     * Checks whether add connection is return the correct types
     */
    public void testConnectionListImplAddConnection() throws Exception {
        ConnectionListImpl clist = new ConnectionListImpl();
        GnutellaConnectionModel cmodel = new GnutellaConnectionModel("127.0.0.1", 6346);
        GnutellaConnectionModel cmodel2 = new GnutellaConnectionModel("127.0.0.1", 6346);
        StubGnutellaConnection conn = new StubGnutellaConnection(cmodel);
        StubGnutellaConnection conn2 = new StubGnutellaConnection(cmodel2);
        //adding this first connection should be true
        assertTrue(clist.addConnection(conn, cmodel));
        //run the conn so that the events are properly registered
        conn.fireStartingEventOnly();
        //adding the same connection should result in false
        assertFalse(clist.addConnection(conn, cmodel));
        //adding a connection that contains the same connection model should result in false
        assertFalse(clist.addConnection(conn2, cmodel2));
    }

    /**
     * tests host restriction
     */
    public void testConnectionListImplRestrictedHost() throws Exception {
        ConnectionListImpl clist = new ConnectionListImpl();
        IRestrictedHostCallback alwaysRestrict = new IRestrictedHostCallback() {
            public boolean isHostRestricted(InetAddress host) {
                if (host.getHostAddress().equals("127.0.0.1"))
                    return true;
                return false;
            }
        };
        clist.setRestrictedHostCallback(alwaysRestrict);
        //this should return true
        assertTrue(clist.isHostRestricted(InetAddress.getByName("127.0.0.1")));
        //this should return false
        assertFalse(clist.isHostRestricted(InetAddress.getByName("10.1.1.1")));
        clist.setRestrictedHostCallback(null);
        //this should return false
        assertFalse(clist.isHostRestricted(InetAddress.getByName("127.0.0.1")));
        //and so should this
        assertFalse(clist.isHostRestricted(InetAddress.getByName("10.1.1.1")));
    }

    /**
     * tests the max incoming connection restriction
     */
    public void testConnectionListImplMaxIncomingConnection() throws Exception {
        ConnectionListImpl clist = new ConnectionListImpl();
        GnutellaConnectionModel cmodel = new GnutellaConnectionModel("127.0.0.1", 6346, GnutellaConnectionModel.INCOMING);
        GnutellaConnectionModel cmodel2 = new GnutellaConnectionModel("10.1.1.1", 6346, GnutellaConnectionModel.INCOMING);
        GnutellaConnectionModel cmodel3 = new GnutellaConnectionModel("10.1.1.2", 6346, GnutellaConnectionModel.INCOMING);
        StubGnutellaConnection conn = new StubGnutellaConnection(cmodel);
        StubGnutellaConnection conn2 = new StubGnutellaConnection(cmodel2);
        StubGnutellaConnection conn3 = new StubGnutellaConnection(cmodel3);
        //set the max incoming connections to 2 for easier testing
        clist.setMaxIncomingConnections(2);
        //adding the three connections should be true
        assertTrue(clist.addConnection(conn, cmodel));
        assertTrue(clist.addConnection(conn2, cmodel2));
        assertTrue(clist.addConnection(conn3, cmodel3));
        //now simulate a connection that is added
        conn.fireStartingEventOnly();
        //max connection shouldn't have reached yet
        assertFalse(clist.isMaxIncomingReached());
        //now let's add another connection
        conn2.fireStartingEventOnly();
        //max connection should be reached here
        assertTrue(clist.isMaxIncomingReached());
        try {
            conn3.fireStartingEventOnly();
            //this should have the connection veto exception fired
            //because max incoming connections is reached
            fail("Connection should have been vetoed due to max incoming connections reached");
        } catch (ConnectionVetoException ex) {
        }
    }

    /**
     * tests the max outgoing connection restriction
     */
    public void testConnectionListImplMaxOutgoingConnection() throws Exception {
        ConnectionListImpl clist = new ConnectionListImpl();
        GnutellaConnectionModel cmodel = new GnutellaConnectionModel("127.0.0.1", 6346, GnutellaConnectionModel.OUTGOING);
        GnutellaConnectionModel cmodel2 = new GnutellaConnectionModel("10.1.1.1", 6346, GnutellaConnectionModel.OUTGOING);
        GnutellaConnectionModel cmodel3 = new GnutellaConnectionModel("10.1.1.2", 6346, GnutellaConnectionModel.OUTGOING);
        StubGnutellaConnection conn = new StubGnutellaConnection(cmodel);
        StubGnutellaConnection conn2 = new StubGnutellaConnection(cmodel2);
        StubGnutellaConnection conn3 = new StubGnutellaConnection(cmodel3);
        //set the max incoming connections to 2 for easier testing
        clist.setMaxOutgoingConnections(2);
        //adding the three connections should be true
        assertTrue(clist.addConnection(conn, cmodel));
        assertTrue(clist.addConnection(conn2, cmodel2));
        assertTrue(clist.addConnection(conn3, cmodel3));
        //now simulate a connection that is added
        conn.fireStartingEventOnly();
        //max connection shouldn't have reached yet
        assertFalse(clist.isMaxOutgoingReached());
        //now let's add another connection
        conn2.fireStartingEventOnly();
        //max connection should be reached here
        assertTrue(clist.isMaxOutgoingReached());
        try {
            conn3.fireStartingEventOnly();
            //this should have the connection veto exception fired
            //because max incoming connections is reached
            fail("Connection should have been vetoed due to max incoming connections reached");
        } catch (ConnectionVetoException ex) {
        }
    }
}
