package com.echomine.jabber.parser;

import com.echomine.jabber.JabberContentHandler;
import com.echomine.jabber.JabberErrorHandler;
import com.echomine.jabber.JabberSAXParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.crimson.jaxp.SAXParserFactoryImpl;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;

/**
 * This uses Crimson as the parser.  The parser instantitates the crimson parser directly.  Thus, this parser is good to use
 * if you're running the API under a servlet engine.
 */
public class JabberCrimsonParser implements JabberSAXParser {
    private static Log log = LogFactory.getLog(JabberCrimsonParser.class);

    /**
     * this method will create the parser, set the handlers, and run the parser.  The method will be run in its own thread so
     * you don't have to worry about IO Blocking.  This InputSource is actually the incoming socket reader
     * from the Jabber connection.
     * @param validating whether to validate the document or not based on the DTD
     * @param namespaceAware whether the parser should be aware of namespaces
     * @param contentHandler the content handler class for receiving the sax events
     * @param errorHandler optional handler to receive error events (null if not used)
     * @param reader the stream to read the document from
     */
    public void parse(boolean validating, boolean namespaceAware, JabberContentHandler contentHandler,
                      JabberErrorHandler errorHandler, InputSource reader) {
        //create a sax handler to handle the incoming xml data
        SAXParserFactory spf = new SAXParserFactoryImpl();
        spf.setValidating(validating);
        spf.setNamespaceAware(namespaceAware);
        try {
            // Create a Crimson SAXParser
            SAXParser jaxpParser = spf.newSAXParser();
            // Get the encapsulated SAX parser
            XMLReader xmlReader = jaxpParser.getXMLReader();
            // Set the ContentHandler of the XMLReader
            xmlReader.setContentHandler(contentHandler);
            // Set an ErrorHandler before parsing
            if (errorHandler != null)
                xmlReader.setErrorHandler(errorHandler);
            // Tell the XMLReader to parse the XML document
            xmlReader.parse(reader);
        } catch (ParserConfigurationException ex) {
            if (log.isInfoEnabled())
                log.info("Parser Configuration Error: " + ex.getMessage());
        } catch (SAXException ex) {
            if (log.isInfoEnabled())
                log.info("SAX Exception: " + ex.getMessage());
        } catch (IOException ex) {
            if (log.isInfoEnabled())
                log.info("IOException: " + ex.getMessage());
        }
    }
}
