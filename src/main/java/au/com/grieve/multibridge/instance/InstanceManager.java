package au.com.grieve.multibridge.instance;

import au.com.grieve.multibridge.MultiBridge;
import au.com.grieve.multibridge.template.Template;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class InstanceManager {
    private final MultiBridge plugin;

    private Map<String, Instance> instances = new HashMap<>();
    private List<Integer> ports = new ArrayList<>();
    private List<InstanceBuilder> instanceBuilders = new ArrayList<>();

    public InstanceManager(MultiBridge plugin) {
        this.plugin = plugin;

        loadInstances();
    }

    /**
     * Stop all Instances
     */
    public void stopAll() {
        for (Instance instance: instances.values()) {
            try {
                instance.stop();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Return our Instance Folder
     */
    @SuppressWarnings("WeakerAccess")
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

        try {
            Files.createDirectories(getInstanceFolder());
        } catch (IOException e) {
            return;
        }

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
                    try {
                        instance.stop();
                    } catch (IOException ignored) {
                    }
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

        instance.cleanUp();
        instances.remove(instance.getName());

        // Remove the Directory
        try (Stream<Path> stream = Files.walk(getInstanceFolder().resolve(instance.getName()))) {
            //noinspection ResultOfMethodCallIgnored
            stream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    /**
     * Return a free port
     * @return port
     */
    int getPort() throws IndexOutOfBoundsException {
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
    void releasePort(int port) {
        ports.remove(Integer.valueOf(port));
    }

    private void deletePath(Path path) throws IOException {
        //noinspection ResultOfMethodCallIgnored
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    public Instance create(String templateName, String instanceName) throws IOException {
        Template template = plugin.getTemplateManager().getTemplate(templateName);

        // We always want to delete the folder if we have an exception
        try {

            // Does Template Exist?
            if (template == null) {
                throw new IOException("Template does not exist");
            }

            // Does Instance Already exist?
            if (getInstance(instanceName) != null) {
                throw new IOException("Instance already exists");
            }

            // Can't create same name as a bungee server
            if (plugin.getProxy().getServers().containsKey(instanceName)) {
                throw new IOException("An existing Bungee server already exists with that name");
            }

            Path target = getInstanceFolder().resolve(instanceName);

            // Make sure parent folder exists
            if (!Files.exists(target.getParent())) {
                Files.createDirectories(target.getParent());
            }

            // Copy Template to Instance
            for (Path p : (Iterable<Path>) Files.walk(template.getTemplateFolder())::iterator) {
                Files.copy(
                        p,
                        target.resolve(template.getTemplateFolder().relativize(p)));
            }

            // Create new Instance Config
            Configuration instanceConfig = new Configuration();
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(instanceConfig, target.resolve("instance.yml").toFile());

            Instance instance = new Instance(this, target);
            instance.setTag("MB_TEMPLATE_NAME", templateName);

            // Execute Builders
            for(InstanceBuilder instanceBuilder : getInstanceBuilders()) {
                instanceBuilder.build(instance);
            }

            instances.put(instanceName, instance);
            return instance;

        } catch (Throwable e) {
            e.printStackTrace();
            deletePath(getInstanceFolder());
            throw new IOException(e.getMessage());
        }


    }

    MultiBridge getPlugin() {
        return plugin;
    }

    public void registerBuilder(InstanceBuilder builder) {
        this.instanceBuilders.add(builder);
    }

    @SuppressWarnings("unused")
    public void unregisterBuilder(InstanceBuilder builder) {
        this.instanceBuilders.remove(builder);
    }

    private List<InstanceBuilder> getInstanceBuilders() {
        return this.instanceBuilders;
    }

}