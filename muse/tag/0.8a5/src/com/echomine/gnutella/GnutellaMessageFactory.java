package com.echomine.gnutella;

import com.echomine.common.ParseException;

/**
 * Factory class that will deserialize a set of bytes into its proper gnutella message classes.  This class is extremely
 * simple and only exists to satisfy the abstract factory pattern.
 */
public interface GnutellaMessageFactory {
    /**
     * Determine function type and returns the proper GnutellaMessage instance.
     * @return a new message object for use or null if it doesn't fit.
     */
    GnutellaMessage deserialize(GnutellaMessageHeader header, byte[] data) throws ParseException;
}
