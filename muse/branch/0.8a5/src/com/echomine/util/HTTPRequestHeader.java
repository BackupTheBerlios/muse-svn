package com.echomine.util;

import java.io.IOException;
import java.io.InputStream;
import org.apache.oro.text.perl.Perl5Util;

/** A class representing the Request Header of the HTTP protocol. */
public class HTTPRequestHeader extends HTTPHeader {
    private Perl5Util httpRequestRE = new Perl5Util();
    private String method = "GET";
    private String uri = "/";
    private String protocol = "HTTP/1.0";

    public HTTPRequestHeader() {
        super();
    }

    /**
     * returns a string that contains the beginning of an HTTP request.  The string contains the request line in
     * addition to the headers.  It will also output the last linefeed that seperates headers from body content.
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer(200);
        //add in the status code
        buffer.append(method).append(" ").append(uri).append(" ").append(protocol).append("\r\n");
        //add in headers
        buffer.append(super.toString());
        //add in the last empty line to end the header
        buffer.append("\r\n");
        return buffer.toString();
    }

    /** parse the headers from an input stream. It will also parse the first request line. */
    public void parse(InputStream is) throws IOException {
        //read in the request...
        int SOCKETBUF = 1024;
        byte[] bytebuf = new byte[SOCKETBUF];
        int bytesread;
        String line;
        //read the first line that should be the request
        bytesread = IOUtil.readToCRLF(is, bytebuf, 0, SOCKETBUF);
        //check if first line is request
        line = new String(bytebuf, 0, bytesread);
        //format: GET URL HTTP/1.0
        if (httpRequestRE.match("m#^(\\S+)\\s+(\\S+)\\s+(.+)#i", line)) {
            method = httpRequestRE.group(1);
            uri = httpRequestRE.group(2);
            protocol = httpRequestRE.group(3);
        } else {
            //invalid request header, throw exception
            throw new IOException("Invalid HTTP request header");
        }
        //parse the headers
        parseHeaders(is);
    }

    /** uses a default method of "GET" and default protocol of "HTTP/1.0" */
    public void setRequest(String uri) {
        setRequest("GET", uri);
    }

    /** uses a default protocol of "HTTP/1.0" when doing request */
    public void setRequest(String method, String uri) {
        setRequest(method, uri, "HTTP/1.0");
    }

    public void setRequest(String method, String uri, String protocol) {
        this.method = method;
        this.uri = uri;
        this.protocol = protocol;
    }

    public String getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }

    public String getProtocol() {
        return protocol;
    }
}
