package au.com.grieve.multibridge.api.event;

import au.com.grieve.multibridge.instance.Instance;
import au.com.grieve.multibridge.util.Task;
import net.md_5.bungee.api.plugin.Event;

import java.util.ArrayList;
import java.util.List;

public class BuildEvent extends Event {

    private final Instance instance;
    private List<Task> tasks;

    public BuildEvent(Instance instance) {
        this.instance = instance;
        tasks = new ArrayList<>();
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public Instance getInstance() {
        return instance;
    }


}
