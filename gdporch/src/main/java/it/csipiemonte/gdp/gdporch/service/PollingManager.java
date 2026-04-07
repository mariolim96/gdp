package it.csipiemonte.gdp.gdporch.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * Manages the runtime state of the polling process.
 * The state is in-memory only and will reset to enabled on application restart.
 */
@ApplicationScoped
public class PollingManager {

    private static final Logger LOG = Logger.getLogger(PollingManager.class);
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        LOG.infof("Polling status changed: %s", enabled ? "ENABLED" : "DISABLED");
        this.enabled = enabled;
    }

    public String getStatus() {
        return enabled ? "ENABLED" : "DISABLED";
    }
}
