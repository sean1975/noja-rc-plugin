package io.jenkins.plugins.noja;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

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
            String hostName = node.getHostName();
            if (!hostName.matches("^[1-9][0-9]{0,2}\\.[1-9][0-9]{0,2}\\.[1-9][0-9]{0,2}\\.[1-9][0-9]{0,2}$")) {
                try {
                    hostName = InetAddress.getByName(hostName).getHostAddress();
                } catch (UnknownHostException e) {
                    listener.getLogger().println("Hostname " + hostName + " cannot be resolved into IP address");
                }
            }
            String portNumber = String.valueOf(node.getPortNumber());
            env.put("RC_IPADDRESS", hostName);
            env.put("RC_PORTNUMBER", portNumber);
            listener.getLogger().println("RC_IPADDRESS=" + hostName);
            listener.getLogger().println("RC_PORTNUMBER=" + portNumber);
        }
    }
}
