package com.echomine.jabber;

import com.echomine.common.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.oro.text.regex.*;

/**
 * Contains the JID resource.  It also knows how to parse the information or output
 * it in the JID compliant format.  This is mainly used to generate or retrieve parts of the JID in a easy way.  you simply
 * pass in the JID string, and then retrieve whatever you like.
 */
public class JID {
    private static Pattern jidPat;
    private static Log log = LogFactory.getLog(JID.class);

    private String node;
    private String host;
    private String resource;

    static {
        Perl5Compiler compiler = new Perl5Compiler();
        try {
            jidPat = compiler.compile("(?:(.+)\\@)?([^/]+)(?:/(.+))?");
        } catch (MalformedPatternException ex) {
            if (log.isWarnEnabled())
                log.warn("JID Regular Expression did not compile properly", ex);
        }
    }

    /**
     * takes in a JID and then parses it into different parts.
     * @throws ParseException if the jid does not conform to the format
     */
    public JID(String jid) throws ParseException {
        if (jid == null) throw new ParseException("JID is null and cannot be parsed");
        //URI Syntax is [node@]domain[/resource]
        Perl5Matcher matcher = new Perl5Matcher();
        if (matcher.matches(jid, jidPat)) {
            //retrieve the info
            MatchResult result = matcher.getMatch();
            this.node = result.group(1);
            this.host = result.group(2);
            this.resource = result.group(3);
        } else {
            throw new ParseException("JID has incorrect format: " + jid);
        }
    }

    /**
     * takes in a set of information to create the JID object that can be use to convert into a JID string
     * @param node the name of the node or user, required
     * @param host the Jabber server name, required
     * @param resource the resource used, can be null to specify none
     */
    public JID(String node, String host, String resource) {
        this.node = node;
        this.host = host;
        this.resource = resource;
    }

    public String getNode() {
        return node;
    }

    public String getHost() {
        return host;
    }

    public String getResource() {
        return resource;
    }

    /** this is the same as getNode(). It's here for less confusion and convenience. */
    public String getUsername() {
        return getNode();
    }

    /**
     * retrieves the node@domain part of the JID.  It is basically the normal JID that you
     * would use if you do not specify a resource.  This is here for convenience.
     * @return the node@domain part of the JID
     */
    public String getJIDWithoutResource() {
        return node + "@" + host;
    }

    /** @return the JID in the correct format */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        if (node != null)
            buf.append(node).append("@");
        buf.append(host);
        if (resource != null)
            buf.append("/").append(resource);
        return buf.toString();
    }

    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (toString().equals(obj.toString())) return true;
        return false;
    }
}
