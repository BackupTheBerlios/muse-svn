package com.echomine.jabber.msg;

import com.echomine.jabber.DefaultMessageParser;
import com.echomine.jabber.JabberCode;
import com.echomine.xmlrpc.Call;
import com.echomine.xmlrpc.Response;
import com.echomine.xmlrpc.SerializerFactory;
import junit.framework.TestCase;

/**
 * Tests the XML RPC message to see if the call is outputting the proper XML
 */
public class XMLRPCMessageTest extends TestCase {
    DefaultMessageParser parser = new DefaultMessageParser();
    SerializerFactory factory = new SerializerFactory();

    public void testXMLRPCMessage() throws Exception {
        Response response = new Response(4, "Too Many Parameters.", factory);
        // Create a Call object.
        Call call = new Call("getHelloWorldString", factory);
        call.addParameter(new Integer(1));
        XMLRPCMessage rpcMsg = new XMLRPCMessage(call);
        assertEquals("<query xmlns=\"jabber:iq:rpc\">" +
                "<methodCall><methodName>getHelloWorldString</methodName><params><param><value><int>1</int></value>" +
                "</param></params></methodCall></query></iq>", rpcMsg.toString().substring(rpcMsg.toString().indexOf("<query")));
        rpcMsg = new XMLRPCMessage(response);
        assertEquals("<query xmlns=\"jabber:iq:rpc\">" +
                "<methodResponse><fault><value><struct><member><name>faultString</name><value>" +
                "<string>Too Many Parameters.</string></value></member><member><name>faultCode</name><value>" +
                "<int>4</int></value></member></struct></value></fault></methodResponse></query></iq>", rpcMsg.toString().substring(rpcMsg.toString().indexOf("<query")));
    }

    /**
     * this tests that the parser has the message registered to parse the namespace
     */
    public void testParserSupportsMessage() throws Exception {
        assertTrue(parser.supportsParsingFor("query", JabberCode.XMLNS_IQ_XMLRPC));
    }

    /** tests message type compliance to make sure it is returning the proper type */
    public void testMessageType() {
        XMLRPCMessage msg = new XMLRPCMessage();
        assertEquals(JabberCode.MSG_IQ_XMLRPC, msg.getMessageType());
    }
}
