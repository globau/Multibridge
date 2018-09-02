package au.com.grieve.multibridge.api.event;

import au.com.grieve.multibridge.instance.Instance;
import net.md_5.bungee.api.plugin.Event;

public class BuildEvent extends Event {

    private final Instance instance;

    public BuildEvent(Instance instance) {
        this.instance = instance;
    }

    public Instance getInstance() {
        return instance;
    }

}
