package com.echomine.example;

import com.echomine.gnutella.*;
import com.echomine.net.*;
import com.echomine.util.IPUtil;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;

public class GnutellaTest {
    private MsgInit msginit;
    private DefaultMessageListener mlistener;
    private GnutellaConnectionModel defaultModel;
    private GnutellaContext context;
    private String ip;

    public void setIP(String ip) {
        this.ip = ip;
    }

    protected void setUp() {
        try {
            context = new GnutellaContext((short)6347, 1500);
            //for testing purposes, we must set our User-Agent to something that other clients
            //will accept
            context.setSupportedFeatureHeader("User-Agent", "MorpheusOS 1.9.1.0");
            if (ip == null)
                ip = "127.0.0.1";
            defaultModel = new GnutellaConnectionModel(ip, 6346);
            mlistener = new DefaultMessageListener();
            msginit = new MsgInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void testConnectionManager() {
        GnutellaListenerRouter router = new GnutellaListenerRouter();
        final GnutellaConnectionManager manager = new GnutellaConnectionManager(context, router);
        manager.addConnectionListener(new ConnectionListener() {
            public void connectionStarting(ConnectionEvent e) throws ConnectionVetoException {
                System.out.println("Active: " + manager.getActiveConnectionsCount() + ", Established:" + manager.getEstablishedConnectionsCount());
            }

            public void connectionEstablished(ConnectionEvent e) {
                System.out.println("Active: " + manager.getActiveConnectionsCount() + ", Established:" + manager.getEstablishedConnectionsCount());
            }

            public void connectionClosed(ConnectionEvent e) {
                System.out.println("Active: " + manager.getActiveConnectionsCount() + ", Established:" + manager.getEstablishedConnectionsCount());
            }
        });
        try {
            manager.start();
            PerpetualSocketAcceptor listenerRouterAcceptor = new PerpetualSocketAcceptor(new ConnectionModel(6347));
            listenerRouterAcceptor.aaccept(router);
            manager.connect(defaultModel);
            Thread.currentThread().sleep(120000);
            listenerRouterAcceptor.close();
            manager.disconnect();
        } catch (Exception ex) {
        }
    }

    /**
     * Tests the listener router and see if it'll accept connections or not.
     * This tests actually just runs a router and listens for incoming connections.
     * An external gnutella client is used to connect to it and test it.
     * This test also only tests the Gnutella client connection only; it doesn't check the GIV and GET request handlers.
     */
    public void testListenerRouter() {
        GnutellaListenerRouter router = new GnutellaListenerRouter();
        GnutellaConnectionManager manager = new GnutellaConnectionManager(context, router);
        manager.addConnectionListener(new ConnectionCounterConnectionListener(manager));
        try {
            manager.start();
            PerpetualSocketAcceptor listenerRouterAcceptor = new PerpetualSocketAcceptor(new ConnectionModel(6347));
            listenerRouterAcceptor.aaccept(router);
            manager.connect(defaultModel);
            Thread.currentThread().sleep(120000);
            //shutdown
            listenerRouterAcceptor.close();
            manager.disconnect();
        } catch (Exception ex) {
        }
    }

    public void testDirectDownload() {
        GnutellaListenerRouter router = new GnutellaListenerRouter();
        GnutellaConnectionManager manager = new GnutellaConnectionManager(context, router);
        final GnutellaDownloadService downloadService = new GnutellaDownloadService(context, manager, router);
        downloadService.addConnectionListener(new DefaultConnectionListener());
        downloadService.addFileListener(new GnutellaFileTransferListener());
        //manager.addConnectionListener(clistener);
        manager.addMessageListener(
            new DefaultMessageListener() {
                private boolean once;
                public void messageReceived(GnutellaMessageEvent event) {
                    if (!once) {
                        super.messageReceived(event);
                        if (event.getMessage().getType() == GnutellaCode.QUERY_RESPONSE) {
                            MsgQueryResponse response = (MsgQueryResponse)event.getMessage();
                            if (response.getNumRecords() > 0) {
                                MsgResRecord resrec = response.getMsgRecord(0);
                                //submit a download request
                                try {
                                    System.out.println("Dowload info: " + response.getRemoteHost().getHostAddress() + ":" +
                                        response.getRemotePort() + ",filename=" + resrec.getFilename() + ",fileindex=" +
                                        resrec.getFileIndex());
                                    if (!IPUtil.isHostIPPrivate(response.getRemoteHost().getHostAddress())) {
                                        downloadService.getFile(response.getRemoteClientID(), response.getRemoteHost().getHostAddress(),
                                            response.getRemotePort(),
                                            new GnutellaFileModel(resrec.getFilename(), "test.mp3", resrec.getFileIndex()),
                                            null, null);
                                        once = true;
                                    }
                                } catch (UnknownHostException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }
                }
            });
        //go online first
        manager.start();
        try {
            manager.connect(defaultModel);
            Thread.currentThread().sleep(3000);
            MsgQuery query = new MsgQuery();
            query.setSearchString("britney mp3");
            manager.send(query);
            Thread.currentThread().sleep(90000);
            manager.disconnect();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * This tests the firewalled download behavior by creating a request listener
     * that listens on a port for incoming requests.  Then it does a search, and then
     * submits a firewalled download request.  The remote should connect to us and send us a GIV request.
     */
    public void testFirewalledDownload() {
        GnutellaListenerRouter router = new GnutellaListenerRouter();
        GnutellaConnectionManager manager = new GnutellaConnectionManager(context, router);
        final GnutellaDownloadService downloadService = new GnutellaDownloadService(context, manager, router);
        downloadService.addConnectionListener(new DefaultConnectionListener());
        downloadService.addFileListener(new GnutellaFileTransferListener());
        manager.addConnectionListener(new ConnectionCounterConnectionListener(manager));
        manager.addMessageListener(
            new DefaultMessageListener() {
                private int count;
                public void messageReceived(GnutellaMessageEvent event) {
                    if (count < 2) {
                        super.messageReceived(event);
                        if (event.getMessage().getType() == GnutellaCode.QUERY_RESPONSE) {
                            MsgQueryResponse response = (MsgQueryResponse)event.getMessage();
                            if (response.getNumRecords() > 0) {
                                MsgResRecord resrec = response.getMsgRecord(0);
                                //submit a download request
                                try {
                                    System.out.println("Dowload info: " + response.getRemoteHost().getHostAddress() + ":" +
                                        response.getRemotePort() + ",filename=" + resrec.getFilename() + ",fileindex=" +
                                        resrec.getFileIndex());
                                    if (!IPUtil.isHostIPPrivate(response.getRemoteHost().getHostAddress())) {
                                        downloadService.getFileThroughFirewall(response.getRemoteClientID(),
                                            new GnutellaFileModel(resrec.getFilename(), "test.mp3", resrec.getFileIndex()),
                                            null, null);
                                        count++;
                                    }
                                } catch (UnknownHostException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }
                }
            });
        //go online first
        manager.start();
        try {
            PerpetualSocketAcceptor listenerRouterAcceptor = new PerpetualSocketAcceptor(new ConnectionModel(context.getPort()));
            listenerRouterAcceptor.aaccept(router);
            manager.connect(defaultModel);
            Thread.currentThread().sleep(3000);
            MsgQuery query = new MsgQuery();
            query.setSearchString("backstreet mp3");
            manager.send(query);
            Thread.currentThread().sleep(90000);
            //shutdown
            listenerRouterAcceptor.close();
            manager.disconnect();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** tests the main Gnutella class and see if everything is working correctly with all the functions. */
    public void testGnutella() {
        final Gnutella gnutella = new Gnutella(context);
        gnutella.getDownloadService().addConnectionListener(new DefaultConnectionListener());
        gnutella.getDownloadService().addFileListener(new GnutellaFileTransferListener());
        gnutella.getConnectionManager().addConnectionListener(new ConnectionCounterConnectionListener(gnutella.getConnectionManager()));
        GnutellaSearchListener slistener = new GnutellaSearchListener() {
            private boolean once;
            public void searchResultReceived(GnutellaSearchEvent event, MsgQueryResponse response) {
                if (!once) {
                    if (response.getNumRecords() > 0) {
                        MsgResRecord resrec = response.getMsgRecord(0);
                        //submit a download request
                        try {
                            System.out.println("Dowload info: " + response.getRemoteHost().getHostAddress() + ":" +
                                response.getRemotePort() + ",filename=" + resrec.getFilename() + ",fileindex=" +
                                resrec.getFileIndex() + ",clientID=" + response.getRemoteClientID());
                            if (!IPUtil.isHostIPPrivate(response.getRemoteHost().getHostAddress())) {
                                gnutella.getDownloadService().getFile(response.getRemoteClientID(), response.getRemoteHost().getHostAddress(),
                                    response.getRemotePort(),
                                    new GnutellaFileModel(resrec.getFilename(), "test.mp3", resrec.getFileIndex()),
                                    null, null);
                                once = true;
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        };
        //go ONLINE
        gnutella.start();
        try {
            gnutella.connect(defaultModel);
            Thread.currentThread().sleep(15000);
            gnutella.getSearchService().search("depeche mp3", (short)0, slistener);
            Thread.currentThread().sleep(120000);
            gnutella.disconnect();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** tests the search service class and see if everything is working correctly with all the functions. */
    public void testSearchService() {
        final Gnutella gnutella = new Gnutella(context);
        //gnutella.getSearchService().addSearchListener(new DefaultSearchListener());
        //go ONLINE
        gnutella.start();
        try {
            gnutella.connect(defaultModel);
            Thread.currentThread().sleep(15000);
            System.out.println("Sending out queries");
            MsgQuery query = new MsgQuery("depeche mp3", (short)0);
            gnutella.getSearchService().search(query, new DefaultSearchListener("Depeche"));
            query = new MsgQuery("britney mp3", (short)0);
            gnutella.getSearchService().search(query, new DefaultSearchListener("Britney"));
            Thread.currentThread().sleep(120000);
            gnutella.disconnect();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** tests direct upload by listening for sharing requests */
    public void testDirectUpload() {
        GnutellaContext ctx = new GnutellaContext((short)6347, 1500);
        ctx.setShareFileController(new TestShareList());
        //set context to use our own shareList
        final Gnutella gnutella = new Gnutella(ctx);
        gnutella.getShareService().addConnectionListener(new DefaultConnectionListener());
        gnutella.getShareService().addFileListener(
            new GnutellaFileTransferListener() {
                public void fileTransferStarting(FileEvent e) throws TransferVetoException {
                    //do nothing as save location is where the real file is located
                }
            });
        //go ONLINE
        gnutella.start();
        try {
            Thread.currentThread().sleep(360000);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        gnutella.disconnect();
    }

    /** tests firewalled uploads by listening for sharing requests */
    public void testFirewalledUpload() {
        //firewalled so set port to 0
        GnutellaContext ctx = new GnutellaContext((short)0, 1500);
        ctx.setShareFileController(new TestShareList());
        //set context to use our own shareList
        final Gnutella gnutella = new Gnutella(ctx);
        gnutella.getShareService().addConnectionListener(new DefaultConnectionListener());
        gnutella.getShareService().addFileListener(
            new GnutellaFileTransferListener() {
                public void fileTransferStarting(FileEvent e) throws TransferVetoException {
                    //do nothing as save location is where the real file is located
                }
            });
        //go ONLINE
        gnutella.start();
        try {
            gnutella.connect(defaultModel);
            Thread.currentThread().sleep(360000);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        gnutella.disconnect();
    }

    public void testStats() {
        Gnutella gnutella = new Gnutella(context);
        gnutella.getConnectionManager().addConnectionListener(new ConnectionCounterConnectionListener(gnutella.getConnectionManager()));
        gnutella.getStatistics().addStatisticsListener(new DefaultStatListener());
        //go ONLINE
        gnutella.start();
        try {
            gnutella.connect(defaultModel);
            Thread.currentThread().sleep(60000);
            //get stats
            gnutella.getStatistics().resetHostStats();
            Thread.currentThread().sleep(120000);
            gnutella.disconnect();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: GnutellaTest <method to run> [<initial host to connect>]");
            System.exit(1);
        }
        GnutellaTest test = new GnutellaTest();
        if (args.length == 2)
            test.setIP(args[1]);
        test.setUp();
        //reflection and call the method
        try {
            Class tclass = test.getClass();
            Method method = tclass.getMethod(args[0], null);
            method.invoke(test, null);
        } catch (NoSuchMethodException ex) {
            System.out.println("Specified method doesn't exist");
        } catch (IllegalAccessException ex) {
            System.out.println("Illegal access to method");
        } catch (InvocationTargetException ex) {
            System.out.println("Error when invoking method");
        }
    }

    class DefaultConnectionListener implements ConnectionListener {
        public void connectionStarting(ConnectionEvent e) throws ConnectionVetoException {
            System.out.println("STARTING: " + ((GnutellaConnectionModel)e.getConnectionModel()).getConnectionTypeString() +
                " " + e.getConnectionModel());
        }

        public void connectionEstablished(ConnectionEvent e) {
            System.out.println("ESTABLISHED: " + ((GnutellaConnectionModel)e.getConnectionModel()).getConnectionTypeString() +
                " " + e.getConnectionModel());
        }

        public void connectionClosed(ConnectionEvent e) {
            System.out.println("CLOSED: " + ((GnutellaConnectionModel)e.getConnectionModel()).getConnectionTypeString() + " " +
                e.getConnectionModel() + " (Online " + e.getConnectionModel().getTimeOnlineString() + ")");
        }
    }


   class ConnectionCounterConnectionListener extends DefaultConnectionListener {
       private GnutellaConnectionManager manager;

       public ConnectionCounterConnectionListener(GnutellaConnectionManager manager) {
           this.manager = manager;
       }

       public void connectionStarting(ConnectionEvent e) throws ConnectionVetoException {
           super.connectionStarting(e);
           System.out.println("Active: " + manager.getActiveConnectionsCount() + ", Established:" + manager.getEstablishedConnectionsCount());
       }

       public void connectionEstablished(ConnectionEvent e) {
           super.connectionEstablished(e);
       }

       public void connectionClosed(ConnectionEvent e) {
           super.connectionClosed(e);
           System.out.println("Active: " + manager.getActiveConnectionsCount() + ", Established:" + manager.getEstablishedConnectionsCount());
       }
   }

    class DefaultMessageListener implements GnutellaMessageListener {
        public void messageReceived(GnutellaMessageEvent event) {
            //if (event.getMessage().getType() == GnutellaCode.QUERY_RESPONSE)
            //	System.out.println("Message: " + event.getMessage());
        }
    }


    class GnutellaFileTransferListener implements GnutellaFileListener {
        long lastTime;

        public void filesizeChanged(FileEvent e) {
            //only report filesize changes once per second
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastTime < 1000) return;
            lastTime = currentTime;
            System.out.print("BPS: " + e.getFileModel().getTransferBPS() + ", ETA: " + e.getFileModel().getTimeLeft() +
                ", Downloading " + e.getFileModel().getCurrentFilesize() + " / " + e.getFilesize() + "             \r");
        }

        public void fileInfoChanged(FileEvent e) {
            System.out.println("File Info Changed: filename=" + e.getFilename() + ",filesize=" + e.getFilesize() +
                ",save location=" + e.getSaveLocation());
        }

        public void fileTransferFinished(FileEvent e) {
            if (e.getStatus() == FileEvent.TRANSFER_ERRORED) {
                System.out.println("File Transfer Errored: " + e.getErrorMessage());
            } else {
                System.out.println("File Transfer finished with code: " + e.getStatus());
            }
        }

        public void fileTransferStarting(FileEvent e) throws TransferVetoException {
            System.out.println("File transfer starting");
            e.getFileModel().setSaveLocation("test.mp3");
            File file = new File("test.mp3");
            //check to see if the file exists already
            //remove if it exists
            if (file.exists()) {
                file.delete();
            }
        }
    }


    class DefaultSearchListener implements GnutellaSearchListener {
        String text;

        public DefaultSearchListener(String text) {
            this.text = text;
        }

        public void searchResultReceived(GnutellaSearchEvent event, MsgQueryResponse msg) {
            //just print out the search result
            if (msg.getNumRecords() > 0) {
                MsgResRecord resrec = msg.getMsgRecord(0);
                //submit a download request
                try {
                    System.out.println(text + " Query Hit [" + msg.getHeader().getMsgID() + "]: " +
                        msg.getRemoteHost().getHostAddress() + ":" + msg.getRemotePort() + ",filename=" +
                        resrec.getFilename() + ",fileindex=" + resrec.getFileIndex() + ",clientID=" + msg.getRemoteClientID());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }


    class DefaultStatListener implements GnutellaStatisticsListener {
        private int total = 0;

        public void globalStatsUpdated(GnutellaStatistics stats) {
        }

        public void connectionStatsUpdated(GnutellaConnection conn) {
            //print out global stats
            //System.out.print("Msgs: " + stats.getTotalMessages() + ", Files: " + stats.getTotalFiles()
            //    + ", Size: " + stats.getTotalSize() + ", Hosts: " + stats.getTotalHosts() + "\r");
            //Print out connection specific stats

                        if (conn == null) return;
            System.out.println("[Connection " + conn.getConnectionModel() + "]Msgs: " +
            conn.getMessages() + ", hosts: " + conn.getHosts() + ", Files: " + conn.getFiles() +
            ", Size: " + conn.getSize() + "\r");

        }
    }


    class TestShareList implements ShareFileController {
        ArrayList list = new ArrayList(1);
        String FILENAME = "britney-nipple.mpg";
        String FILEPATH = "z:\\generic-master.mp3";

        public TestShareList() {
            ShareFile file = new ShareFile(1, FILENAME, 4000000, null);
            list.add(file);
        }

        public String getFilePath(int fileidx) {
            //return the same file
            return FILEPATH;
        }

        public Collection getFiles(MsgQuery query) {
            return list;
        }

        public int getFileCount() {
            return 1;
        }

        public int getTotalSize() {
            return 4000000;
        }

        public int getMaxUploads() {
            return 1;
        }

        public String getFilename(int fileidx) {
            return FILENAME;
        }
    }
}
