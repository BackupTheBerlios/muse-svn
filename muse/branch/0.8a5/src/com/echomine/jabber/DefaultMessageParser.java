package com.echomine.jabber;

import com.echomine.common.ParseException;
import org.jdom.Element;
import org.jdom.Namespace;

import java.util.HashMap;

/**
 * <p>Contains a list of message parsers.  Essentially, for each type of message, there should be a registered parser for it
 * that will be able to process the incoming data.</p> <p>When a handler receives a message,
 * it will lookup a message class name that can be instantiated to parse the incoming message.
 * Thus, for each custom message that you create, you should register that message's class name.
 * This includes classes that are contained inside the IQ Message.  In fact, the way that the
 * IQMessageParser works is it calls this class again to retrieve a second parser for the message
 * contained inside the IQ Message.</p>
 */
public class DefaultMessageParser implements JabberMessageParser {
    private HashMap msgParsers = new HashMap();
    private HashMap msgClasses = new HashMap();

    public DefaultMessageParser() {
        //create the default set of message parsers
        try {
            setParser("presence", JabberCode.XMLNS_PRESENCE, JabberCode.PARSER_PRESENCE);
            setParser("message", JabberCode.XMLNS_CHAT, JabberCode.PARSER_CHAT);
            setParser("iq", JabberCode.XMLNS_IQ, JabberCode.PARSER_IQ);
            setParser("query", JabberCode.XMLNS_IQ_AUTH, JabberCode.PARSER_IQ_AUTH);
            setParser("query", JabberCode.XMLNS_IQ_ROSTER, JabberCode.PARSER_IQ_ROSTER);
            setParser("query", JabberCode.XMLNS_IQ_REGISTER, JabberCode.PARSER_IQ_REGISTER);
            setParser("query", JabberCode.XMLNS_IQ_XMLRPC, JabberCode.PARSER_IQ_XMLRPC);
            setParser("query", JabberCode.XMLNS_IQ_TIME, JabberCode.PARSER_IQ_TIME);
            setParser("query", JabberCode.XMLNS_IQ_VERSION, JabberCode.PARSER_IQ_VERSION);
            setParser("query", JabberCode.XMLNS_IQ_LAST, JabberCode.PARSER_IQ_LAST);
            setParser("query", JabberCode.XMLNS_IQ_GATEWAY, JabberCode.PARSER_IQ_GATEWAY);
            setParser("query", JabberCode.XMLNS_IQ_AGENTS, JabberCode.PARSER_IQ_AGENTS);
            setParser("query", JabberCode.XMLNS_IQ_PRIVATE, JabberCode.PARSER_IQ_PRIVATE);
            setParser("query", JabberCode.XMLNS_IQ_SEARCH, JabberCode.PARSER_IQ_SEARCH);
            setParser("query", JabberCode.XMLNS_IQ_OOB, JabberCode.PARSER_IQ_OOB);
            setParser("query", JabberCode.XMLNS_IQ_VACATION, JabberCode.PARSER_IQ_VACATION);
            setParser("query", JabberCode.XMLNS_IQ_DISCO_INFO, JabberCode.PARSER_IQ_DISCO_INFO);
            setParser("query", JabberCode.XMLNS_IQ_DISCO_ITEMS, JabberCode.PARSER_IQ_DISCO_ITEMS);
            setParser("vCard", JabberCode.XMLNS_IQ_VCARD, JabberCode.PARSER_IQ_VCARD);
            //register every possible jid type
            //adding workaround.. currently service is under "jabber:client"
            setParser("service", JabberCode.XMLNS_IQ, JabberCode.PARSER_IQ_BROWSE);
            setParser("service", JabberCode.XMLNS_IQ_BROWSE, JabberCode.PARSER_IQ_BROWSE);
            setParser("conference", JabberCode.XMLNS_IQ_BROWSE, JabberCode.PARSER_IQ_BROWSE);
            setParser("user", JabberCode.XMLNS_IQ_BROWSE, JabberCode.PARSER_IQ_BROWSE);
            setParser("application", JabberCode.XMLNS_IQ_BROWSE, JabberCode.PARSER_IQ_BROWSE);
            setParser("headline", JabberCode.XMLNS_IQ_BROWSE, JabberCode.PARSER_IQ_BROWSE);
            setParser("render", JabberCode.XMLNS_IQ_BROWSE, JabberCode.PARSER_IQ_BROWSE);
            setParser("keyword", JabberCode.XMLNS_IQ_BROWSE, JabberCode.PARSER_IQ_BROWSE);
            setParser("x", JabberCode.XMLNS_X_DELAY, JabberCode.PARSER_X_DELAY);
            setParser("x", JabberCode.XMLNS_X_ROSTER, JabberCode.PARSER_X_ROSTER);
            setParser("x", JabberCode.XMLNS_X_EVENT, JabberCode.PARSER_X_EVENT);
            setParser("x", JabberCode.XMLNS_X_EXPIRE, JabberCode.PARSER_X_EXPIRE);
            setParser("x", JabberCode.XMLNS_X_PGP_ENCRYPTED, JabberCode.PARSER_X_PGP_ENCRYPTED);
            setParser("x", JabberCode.XMLNS_X_DATA, JabberCode.PARSER_X_DATA);
            setParser("x", JabberCode.XMLNS_X_OOB, JabberCode.PARSER_X_OOB);
        } catch (ParseException ex) {
            //there should absolutely be no parsing exceptions
            //since we know these parsers exists
            ex.printStackTrace();
        }
    }

