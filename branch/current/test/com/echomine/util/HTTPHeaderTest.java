package com.echomine.util;

import junit.framework.TestCase;
import com.echomine.util.HTTPResponseHeader;

/**
 * Tests HTTPHeader and its derivative subclasses
 */
public class HTTPHeaderTest extends TestCase {
    public HTTPHeaderTest(String name) {
        super(name);
    }

    public void testHTTPResponseHeaderParse() throws Exception {
        HTTPResponseHeader response = new HTTPResponseHeader();
        String resHeader = "HTTP 200 OK\r\n\r\n";
        StringStream sis = new StringStream(resHeader);
        response.parse(sis);
        assertEquals("HTTP", response.getProtocol());
        assertTrue(response.getStatusCode() == 200);
        assertEquals("OK", response.getStatusMessage());
        //now test another variation
        resHeader = "HTTP 200\r\n\r\n";
        sis = new StringStream(resHeader);
        response.parse(sis);
        assertEquals("HTTP", response.getProtocol());
        assertTrue(response.getStatusCode() == 200);
        assertNull(response.getStatusMessage());
    }
}
