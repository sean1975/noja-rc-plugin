package io.jenkins.plugins.noja;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.remoting.VirtualChannel;
import hudson.slaves.OfflineCause;
import hudson.slaves.RetentionStrategy;
import hudson.util.Futures;

public class RelayControllerComputer extends Computer {

    private static final Logger LOGGER = Logger.getLogger(RelayControllerComputer.class.getName());

    private CMSChannel channel;

    public RelayControllerComputer(Slave slave) {
        super(slave);
    }

    @Override   
    public synchronized VirtualChannel getChannel() {
        if (channel == null) {
            Node slave = getNode();
            if (slave instanceof RelayControllerSlave) {
                RelayControllerSlave relayControllerSlave = (RelayControllerSlave) slave;
                String hostName = relayControllerSlave.getHostName();
                int portNumber = relayControllerSlave.getPortNumber();
                try {
                    channel = new CMSChannel(hostName, portNumber);
                } catch (Exception e) {
                    LOGGER.warning(e.getMessage());
                }
                if (!channel.connect()) {
                    LOGGER.warning("Failed to connect to " + relayControllerSlave.getNodeName());
                }
            }
          
        }
        return channel;
    }
    
    @Override
    protected Future<?> _connect(boolean forceReconnect) {
        LOGGER.info("Connect to Relay Controller " + this.getDisplayName());
        Node node = this.getNode();
        RelayControllerSlave relayControllerSlave = null;
        if (node instanceof RelayControllerSlave) {
            relayControllerSlave = (RelayControllerSlave) node;
        }
        if (relayControllerSlave == null) {
            return Futures.precomputed(null);
        }
        LOGGER.info("Connecting to Relay Controller " + this.getDisplayName());
        CMSChannel channel = (CMSChannel) getChannel();
        if (channel == null) {
            return Futures.precomputed(null);
        }
        return Futures.precomputed(null);
    }

    @Override
    public Future<?> disconnect(OfflineCause cause) {
        LOGGER.info("disconnect(" + cause.toString() + ")");
        super.disconnect(cause);
        return Futures.precomputed(null);
    }
    
    @Override
    public Boolean isUnix() {
        return true;
    }

    @Override
    public Charset getDefaultCharset() {
        return Charset.forName("UTF-8");
    }

    @Override
    public List<LogRecord> getLogRecords() throws IOException, InterruptedException {
         return null;
    }

    @Override
    public void doLaunchSlaveAgent(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
    }

    @Override
    public boolean isConnecting() {
        return false;
    }

    @Override
    public RetentionStrategy getRetentionStrategy() {
        return RetentionStrategy.NOOP;
    }

    @Override
    public List<AbstractProject> getTiedJobs() {
        Node node = getNode();
        if (node instanceof RelayControllerSlave) {
            RelayControllerSlave slave = (RelayControllerSlave) node;
            return slave.getTiedJobs();
        }
        return Collections.EMPTY_LIST;
    }
}
