package io.jenkins.plugins.noja;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.remoting.VirtualChannel;
import hudson.slaves.OfflineCause;
import hudson.slaves.RetentionStrategy;
import hudson.util.Futures;
import jenkins.model.Jenkins;

public class RelayControllerComputer extends Computer {

    private static final Logger LOGGER = Logger.getLogger(RelayControllerComputer.class.getName());

    private String serialNumber;
    private String softwareVersion;
    private CMSChannel channel;

    public RelayControllerComputer(Slave slave) {
        super(slave);
        channel = new CMSChannel(this);
    }
    
    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }
    
    public String getSerialNumber() {
        return serialNumber;
    }
    
    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
    }
    
    public String getSoftwareVersion() {
        return softwareVersion;
    }
    
    @Override   
    public VirtualChannel getChannel() {
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
        if (channel.connect()) {
            LOGGER.info("Serial Number: " + getSerialNumber());
            LOGGER.info("Software Version: " + getSoftwareVersion());
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
        List<AbstractProject> r = new ArrayList<AbstractProject>();
        System.out.println("Searching for tired jobs for node " + node.getDisplayName());
        for (AbstractProject<?,?> p : Jenkins.getInstance().getAllItems(AbstractProject.class)) {
            for (RelayControllerProperty property : Util.filter(p.getAllProperties(), RelayControllerProperty.class)) {
                if (property.getRelayControllerName().compareToIgnoreCase(node.getNodeName()) == 0) {
                    System.out.println(p.getDisplayName() + " is tired to node " + node.getNodeName());
                    r.add(p);
                    break;
                }
            }
        }
        return r.isEmpty() ? Collections.EMPTY_LIST : r;
    }
}
