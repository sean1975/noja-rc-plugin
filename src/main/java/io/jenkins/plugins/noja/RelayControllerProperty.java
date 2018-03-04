package io.jenkins.plugins.noja;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.Items;
import hudson.model.Job;
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
