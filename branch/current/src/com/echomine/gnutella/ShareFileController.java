package com.echomine.gnutella;

import java.util.Collection;

/**
 * <p>The interface contains methods to retrieve share files.</p>
 * <p>Gnutella uses a indexing system to uniquely identify files.  Thus, you need to have a way of giving each file a unique
 * index number.  The easiest way is to just number them sequentially as you go through the list of shared files (ie. with a
 * for-do loop) and put them into a hash table for easier retrieval.</p>
 * <p>A problem you will run into is when you share the file, only the filename and the index number will be used.  Thus,
 * during the process of searching and requesting, the path of where the filename will be lost.  You need to also keep a
 * record of exactly where the file is stored (path + filename).  Thus, the way to go about this is to use the file index as
 * the unique identifier that retrieves the entire path to the file being requested.  You can then either validate the
 * filename to make sure the file being requested corresponds with the file index.</p>
 * <p>The reason why the share file searching mechanism is delegated to the developer is because this will give the developer
 * a chance to implement the sharing of files for all services being used in one class.  Thus, for instance, one class can be
 * written to be used by the Napster and Gnutella modules.  Only one shared file list will need to be maintained and makes it
 * much easier to work with on the higher level.  Of course, some default implementations of ShareFileController are provided
 * for convenience if you're not interested in using multiple modules.  Also, if advanced searching algorithms are used, the
 * developer will need to implement that.</p> <p>The number of uploads are limited by the Share Service.  However, the number
 * of uploads is determined by the controller and does not get set in the Share Service.</p>
 */
public interface ShareFileController {
    /** @return location to the file (directory path + filename), or null if file is not shared or doesn't exist */
    String getFilePath(int fileidx);

    /**
     * retrieves just the filename associated with the file index.  The reason this method is required
     * is because sometimes the real filename + path does not match the published filename.  If this is the
     * case, then we need to have a way to get the published filename based on the file index.
     * @return filename for the file index, null if index doesn't exist.
     */
    String getFilename(int fileidx);

    /**
     * retreives a list of files to be shared.  This is used to send a list of shared files in response to a search request
     * with the specified criteria.  It is up to the implementor to use whatever search algorithm to do the criteria matching.
     * @see ShareFile
     * @return a Collection of MsgResRecord objects, null if no files fit the search criteria
     */
    Collection getFiles(MsgQuery query);

    /** @return the number of files shared */
    int getFileCount();

    /** @return total shared file size in KB. */
    int getTotalSize();

    /** @return max number of simultaneous uploads. -1 if unlimited. */
    int getMaxUploads();
}
