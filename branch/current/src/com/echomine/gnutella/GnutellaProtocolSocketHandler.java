package com.echomine.gnutella;

import com.echomine.net.HandshakeableSocketHandler;
import com.echomine.util.HTTPHeader;

public interface GnutellaProtocolSocketHandler extends HandshakeableSocketHandler {
  /** queues up the data and wait for thread to send out the data */
  void send(GnutellaMessage msg);

  /** retrieves the protocol type. The types are listed in GnutellaProtocolType */
  int getProtocolType();

  /**
   * @return the remote supported feature headers. null if none exists or not supported
   */
  HTTPHeader getSupportedFeatureHeaders();

  /**
   * @return the remote vendor feature headers. null if none exists or not supported
   */
  HTTPHeader getVendorFeatureHeaders();
}
