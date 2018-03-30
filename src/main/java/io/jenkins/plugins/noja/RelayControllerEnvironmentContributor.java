package io.jenkins.plugins.noja;

import java.io.IOException;
import java.util.Map;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentContributor;
import hudson.model.Job;
import hudson.model.TaskListener;

@Extension(ordinal=1000)
public class RelayControllerEnvironmentContributor extends EnvironmentContributor {

    @Override
    public void buildEnvironmentFor(Job j, EnvVars env, TaskListener listener) throws IOException, InterruptedException {
        RelayControllerProperty property = (RelayControllerProperty) j.getProperty(RelayControllerProperty.class.getName());
        if (property != null) {
            RelayControllerSlave node = property.getRelayControllerNode();
            Map<String, String> envVars = node.getEnvVars();
            for (Map.Entry<String, String> entry : envVars.entrySet()) {
                env.put(entry.getKey(), entry.getValue());
            }
        }
    }
}
