package io.jenkins.plugins.noja;

import java.io.IOException;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Queue;
import hudson.model.Slave;
import hudson.model.Descriptor.FormException;
import hudson.model.queue.CauseOfBlockage;
import hudson.slaves.ComputerLauncher;

public class RelayControllerSlave extends Slave {

    private static final Logger LOGGER = Logger.getLogger(RelayControllerSlave.class.getName());

    private String hostName;
    private int portNumber;

    @DataBoundConstructor
    public RelayControllerSlave(String name, ComputerLauncher launcher) throws FormException, IOException {
        super(name, null, new ComputerLauncher() {
            public boolean isLaunchSupported() {
                return false;
            }
        });
        setNumExecutors(1);
    }

    @DataBoundSetter
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getHostName() {
        return hostName;
    }

    @DataBoundSetter
    public void setPortNumber(String portNumber) {
        this.portNumber = Integer.parseInt(portNumber);
    }

    public int getPortNumber() {
        return portNumber;
    }

    @Override
    public RelayControllerComputer createComputer() {
        return new RelayControllerComputer(this);
    }

    @Override
    public FilePath getRootPath() {
        return null;
    }
    
    @Override
    public CauseOfBlockage canTake(final Queue.BuildableItem item) {
        return new CauseOfBlockage() {
            public String getShortDescription() {
                return new String("Project " + item.getDisplayName() + " is not supported on " + getNodeName());
            }
        };
    }
    
    @Extension
    public static final class DescriptorImpl extends SlaveDescriptor {

        @Override
        public String getDisplayName() {
            // Return the display name shown in http://server/hudson/computers/new</tt> page as an agent type
            // The detail explanation for this agent type is loaded from
            // src/main/resources/io/jenkins/plugins/noja/RelayControllerSlave/newInstanceDetail.jelly
            return "Relay Controller";
        }
    }
}
