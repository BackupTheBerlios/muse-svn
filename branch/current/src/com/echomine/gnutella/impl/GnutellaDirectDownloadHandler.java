package com.echomine.gnutella.impl;

import alt.java.net.Socket;
import com.echomine.gnutella.GnutellaContext;
import com.echomine.gnutella.GnutellaFileHandler;
import com.echomine.gnutella.GnutellaFileModel;
import com.echomine.net.FileEvent;
import com.echomine.net.TransferRateThrottler;
import com.echomine.net.TransferVetoException;
import com.echomine.util.HTTPRequestHeader;
import com.echomine.util.HTTPResponseHeader;
import com.echomine.util.IOUtil;
import org.apache.oro.text.perl.Perl5Util;

import java.io.*;

/**
 * Implementation of the underlying layer that does the connection and handshaking with the remote host to download a file.
 * NOTE that the handler is NOT thread-safe because of the class scope of the RE objects, which aren't thread-safe to begin
 * with. However, since in this context, the handle is used to process one request at a time, it's ok.
 */
public class GnutellaDirectDownloadHandler extends GnutellaFileHandler {
    protected static final int SOCKETBUF = 8192;
    private boolean shutdown;
    private Perl5Util httpContentRangeHeaderRE = new Perl5Util();

    public GnutellaDirectDownloadHandler(GnutellaContext context, GnutellaFileModel model) {
        super(context, model);
        shutdown = false;
    }

