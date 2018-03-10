package io.jenkins.plugins.noja;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Queue;
import hudson.model.Slave;
import hudson.model.Descriptor.FormException;
import hudson.model.queue.CauseOfBlockage;
import hudson.model.queue.SubTask;
import hudson.slaves.ComputerLauncher;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;

public class RelayControllerSlave extends Slave {

    private static final Logger LOGGER = Logger.getLogger(RelayControllerSlave.class.getName());

    private String hostName;
    private int portNumber;

    @DataBoundConstructor
    public RelayControllerSlave(String name, String nodeDescription, String hostName, String portNumber) throws FormException, IOException {
        super(name, null, new ComputerLauncher() {
            public boolean isLaunchSupported() {
                return false;
            }
        });
        setNumExecutors(1);
        setNodeDescription(nodeDescription);
        setHostName(hostName);
        setPortNumber(portNumber);
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
        try {
            this.portNumber = Integer.parseInt(portNumber);
        } catch (NumberFormatException e) {
            LOGGER.warning("Port number must be number");
        }
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
        for (SubTask task : item.task.getSubTasks()) {
            if (task.getClass().isAssignableFrom(RelayControllerTask.class)) {
                if (task.getAssignedLabel().matches(this)) {
                    return null;
                }
            }
        }
        return new CauseOfBlockage() {
            public String getShortDescription() {
                return new String("Project " + item.getDisplayName() + " is not supported on " + getNodeName());
            }
        };
    }
    
    public List<AbstractProject> getTiedJobs() {
        List<AbstractProject> r = new ArrayList<AbstractProject>();
        //System.out.println("Searching for tired jobs for node " + getNodeName());
        for (AbstractProject<?,?> p : Jenkins.getInstance().getAllItems(AbstractProject.class)) {
            for (RelayControllerProperty property : Util.filter(p.getAllProperties(), RelayControllerProperty.class)) {
                if (property.getRelayControllerName().compareToIgnoreCase(getNodeName()) == 0) {
                    //System.out.println(p.getDisplayName() + " is tired to node " + getNodeName());
                    r.add(p);
                    break;
                }
            }
        }
        return r.isEmpty() ? Collections.EMPTY_LIST : r;
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
        
        public FormValidation doCheckHostName(@QueryParameter String value) {
            String hostName = Util.fixEmptyAndTrim(value);
            if (hostName==null || hostName.length() == 0) {
                return FormValidation.error("Hostname cannot be empty");
            }
            try {
                InetAddress.getByName(hostName);
            } catch (UnknownHostException e) {
                if (hostName.matches("^[0-9.]+$")) {
                    return FormValidation.error(hostName + " is not valid IPv4 address");
                } else {
                    return FormValidation.error(hostName + " cannot be resolved into IP address");
                }
            }
            return FormValidation.ok();
        }    

        public FormValidation doCheckPortNumber(@QueryParameter String value) {
            String portNumber = Util.fixEmptyAndTrim(value);
            if (portNumber==null || portNumber.length() == 0) {
                return FormValidation.error("Port number cannot be empty");
            }
            if (!portNumber.matches("^[1-9][0-9]{0,4}$")) {
                return FormValidation.error("Invalid port number " + portNumber);
            }
            try {
                int number = Integer.parseInt(portNumber);
                if (number > 65535) {
                    return FormValidation.error("Port number cannot be larger than 65535");
                }
            } catch (NumberFormatException e) {
                FormValidation.error("Invalid port number " + portNumber);
            }
            return FormValidation.ok();
        }    
    }
}
