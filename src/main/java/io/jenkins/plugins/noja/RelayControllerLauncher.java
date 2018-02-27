package io.jenkins.plugins.noja;

import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.model.TaskListener;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;

public class RelayControllerLauncher extends ComputerLauncher {

    private static final Logger LOGGER = Logger.getLogger(RelayControllerLauncher.class.getName());

    @DataBoundConstructor
    public RelayControllerLauncher() {
        super();
    }

    @Override
    public void launch(SlaveComputer computer, TaskListener listener) throws IOException, InterruptedException {
        RelayControllerComputer relayControllerComputer;
        if (computer instanceof RelayControllerComputer) {
            relayControllerComputer = (RelayControllerComputer) computer;
        } else {
            listener.error("Relay Controller Launcher accepts only RelayControllerComputer.class");
            throw new IllegalArgumentException("Relay Controller Launcher accepts only RelayControllerComputer.class");
        }
        final PrintStream logger = listener.getLogger();
        logger.println("Launched " + relayControllerComputer.getName());
        LOGGER.info("Launched " + relayControllerComputer.getName());
    }
}
