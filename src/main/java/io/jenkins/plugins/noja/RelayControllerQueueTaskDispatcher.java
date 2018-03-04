package io.jenkins.plugins.noja;

import java.util.logging.Logger;

import javax.annotation.CheckForNull;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.queue.CauseOfBlockage;
import hudson.model.queue.QueueTaskDispatcher;
import jenkins.model.Jenkins;

@Extension
public class RelayControllerQueueTaskDispatcher extends QueueTaskDispatcher {

    private static final Logger LOGGER = Logger.getLogger(RelayControllerQueueTaskDispatcher.class.getName());

    @Override
    public @CheckForNull CauseOfBlockage canRun(Queue.Item item) {
        if (item.task instanceof Job<?, ?>) {
            Job<?, ?> job = (Job<?, ?>) item.task;
            RelayControllerProperty property = job.getProperty(RelayControllerProperty.class);
            if (property != null) {
                String rc = property.getRelayControllerName();
                LOGGER.info("Check if relay controller " + rc + " is available");
                Computer c = Jenkins.getInstance().getComputer(rc);
                RelayControllerComputer computer = null;
                if (c instanceof RelayControllerComputer) {
                    computer = (RelayControllerComputer) c;
                    if (rc.trim().compareToIgnoreCase(computer.getName()) == 0) {
                        if (computer.countIdle() > 0) {
                            return null;
                        }
                    }
                }
                return new CauseOfBlockage() {
                    @Override
                    public String getShortDescription() {
                        return "Relay controller is not available";
                    }                    
                };
            }            
        }
        return null;
    }
}
