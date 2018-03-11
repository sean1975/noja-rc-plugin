package io.jenkins.plugins.noja;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    // Methods to access member variable channel must be synchronized
    private CMSChannel channel;
    private Future<?> lastAttempt;
    private ExecutorService executor;

    public RelayControllerComputer(Slave slave) {
        super(slave);
        executor = Executors.newSingleThreadExecutor();
    }

    public void createChannel() {
        synchronized (this) {
            RelayControllerSlave relayControllerSlave = (RelayControllerSlave) getNode();
            String hostName = relayControllerSlave.getHostName();
            int portNumber = relayControllerSlave.getPortNumber();
            CMSChannel newChannel = null;
            try {
                newChannel = new CMSChannel(hostName, portNumber);
            } catch (Exception e) {
                LOGGER.warning(e.getMessage());
            }
            if (!newChannel.connect()) {
                LOGGER.warning("Failed to connect to " + relayControllerSlave.getNodeName());
            } else {
                LOGGER.info("Connected to " + relayControllerSlave.getNodeName());
                channel = newChannel;
            }
        }
    }
    
    @Override   
    public VirtualChannel getChannel() {
        synchronized (this) {
            return channel;
        }
    }
    
    @Override
    protected Future<?> _connect(boolean forceReconnect) {
        //LOGGER.info("Connect to Relay Controller " + this.getDisplayName());
        Node node = this.getNode();
        RelayControllerSlave relayControllerSlave = null;
        if (node instanceof RelayControllerSlave) {
            relayControllerSlave = (RelayControllerSlave) node;
        }
        if (relayControllerSlave == null) {
            return Futures.precomputed(null);
        }
        LOGGER.info("Connecting to Relay Controller " + this.getDisplayName() + " forceReconnect = " + (forceReconnect ? "true" : "false"));
        lastAttempt = executor.submit(new Runnable() {
            @Override
            public void run() {
                createChannel();                
            } 
        });
        return lastAttempt;
    }

    @Override
    public Future<?> disconnect(OfflineCause cause) {
        LOGGER.info("disconnect(" + cause.toString() + ")");
        super.disconnect(cause);
        executor.shutdownNow();
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
        LOGGER.info("doLaunchSlaveAgent: " + req.getPathInfo());
    }

    @Override
    public boolean isConnecting() {
        if (lastAttempt == null || lastAttempt.isDone() || lastAttempt.isCancelled()) {
            return false;
        }
        return true;
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