    /** checks whether a parser is registered for the specified qname and namespace */
    public boolean supportsParsingFor(String qName, Namespace ns) {
        return msgParsers.containsKey(ns.getURI() + ":" + qName);
    }

    /** removes the message parser associated with a specific namespace tag */
    public void removeParser(String qName, Namespace ns) {
        Object clsName = msgParsers.remove(ns.getURI() + ":" + qName);
        msgClasses.remove(clsName);
    }

    /**
     * sets a message parser to handle a specific namespace. If a parser already exists for the specific namespace, the new
     * parser will replace the old one.  This way, if you decide to override the default parsers, you can
     * do so in an easy manner.
     * @param qName the fully qualified tag name
     * @param ns the Namespace for the tag element
     * @param msgClass the message class that will be instantiated
     * @exception ParseException thrown when class is not found or class is not a message parser
     */
    public void setParser(String qName, Namespace ns, String msgClass) throws ParseException {
        try {
            Class cls = Class.forName(msgClass);
            //do an explicit instantiation and cast to make sure the class is a parser
            JabberMessageParsable parser = (JabberMessageParsable) cls.newInstance();
            //everything is fine, store the class and the parser
            msgClasses.put(msgClass, cls);
            msgParsers.put(ns.getURI() + ":" + qName, msgClass);
        } catch (ClassNotFoundException ex) {
            throw new ParseException("Parser class not found");
        } catch (ClassCastException ex) {
            throw new ParseException("Parser is not of type JabberMessageParsable");
        } catch (InstantiationException ex) {
            throw new ParseException("Parser cannot be initialized to check for validity: " + ex.getMessage());
        } catch (IllegalAccessException ex) {
            throw new ParseException("Illegal access while checking for validity: " + ex.getMessage());
        }
    }

    /**
     * instantiate a message object by looking at the DOM tree.  It does this through reflection.
     * @throws MessageNotSupportedException if no class is able to parse this message
     */
    public JabberMessage createMessage(String qName, Namespace ns, Element msgTree) throws MessageNotSupportedException {
        //find a parser
        String msgClassName = (String) msgParsers.get(ns.getURI() + ":" + qName);
        if (msgClassName == null)
            throw new MessageNotSupportedException("Parser does not exist for the message");
        JabberMessageParsable parser;
        JabberMessage msg = null;
        Class msgClass = (Class) msgClasses.get(msgClassName);
        if (msgClass != null) {
            try {
                parser = (JabberMessageParsable) msgClass.newInstance();
                msg = parser.parse(this, msgTree);
            } catch (InstantiationException ex) {
                throw new MessageNotSupportedException("Error while instantiating message");
            } catch (IllegalAccessException ex) {
                throw new MessageNotSupportedException("Illegal access to message");
            } catch (ParseException ex) {
                throw new MessageNotSupportedException("Message cannot be properly parsed");
            }
        }
        if (msg == null)
            throw new MessageNotSupportedException("Parser does not exist for the message");
        return msg;
    }
}
