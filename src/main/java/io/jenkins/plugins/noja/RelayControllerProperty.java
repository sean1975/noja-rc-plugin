package io.jenkins.plugins.noja;

import java.util.ArrayList;
import java.util.Collection;


import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.Items;
import hudson.model.Job;
import hudson.model.queue.SubTask;
import jenkins.model.OptionalJobProperty;

public class RelayControllerProperty extends OptionalJobProperty<Job<?, ?>> {

    private String relayControllerName;

    @DataBoundConstructor
    public RelayControllerProperty(String relayControllerName) {
        this.relayControllerName = relayControllerName;
    }

    public String getRelayControllerName() {
        return relayControllerName;
    }
    
    public void setRelayControllerName(String relayControllerName) {
        this.relayControllerName = relayControllerName;
    }
    
    public Collection<? extends SubTask> getSubTasks() {
        ArrayList<RelayControllerTask> tasks = new ArrayList<RelayControllerTask>();
        tasks.add(new RelayControllerTask(this));
        return tasks;
    }
    
    @Extension
    public static class DescriptorImpl extends OptionalJobPropertyDescriptor {

        public String getDisplayName() {
            return "NOJA Power Relay Controller is required to run this project";
        }

        static {
            Items.XSTREAM2.addCompatibilityAlias(
                    "org.jenkinsci.plugins.workflow.job.properties.RelayControllerProperty",
                    RelayControllerProperty.class);
        }
    }
}
