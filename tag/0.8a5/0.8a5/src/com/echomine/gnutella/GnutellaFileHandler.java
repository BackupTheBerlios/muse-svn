package com.echomine.gnutella;

import com.echomine.net.AbstractFileHandler;
import com.echomine.net.FileModel;
import com.echomine.net.FileEvent;
import com.echomine.util.HTTPHeader;
import com.echomine.util.HTTPResponseHeader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;

public abstract class GnutellaFileHandler extends AbstractFileHandler {
    private static Log dllogger = LogFactory.getLog("gnutella/file/download");
    private static Log ullogger = LogFactory.getLog("gnutella/file/upload");
    private GnutellaContext context;
    private HTTPHeader response;

    public GnutellaFileHandler(GnutellaContext context, FileModel model) {
        super(model);
        this.context = context;
    }

    protected String createErrorHeader(int code, String msg) {
        HTTPResponseHeader response = new HTTPResponseHeader();
        response.setStatus(code, msg);
        response.setHeader("Server", context.getSupportedFeatureHeaders().getHeader("User-Agent"));
        return response.toString();
    }

    /**
     * @return the response headers that the remote sent to us when requesting for the file
     */
    public HTTPHeader getResponseHeaders() {
        return response;
    }

    /**
     * sets the response header that the remote connection sent to us
     */
    protected void setResponseHeaders(HTTPHeader header) {
        this.response = header;
    }

    protected GnutellaContext getContext() {
        return context;
    }

    /**
     * Logs the transfer starting info
     */
    protected void logTransferStarting() {
        GnutellaFileModel fmodel = (GnutellaFileModel) getModel();
        if (fmodel.getConnectionModel().getConnectionType() == GnutellaConnectionModel.INCOMING)
            if (dllogger.isInfoEnabled())
                dllogger.info("Initiating file transfer with " + fmodel.getConnectionModel() +
                    " for /" + fmodel.getFileIndex() + "/" + fmodel.getFilename());
        else
            if (ullogger.isInfoEnabled())
                ullogger.info("Initiating file transfer with " + fmodel.getConnectionModel() +
                    " for /" + fmodel.getFileIndex() + "/" + fmodel.getFilename());
    }

    /**
     * logs the transfer finished info
     */
    protected void logTransferFinished(FileEvent event) {
        GnutellaFileModel fmodel = (GnutellaFileModel) getModel();
        Log logger;
        if (fmodel.getConnectionModel().getConnectionType() == GnutellaConnectionModel.INCOMING)
            logger = dllogger;
        else
            logger = ullogger;
        if (logger.isInfoEnabled()) {
            StringBuffer buf = new StringBuffer(100);
            buf.append("File transfer ");
            switch(event.getStatus()) {
                case FileEvent.TRANSFER_FINISHED:
                    buf.append("finished ");
                    break;
                case FileEvent.TRANSFER_ERRORED:
                    buf.append("errored ");
                    break;
                case FileEvent.TRANSFER_CANCELLED:
                    buf.append("cancelled ");
                    break;
                case FileEvent.TRANSFER_VETOED:
                    buf.append("vetoed ");
                    break;
                case FileEvent.TRANSFER_QUEUED:
                    buf.append("queued ");
                    break;
                default:
                    buf.append("finished (status " + event.getStatus() + ") ");
            }
            buf.append("with " + fmodel.getConnectionModel() + " for /" + fmodel.getFileIndex() +
                "/" + fmodel.getFilename());
            if (event.getErrorMessage() != null)
                buf.append(" (" + event.getErrorMessage() + ")");
            logger.info(buf.toString());
            if (logger.isDebugEnabled()) {
                //log the remote headers
                HTTPHeader headers = getResponseHeaders();
                if (headers != null) {
                    Iterator iter = headers.getHeaderNames().iterator();
                    String header, value;
                    while (iter.hasNext()) {
                        header = (String) iter.next();
                        value = headers.getHeader(header);
                        logger.debug("Header " + header + ": " + value);
                    }
                }
            }
        }
    }
}
