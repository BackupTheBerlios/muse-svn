package com.echomine.jabber.msg;

import com.echomine.jabber.JabberJDOMMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.oro.text.regex.*;
import org.jdom.Element;
import org.jdom.Namespace;

/**
 * This abstract class provides shared functions for the PGP encrypted and signed messages.
 * It contains methods to attach and strip PGP headers.
 */
abstract class AbstractPGPXMessage extends JabberJDOMMessage {
    private static Log log = LogFactory.getLog(AbstractPGPXMessage.class);
    private String pgpStartHeader;
    private String pgpEndHeader;
    private Pattern pattern;

    /**
     * constructs a default message
     */
    public AbstractPGPXMessage(Namespace ns, String pgpStartHeader, String pgpEndHeader) {
        super(new Element("x", ns));
        this.pgpStartHeader = pgpStartHeader;
        this.pgpEndHeader = pgpEndHeader;
        Perl5Compiler compiler = new Perl5Compiler();
        try {
            // This pattern matches the OpenPGP header as defined in RFC2240 - in (6)Radix-64 Conversions.
            String p = "^.*" + pgpStartHeader + ".*?\n\\s*\n(.*)" + pgpEndHeader + ".*$";
            pattern = compiler.compile(p, Perl5Compiler.SINGLELINE_MASK);
        } catch (MalformedPatternException ex) {
            if (log.isWarnEnabled())
                log.warn("PGPEncryptedXMessage Regular Expression did not compile properly", ex);
        }
    }

    /**
     * sets the PGP specified data
     */
    public void setPGPMessage(String data) {
        getDOM().setText(stripPGPHeaders(data));
    }

    /**
     * retrieves the PGP data from the  message
     *
     * @return the PGP data or null if there is none.
     */
    public String getPGPMessage() {
        return addPGPHeaders(getDOM().getText());
    }

    /**
     * Strips the pgp headers off the data passed in
     */
    private String stripPGPHeaders(String data) {
        return Util.substitute(new Perl5Matcher(), pattern, new Perl5Substitution("$1"), data, 1);
    }

    /**
     * Adds the pgp header back into the data
     */
    private String addPGPHeaders(String data) {
        return pgpStartHeader + "\n\n" + data + pgpEndHeader + "\n";
    }
}