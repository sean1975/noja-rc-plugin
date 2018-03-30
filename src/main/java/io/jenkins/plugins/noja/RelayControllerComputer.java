package io.jenkins.plugins.noja;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    private transient int numRetryAttempt;
    private ExecutorService executor;

    public RelayControllerComputer(Slave slave) {
        super(slave);
        executor = Executors.newSingleThreadExecutor();
    }

    public void updateNodeEnvVars() {
        RelayControllerSlave relayControllerSlave = (RelayControllerSlave) getNode();
        synchronized (this) {
            if (channel != null) {
                LOGGER.info("Update environment variables for " + relayControllerSlave.getDisplayName());
                Map<String, String> envVars = relayControllerSlave.getEnvVars();
                envVars.put("RC_SERIALNUMBER", channel.getSerialNumber());
                envVars.put("RC_SOFTWAREVERSION", channel.getSoftwareVersion());
                envVars.put("RC_HARDWAREVERSION", channel.getHardwareVersion());
            } else {
                LOGGER.info("Channel has not been created for " + relayControllerSlave.getDisplayName());;
            }
        }
    }
    
    public boolean createChannel() {
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
            return false;
        } else {
            LOGGER.info("Connected to " + relayControllerSlave.getNodeName());
            setChannel(newChannel);
            updateNodeEnvVars();
            return true;
        }
    }
    
    protected void setChannel(CMSChannel channel) {
        synchronized (this) {
            this.channel = channel;
        }
    }
    
    @Override   
    public VirtualChannel getChannel() {
        synchronized (this) {
            if (channel == null) {
                connect(false);
            }
            return channel;
        }
    }
    
    @Override
    protected Future<?> _connect(boolean forceReconnect) {
        //LOGGER.info("Connect to Relay Controller " + this.getDisplayName());
        if (!forceReconnect && isConnecting()) {
            return lastAttempt;
        }
        if (!forceReconnect && numRetryAttempt>6 && (numRetryAttempt%60)!=0) {
            return Futures.precomputed(null);
        }
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
                if (createChannel()) {
                    numRetryAttempt = 0;
                } else {
                    numRetryAttempt++;
                }
            } 
        });
        return lastAttempt;
    }

    @Override
    public Future<?> disconnect(OfflineCause cause) {
        LOGGER.info("disconnect " + this.getDisplayName() + " (" + cause.toString() + ")");
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
