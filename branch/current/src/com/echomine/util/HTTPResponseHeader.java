package com.echomine.util;

import org.apache.oro.text.perl.Perl5Util;

import java.io.IOException;
import java.io.InputStream;

/**
 * This object handles outputting and parsing of a HTTP response for header information.  It will parse out
 * the headers and store them for easy retrieval. The body content of the HTTP response is NOT parsed by this object.
 */
public class HTTPResponseHeader extends HTTPHeader {
    private String protocol = "HTTP";
    private int code;
    private String statusMsg;
    private Perl5Util httpResponseRE = new Perl5Util();

    public HTTPResponseHeader() {
        super();
    }

    public HTTPResponseHeader(int code, String msg) {
        super();
        setStatus(code, msg);
    }

    public void clear() {
        super.clear();
        code = 0;
        statusMsg = null;
    }

    /**
     * returns a string that contains the beginning of an HTTP request.  The string contains the first status code line in
     * addition to the headers.  It will also output the last linefeed that seperates headers from body content.
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer(200);
        //add in the status code
        buffer.append(protocol).append(" ").append(code).append(" ").append(statusMsg).append("\r\n");
        //add in headers
        buffer.append(super.toString());
        //add in the last empty line to end the header
        buffer.append("\r\n");
        return buffer.toString();
    }

    /** parse the headers from an input stream. It will also parse the first status code line. */
    public void parse(InputStream is) throws IOException {
        //read in the response... A response with status code of 200 is OK.
        //all other status codes are considered errors
        int SOCKETBUF = 1024;
        byte[] bytebuf = new byte[SOCKETBUF];
        int bytesread;
        String line;
        //read the first line that should be the response
        bytesread = IOUtil.readToCRLF(is, bytebuf, 0, SOCKETBUF);
        //check if first line is response code
        line = new String(bytebuf, 0, bytesread);
        if (httpResponseRE.match("m#^(\\S+)\\s+(\\d+)(?:\\s+(.+))?#i", line)) {
            protocol = httpResponseRE.group(1);
            code = Integer.parseInt(httpResponseRE.group(2));
            statusMsg = httpResponseRE.group(3);
        } else {
            throw new IOException("Invalid HTTP response header");
        }
        //parse the headers
        parseHeaders(is);
    }

    /** sets the status code and message */
    public void setStatus(int code, String msg) {
        this.code = code;
        this.statusMsg = msg;
    }

    /** @return status code associated with this request */
    public int getStatusCode() {
        return code;
    }

    /** @return status message associated with this request */
    public String getStatusMessage() {
        return statusMsg;
    }

    /** @return the protocol string (ie. HTTP/1.1) */
    public String getProtocol() {
        return protocol;
    }

    /** sets the protocol string.  defaults to HTTP */
    public void setProtocol(String protocol) {
        if (protocol == null)
            this.protocol = "HTTP";
        else
            this.protocol = protocol;
    }
}
