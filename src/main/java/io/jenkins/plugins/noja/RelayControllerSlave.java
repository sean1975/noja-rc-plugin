package io.jenkins.plugins.noja;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

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
import jenkins.model.Jenkins;

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
        for (SubTask task : item.task.getSubTasks()) {
            if (task.getClass().isAssignableFrom(RelayControllerTask.class)) {
                return null;
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
    }
}