    public void handle(Socket socket) throws IOException {
        BufferedInputStream bis = null;
        BufferedWriter bw = null;
        //obtain a filemodel first
        GnutellaFileModel filemodel = (GnutellaFileModel) model;
        try {
            bis = new BufferedInputStream(socket.getInputStream(), SOCKETBUF);
            bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            //fire transfer starting so that the transfer is authorized to continue
            //also, resume file size and save location can also be set here if not set already
            fireFileTransferStarting(new FileEvent(this, filemodel, FileEvent.TRANSFER_STARTING), new FileEvent(this, filemodel, FileEvent.TRANSFER_VETOED));
            logTransferStarting();
            //submit GET request for the file
            HTTPRequestHeader request = new HTTPRequestHeader();
            request.setRequest("/get/" + filemodel.getFileIndex() + "/" + filemodel.getFilename());
            request.setHeader("User-Agent", getContext().getSupportedFeatureHeaders().getHeader("User-Agent"));
            request.setHeader("Connection", "Keep-Alive");
            request.setHeader("Range", "bytes=" + filemodel.getCurrentFilesize() + "-");
            bw.write(request.toString());
            bw.flush();
            //do a periodic check to see if caller shut us down
            if (shutdown) {
                FileEvent e = new FileEvent(this, filemodel, FileEvent.TRANSFER_CANCELLED);
                fireFileTransferFinished(e);
                logTransferFinished(e);
                return;
            }
            //read in the response... A response with status code of 200 is OK.
            //all other status codes are considered errors
            HTTPResponseHeader response = new HTTPResponseHeader();
            response.parse(bis);
            //save the response for developer to use
            setResponseHeaders(response);
            //check if status code is 200 (which means it's ok)
            if (response.getStatusCode() != 200) {
                FileEvent e;
                if (response.getStatusCode() == 503)
                    e = new FileEvent(this, filemodel, FileEvent.TRANSFER_ERRORED, "Busy");
                else if (response.getStatusCode() == 403)
                    e = new FileEvent(this, filemodel, FileEvent.TRANSFER_ERRORED, "Forbidden");
                else if (response.getStatusMessage() != null)
                    e = new FileEvent(this, filemodel, FileEvent.TRANSFER_ERRORED, response.getStatusMessage());
                else
                    e = new FileEvent(this, filemodel, FileEvent.TRANSFER_ERRORED, "Status " + response.getStatusCode());
                fireFileTransferFinished(e);
                logTransferFinished(e);
                return;
            }
            //reaching here indicates that status is OK.  Continue on by obtaining the
            //Content-Length or Content-Range header and getting the file size information
            //read until header information are finished (ie. bytesread == 0)
            boolean contentHeaderFound = false;
            String contentLength = response.getHeader("Content-Length");
            String contentRange = response.getHeader("Content-Range");
            //add checks for Limewire which is sending header as Content-length
            if (contentLength == null)
                contentLength = response.getHeader("Content-length");
            if (contentRange == null)
                contentRange = response.getHeader("Content-range");
            if (contentLength != null) {
                //set file size
                filemodel.setFilesize(Integer.parseInt(contentLength));
                contentHeaderFound = true;
            } else if (contentRange != null && httpContentRangeHeaderRE.match("m#^bytes=(\\d+)-(\\d+)/(\\d+)#i",
                    contentRange)) {
                //match "HTTP 200 OK" header information
                //set file size
                filemodel.setFilesize(Integer.parseInt(httpContentRangeHeaderRE.group(3)));
                contentHeaderFound = true;
            }
            if (!contentHeaderFound) {
                FileEvent e = new FileEvent(this, filemodel, FileEvent.TRANSFER_ERRORED, "Invalid Response: No Content Length/Range Header Found!");
                fireFileTransferFinished(e);
                logTransferFinished(e);
                return;
            }
            //now fire file info changed to notify that some parts of the filemodel has been changed
            fireFileInfoChanged(new FileEvent(this, filemodel, FileEvent.FILEINFO_CHANGED));
            //now start the real file transfer part
            RandomAccessFile fileout = new RandomAccessFile(filemodel.getSaveLocation(), "rw");
            fileout.seek(filemodel.getCurrentFilesize());
            filemodel.setStartTime(System.currentTimeMillis());
            byte[] bytebuf = new byte[SOCKETBUF];
            int bytesread;
            TransferRateThrottler throttler = filemodel.getThrottler();
            while ((bytesread = bis.read(bytebuf, 0, SOCKETBUF)) != -1) {
                //shutdown detected, break out immediately
                fileout.write(bytebuf, 0, bytesread);
                filemodel.incrementCurrentFilesize(bytesread);
                fireFilesizeChanged(new FileEvent(this, filemodel, FileEvent.FILESIZE_CHANGED));
                //current filesize has reached project filesize.. stop now
                //otherwise, it could cause a security risk
                if (filemodel.getCurrentFilesize() >= filemodel.getFilesize()) break;
                if (shutdown) break;
                if (throttler != null)
                    throttler.throttle(filemodel);
                else
                    Thread.currentThread().yield();
            }
            //close the file, which also flushes the buffer
            fileout.close();
            filemodel.setEndTime(System.currentTimeMillis());
            if (shutdown) {
                FileEvent e = new FileEvent(this, filemodel, FileEvent.TRANSFER_CANCELLED);
                fireFileTransferFinished(e);
                logTransferFinished(e);
                return;
            }
            FileEvent e = new FileEvent(this, filemodel, FileEvent.TRANSFER_FINISHED);
            fireFileTransferFinished(e);
            logTransferFinished(e);
        } catch (IOException ex) {
            FileEvent e = new FileEvent(this, filemodel, FileEvent.TRANSFER_ERRORED, ex.getMessage());
            fireFileTransferFinished(e);
            logTransferFinished(e);
        } catch (TransferVetoException ex) {
            FileEvent e = new FileEvent(this, filemodel, FileEvent.TRANSFER_VETOED, ex.getMessage());
            fireFileTransferFinished(e);
            logTransferFinished(e);
        } finally {
            IOUtil.closeStream(bis);
            IOUtil.closeStream(bw);
        }
    }

    public void start() {
        shutdown = false;
    }

    public void shutdown() {
        shutdown = true;
        //interrupt just in case any throttlers are sleeping
        Thread.currentThread().interrupt();
    }
}
