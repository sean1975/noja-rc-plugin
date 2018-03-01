package io.jenkins.plugins.noja;

import java.util.HashMap;
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
        HashMap<Integer, String> dataPointMap = new HashMap<Integer, String>();
        if (CMSChannel.connect(hostName, portNumber, dataPointMap)) {
            offline = false;
            LOGGER.info("Serial Number: " + dataPointMap.get(CMSChannel.IdRelayNumber));
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
