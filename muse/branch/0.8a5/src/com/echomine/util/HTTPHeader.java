package com.echomine.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.oro.text.perl.Perl5Util;

/** base template class that contains rudimentary methods to handle headers. */
public class HTTPHeader {
    private HashMap headers;
    private Perl5Util httpHeaderRE = new Perl5Util();

    public HTTPHeader() {
        headers = new HashMap();
    }

    /** clears the contents in this header */
    public void clear() {
        headers.clear();
    }

    /** returns a string that contains only the headers. */
    public String toString() {
        return getHeadersAsString();
    }

    public String getHeadersAsString() {
        StringBuffer buffer = new StringBuffer(200);
        Iterator iter = getHeaderNames().iterator();
        String name;
        String value;
        //append headers
        while (iter.hasNext()) {
            name = (String)iter.next();
            buffer.append(name).append(": ");
            value = (String)headers.get(name);
            buffer.append(value).append("\r\n");
        }
        return buffer.toString();
    }

    public Collection getHeaderNames() {
        return headers.keySet();
    }

    public boolean isEmpty() {
        return headers.isEmpty();
    }

    /**
     * retrieves a specific header
     * @return the header name's value or null if the header doesn't exist
     */
    public String getHeader(String name) {
        return (String)headers.get(name);
    }

    /** adds or replaces a header with the indicated value */
    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    /** parse the headers from an input stream. */
    public void parseHeaders(InputStream is) throws IOException {
        //read in the response... A response with status code of 200 is OK.
        //all other status codes are considered errors
        int SOCKETBUF = 1024;
        byte[] bytebuf = new byte[SOCKETBUF];
        int bytesread;
        String line;
        //keep reading each line until all header information are read
        //(ie. bytesread == 0)
        while ((bytesread = IOUtil.readToCRLF(is, bytebuf, 0, SOCKETBUF)) != 0) {
            //check if first line is response code
            line = new String(bytebuf, 0, bytesread);
            if (httpHeaderRE.match("m#^(\\S+):\\s*(.+)#i", line)) {
                //add header to hashtable
                headers.put(httpHeaderRE.group(1), httpHeaderRE.group(2));
            }
        }
    }
}
