package com.echomine.util;

import org.apache.oro.text.perl.Perl5Util;

/**
 * Contains methods to work with the HTTP Range header field.  It will parse the header fields and provide methods to easily
 * access the information contained in the header.
 */
public class HTTPRangeHeader {
    private Perl5Util rangeRE = new Perl5Util();
    private long start;
    private long end;

    public HTTPRangeHeader() {
    }

    /**
     * takes in a range value (NOT the header name) and parses it for use.
     * the format of the range value is "byte=<starting pos>-<ending pos>".
     * a -1 for one of the positions indicates that it doesn't exist.
     * @return true if parsing is successful. False otherwise
     */
    public boolean parse(String range) {
        //try to match and if no match, return false
        //Range format: byte=0-1,0-,-1
        //will currently not take in more than one
        if (!rangeRE.match("m#^bytes=(\\d+)?-(\\d+)?$#", range)) return false;
        //matched, let's extract the values
        String value = rangeRE.group(1);
        //value doesn't exist for start position
        if (value == null) start = -1;
        else
            start = Long.parseLong(value);
        value = rangeRE.group(2);
        if (value == null) end = -1;
        else
            end = Long.parseLong(value);
        return true;
    }

    /** @return the starting position.  The position is INCLUSIVE and starts at zero. -1 means nonexistent. */
    public long getStart() {
        return start;
    }

    /** @return the end position.  The position is INCLUSIVE. -1 means nonexistent. */
    public long getEnd() {
        return end;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer(30);
        buffer.append("Range: bytes=");
        if (start > -1) buffer.append(start);
        buffer.append("-");
        if (end > -1) buffer.append(end);
        buffer.append("\r\n");
        return buffer.toString();
    }

    /**
     * sets the starting and ending position.  Note that the positions are INCLUSIVE and can start at 0.
     * If the ending offset is less than the starting offset, the ending offset will be set as -1 while
     * the starting offset is retained.  The offsets must be either -1 or greater than -1.  Anything
     * less than -1 will be considered as -1.
     * @param start the starting offset. -1 to start at the beginning.
     * @param end the ending offset. -1 to specify the end of the file.
     */
    public void setRange(long start, long end) {
        if (end < start) end = -1;
        this.start = start;
        this.end = end;
    }
}
