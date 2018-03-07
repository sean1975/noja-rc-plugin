package io.jenkins.plugins.noja;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.acegisecurity.Authentication;

import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.ResourceList;
import hudson.model.Queue.Item;
import hudson.model.Queue.Task;
import hudson.model.queue.CauseOfBlockage;
import hudson.model.queue.SubTask;
import jenkins.model.Jenkins;
import jenkins.model.queue.AsynchronousExecution;

public class RelayControllerTask implements Task {
    
    private final RelayControllerProperty property;

    public RelayControllerTask(RelayControllerProperty property) {
        this.property = property;
    }
    
    public String getRelayControllerName() {
        return property.getRelayControllerName();
    }
    
    @Override
    public ResourceList getResourceList() {
        return ResourceList.EMPTY;
    }

    @Override
    public String getDisplayName() {
        return "Occupied";
    }

    @Override
    public Label getAssignedLabel() {
        Node node = Jenkins.getInstance().getNode(getRelayControllerName());
        if (node != null) {
            return node.getSelfLabel();
        }
        return null;
    }

    @Override
    public Node getLastBuiltOn() {
        return null;
    }

    @Override
    public long getEstimatedDuration() {
        return -1;
    }

    @Override
    public Executable createExecutable() throws IOException {
        return new RelayControllerTask.Executable(this);
    }

    @Override
    public Task getOwnerTask() {
        return this;
    }

    @Override
    public Object getSameNodeConstraint() {
        return null;
    }
    
    @Override
    public boolean isBuildBlocked() {
        return getCauseOfBlockage() != null;
    }

    @Override
    public String getWhyBlocked() {
        return null;
    }

    @Override
    public CauseOfBlockage getCauseOfBlockage() {
        return null;
    }

    @Override
    public String getName() {
        return getDisplayName();
    }

    @Override
    public String getFullDisplayName() {
       return getDisplayName();
    }

    @Override
    public void checkAbortPermission() {
    }

    @Override
    public Authentication getDefaultAuthentication() {
        return null;
    }

    @Override
    public Authentication getDefaultAuthentication(Item arg0) {
        return null;
    }

    @Override
    public boolean hasAbortPermission() {
        return false;
    }

    @Override
    public String getUrl() {
        return "computer/" + getRelayControllerName() + "/";
    }

    @Override
    public boolean isConcurrentBuild() {
         return false;
    }

    @Override
    public Collection<? extends SubTask> getSubTasks() {
        return Collections.singleton(this);
    }
    
    public class Executable implements Queue.Executable {

        private final SubTask parent;
        
        public Executable(SubTask parent) {
            this.parent = parent;
        }
        
        @Override
        public SubTask getParent() {
            return parent;
        }

        @Override
        public void run() throws AsynchronousExecution {
            // Do nothing
        }

        @Override
        public long getEstimatedDuration() {
            return parent.getEstimatedDuration();
        }
        
    }
}
