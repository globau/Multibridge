package au.com.grieve.multibridge.api.event;

import au.com.grieve.multibridge.instance.Instance;
import net.md_5.bungee.api.plugin.Event;

public class ReadyEvent extends Event {

    private final Instance instance;
    private boolean ready = true;

    public ReadyEvent(Instance instance) {
        this.instance = instance;
    }

    public Instance getInstance() {
        return instance;
    }

    public void setReady(boolean b) {
        ready = ready & b;
    }

    public boolean getReady() {
        return ready;
    }
}
