package io.jenkins.plugins.noja;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import hudson.model.Slave;
import hudson.slaves.SlaveComputer;
import hudson.util.Futures;

public class RelayControllerComputer extends SlaveComputer {

    private static final Logger LOGGER = Logger.getLogger(RelayControllerComputer.class.getName());

    private boolean offline = true;

    public RelayControllerComputer(Slave slave) {
        super(slave);
    }

    protected static List<String> requests;
    static {
        requests = new ArrayList<String>();
        // request to connect and set CmsSerialFrameRxTimeout 0x01c6
        requests.add("05 00 15 00 4e f0 ad e0 01 00 09 04 00 06 01 c6 00 00 00 01 d4 23 1e 2a");
        // ack and request to get relay version 0x085c
        requests.add("f0 ad e1 02 00 00 f8 4f 9b 73 f0 ad e0 03 00 05 05 00 02 08 5c da 53 e0 bd");
        // ack and request to get serial number 0x85a
        requests.add("f0 ad e1 04 00 00 55 1b 2a 43 f0 ad e0 05 00 05 05 00 02 08 5a e2 72 d5 df");
    }

    protected void sendRequest(String hostName, int portNumber, List<String> requests, List<String> replies) {
        try {
            Socket clientSocket = new Socket(hostName, portNumber);
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            for (String request : requests) {
                LOGGER.info("Send: " + request);
                String data[] = request.split("\\s+");
                for (int i = 0; i < data.length; i++) {
                    out.writeByte(Integer.valueOf(data[i], 16));
                }
                out.flush();
                int retry = 0;
                StringBuffer reply = new StringBuffer();
                while (retry < 5) {
                    int value = in.read();
                    if (value >= 0) {
                        reply.append(String.format("%02x ", value));
                    } else {
                        if (reply.length() > 0) {
                            LOGGER.info("Received: " + reply.toString());
                            break;
                        }
                        retry++;
                        Thread.sleep(300);
                    }
                }
                replies.add(reply.toString());
            }
            clientSocket.close();
        } catch (Exception e) {
            LOGGER.warning("Failed to connect to Relay Controller @ " + hostName + ":" + portNumber);
            LOGGER.warning(e.getMessage());
        } finally {
        }
    }

    @Override
    protected Future<?> _connect(boolean forceReconnect) {
        LOGGER.info("Connect to Relay Controller " + this.getDisplayName());
        Slave slave = this.getNode();
        RelayControllerSlave relayControllerSlave = null;
        if (slave instanceof RelayControllerSlave) {
            relayControllerSlave = (RelayControllerSlave) slave;
        }
        if (relayControllerSlave == null) {
            return Futures.precomputed(null);
        }
        String hostName = relayControllerSlave.getHostName();
        int portNumber = relayControllerSlave.getPortNumber();
        ArrayList<String> replies = new ArrayList<String>();
        sendRequest(hostName, portNumber, requests, replies);
        if (replies.size() > 0) {
            offline = false;
        }
        return Futures.precomputed(null);
    }

    @Override
    public boolean isOffline() {
        return offline;
    }

    @Override
    public Boolean isUnix() {
        return true;
    }

}
