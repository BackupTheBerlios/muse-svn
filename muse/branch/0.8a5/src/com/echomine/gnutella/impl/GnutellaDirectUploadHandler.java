package com.echomine.gnutella.impl;

import com.echomine.gnutella.GnutellaContext;
import com.echomine.gnutella.GnutellaFileHandler;
import com.echomine.gnutella.GnutellaFileModel;
import com.echomine.net.FileEvent;
import com.echomine.net.FileModel;
import com.echomine.net.TransferRateThrottler;
import com.echomine.net.TransferVetoException;
import com.echomine.util.HTTPHeader;
import com.echomine.util.HTTPRangeHeader;
import com.echomine.util.HTTPResponseHeader;
import com.echomine.util.IOUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;

/**
 * Handles all direct uploads.  The first GET request line is already parsed by the GetRequestHandler and the information is
 * stored in the file model already.  Thus, all that's need to be parsed by this object is the rest of the HTTP headers.
 * The file model is used a little differently from all the other file transfer handlers.  In
 * this particular handler, the file model's filename is the PUBLISHED filename (the name sent out for gnutella search
 * requests).  The file model's save location is the ACTUAL file path (path + filename) of the stored file.  Thus, the save
 * location stores the file where you should be opening while the file model's filename is simply the alias for the real file.
 */
public class GnutellaDirectUploadHandler extends GnutellaFileHandler {
    protected static final int SOCKETBUF = 8192;
    private boolean shutdown;

    public GnutellaDirectUploadHandler(GnutellaContext context, FileModel model) {
        super(context, model);
    }

    public void handle(Socket socket) throws IOException {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        //obtain a filemodel first
        GnutellaFileModel filemodel = (GnutellaFileModel) model;
        try {
            bis = new BufferedInputStream(socket.getInputStream(), SOCKETBUF);
            bos = new BufferedOutputStream(socket.getOutputStream(), SOCKETBUF);
            //fire transfer starting so that the transfer is authorized to continue
            fireFileTransferStarting(new FileEvent(this, filemodel, FileEvent.TRANSFER_STARTING), new FileEvent(this, filemodel, FileEvent.TRANSFER_VETOED));
            logTransferStarting();
            //get request is already consumed by the command handler
            //so now let's just retrieve the headers only
            //need to retrieve the Range header to know about file offset resumes
            //note that we don't care about the end because we're gonna send till
            //the end of the file
            HTTPRangeHeader range = new HTTPRangeHeader();
            HTTPHeader header = new HTTPHeader();
            header.parseHeaders(bis);
            //save the response header
            setResponseHeaders(header);
            //if no Range, then assume it starts from 0
            if (header.getHeader("Range") == null) {
                filemodel.setCurrentFilesize(0);
            } else if (range.parse(header.getHeader("Range"))) {
                //Range header exists and parsing is successful
                //range is valid
                long pos = range.getStart();
                if (pos == -1) //start at beginning
                    filemodel.setCurrentFilesize(0);
                else
                    filemodel.setCurrentFilesize(pos);
            } else {
                //range header is invalid
                //return error
                bos.write(createErrorHeader(503, "Invalid Range Header").getBytes());
                bos.flush();
                //fire transfer finished
                FileEvent e = new FileEvent(this, filemodel, FileEvent.TRANSFER_ERRORED, "Invalid Range Header");
                fireFileTransferFinished(e);
                logTransferFinished(e);
                return;
            }
            //do a periodic check to see if caller shut us down
            if (shutdown) {
                FileEvent e = new FileEvent(this, filemodel, FileEvent.TRANSFER_CANCELLED);
                fireFileTransferFinished(e);
                logTransferFinished(e);
                return;
            }
            RandomAccessFile filein = new RandomAccessFile(filemodel.getSaveLocation(), "r");
            //if resume offset is greater than file size, then just return
            //remember that currentfilesize is inclusive and starts at 0
            if (filemodel.getCurrentFilesize() >= filein.length()) {
                //fire transfer finished since it's considered that going beyond
                bos.write(createErrorHeader(503, "Invalid Request").getBytes());
                bos.flush();
                FileEvent e = new FileEvent(this, filemodel, FileEvent.TRANSFER_ERRORED, "resume offset > file size");
                fireFileTransferFinished(e);
                logTransferFinished(e);
                return;
            }
            filemodel.setFilesize(filein.length());
            //now fire file info changed to notify that some parts of the model has been changed
            fireFileInfoChanged(new FileEvent(this, filemodel, FileEvent.FILEINFO_CHANGED));
            //write out success response header
            HTTPResponseHeader response = new HTTPResponseHeader();
            response.setStatus(200, "OK");
            response.setHeader("Server", getContext().getSupportedFeatureHeaders().getHeader("User-Agent"));
            response.setHeader("Content-type", "application/binary");
            response.setHeader("Content-Range", "bytes=" + filemodel.getCurrentFilesize() + "-" +
                    (filemodel.getFilesize() - 1) + "/" + filemodel.getFilesize());
            response.setHeader("Content-Length", Long.toString(filemodel.getFilesize()));
            bos.write(response.toString().getBytes());
            bos.flush();
            //now start the real file transfer part
            filein.seek(filemodel.getCurrentFilesize());
            filemodel.setStartTime(System.currentTimeMillis());
            byte[] bytebuf = new byte[SOCKETBUF];
            int bytesread;
            TransferRateThrottler throttler = filemodel.getThrottler();
            while ((bytesread = filein.read(bytebuf, 0, SOCKETBUF)) != -1) {
                bos.write(bytebuf, 0, bytesread);
                filemodel.incrementCurrentFilesize(bytesread);
                fireFilesizeChanged(new FileEvent(this, filemodel, FileEvent.FILESIZE_CHANGED));
                //current filesize has reached real filesize.. stop now
                //otherwise, it could cause a security risk
                if (filemodel.getCurrentFilesize() >= filemodel.getFilesize()) break;
                if (shutdown) break;
                if (throttler != null)
                    throttler.throttle(filemodel);
                else
                    Thread.currentThread().yield();
            }
            //flush output stream and close file
            bos.flush();
            filein.close();
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
            IOUtil.closeStream(bos);
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
