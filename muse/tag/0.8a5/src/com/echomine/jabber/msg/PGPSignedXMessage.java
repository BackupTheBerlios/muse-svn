package com.echomine.jabber.msg;

import org.jdom.Element;
import com.echomine.jabber.JabberJDOMMessage;
import com.echomine.jabber.JabberCode;

/**
 * <p>Support PGP signed messages.  This class will work with PGP-signed message.  It's really simple
 * because it simply contains only the encrypted data.  It will not do any encryption for you.
 * That is up to you to implement on the client level since developers use
 * different Encryption packages to encrypt data.
 * <p>Signed messages are normally used for <presence/> messages.
 * The signing should use Status string of the Presence message, and signed
 * using the private key of the sender.</p>
 * <p><b>Current Implementation: <a href="http://www.jabber.org/jeps/jep-0027.html">JEP-0027 Version 0.2</a></b></p>
 */
public class PGPSignedXMessage extends JabberJDOMMessage {
    /** constructs a default message */
    public PGPSignedXMessage() {
        super(new Element("x", JabberCode.XMLNS_X_PGP_SIGNED));
    }

    /** sets the PGP encrypted data to the specified data */
    public void setPGPMessage(String data) {
        getDOM().setText(data);
    }

    /**
     * retrieves the encrypted message
     * @return the encrypted message or null if there is none.
     */
    public String getPGPMessage() {
        return getDOM().getText();
    }

    public int getMessageType() {
        return JabberCode.MSG_X_PGP_SIGNED;
    }
}
