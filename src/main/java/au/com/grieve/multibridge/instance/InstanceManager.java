package au.com.grieve.multibridge.instance;

import au.com.grieve.multibridge.MultiBridge;
import au.com.grieve.multibridge.template.TemplateManager;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class InstanceManager implements Listener {
    private final MultiBridge plugin;

    private Map<String, Instance> instances = new HashMap<>();
    private List<Integer> ports = new ArrayList<>();

    public InstanceManager(MultiBridge plugin) {
        this.plugin = plugin;

        loadInstances();
    }

    /**
     * Stop all Instances
     */
    public void stopAll() {
        for (Instance instance: instances.values()) {
            instance.stopNow();
        }
    }

    /**
     * Return our Instance Folder
     */
    public Path getInstanceFolder() {
        Path path = Paths.get(plugin.getConfig().getString("instancesFolder"));

        // Prepend our DataFolder if this does not begin with a /
        return path.isAbsolute() ? path : Paths.get(plugin.getDataFolder().toString(), path.toString());
    }

    /**
     * Load all instances
     */
    private void loadInstances() {
        Map<String, Instance> old = instances;
        instances = new HashMap<>();

        try (Stream<Path> paths = Files.list(getInstanceFolder())) {
            paths
                    .filter(Files::isDirectory)
                    .filter(p -> Files.exists(p.resolve("instance.yml")))
                    .forEach(p -> {
                        String name = p.getFileName().toString();
                        if (!old.containsKey(name)) {
                            // Can't create same name as a bungee server, so skip if so
                            if (!plugin.getProxy().getServers().containsKey(name)) {
                                try {
                                    Instance instance = new Instance(this, p);

                                    // Register as a Listener
                                    plugin.getProxy().getPluginManager().registerListener(plugin,this);

                                    instances.put(instance.getName(), instance);
                                } catch (InstantiationException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            instances.put(name, old.get(name));
                            old.remove(name);
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Stop and Unregister Old Instances
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            for (Instance instance: old.values()) {
                if (instance.isRunning()) {
                    plugin.getProxy().getPluginManager().unregisterListener(instance);
                    instance.stopNow();
                }
            }
        });
    }

    /**
     * Return an instance by name
     */
    public Instance getInstance(String name) {
        if (instances.containsKey(name)) {
            return instances.get(name);
        }

        return null;
    }

    /**
     * Return a list of Instances
     */
    public Map<String, Instance> getInstances() {
        return instances;
    }

    /**
     * Remove Instance
     */
    public void remove(Instance instance) throws IOException {
        assert(instance != null);
        assert(!instance.isRunning());

        // Remove the Directory
        try (Stream<Path> stream = Files.walk(getInstanceFolder().resolve(instance.getName()))) {
            stream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }

        instances.remove(instance.getName());
        plugin.getProxy().getPluginManager().unregisterListener(instance);
    }

    /**
     * Return a free port
     * @return port
     */
    public int getPort() throws IndexOutOfBoundsException {
        Configuration config = plugin.getConfig();
        Integer portMin = config.getInt("ports.min", 26000);
        Integer portMax = config.getInt("ports.max", 26100);
        for(Integer port = portMin; port < portMax; port++) {
            if (!ports.contains(port)) {
                ports.add(port);
                return port;
            }
        }

        throw new IndexOutOfBoundsException("No free ports");
    }

    /**
     * Release used port
     */
    public void releasePort(int port) {
        ports.remove(Integer.valueOf(port));
    }

    public Instance create(String templateName, String instanceName) {
        TemplateManager.Template template = plugin.getTemplateManager().getTemplate(templateName);

        // Does Template Exist?
        if (template == null) {
            return null;
        }

        // Does Instance Already exist?
        if (getInstance(instanceName) != null) {
            return null;
        }

        // Can't create same name as a bungee server
        if (plugin.getProxy().getServers().containsKey(instanceName)) {
            return null;
        }

        Path target = getInstanceFolder().resolve(instanceName);

        // Make sure parent folder exists
        if (!Files.exists(target.getParent())) {
            try {
                Files.createDirectories(target.getParent());
            } catch (IOException e) {
                return null;
            }
        }

        // Copy Template to Instance
        try (Stream<Path> stream = Files.walk(template.location)) {
            stream.forEach(sourcePath -> {
                try {
                    Files.copy(
                            sourcePath,
                            target.resolve(template.location.relativize(sourcePath)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create new Instance Config
        Configuration instanceConfig = new Configuration();
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(instanceConfig, target.resolve("instance.yml").toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Instance instance = new Instance(this, target);
            instance.setTag("MB_TEMPLATE_NAME", templateName);
            instances.put(instanceName, instance);
            return instance;
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

    public MultiBridge getPlugin() {
        return plugin;
    }

}