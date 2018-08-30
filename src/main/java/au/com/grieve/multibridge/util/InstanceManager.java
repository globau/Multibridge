package au.com.grieve.multibridge.util;

import au.com.grieve.multibridge.MultiBridge;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class InstanceManager {
    private final MultiBridge plugin;

    private List<Instance> registeredInstances = new ArrayList<>();
    private List<Integer> registeredPorts = new ArrayList<>();

    public InstanceManager(MultiBridge plugin) {
        this.plugin = plugin;
    }

    /**
     * Stop all Instances
     */
    public void stopAll() {
        for (Instance instance: registeredInstances) {
            instance._stop();
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
     * Return an instance by name
     * @TODO: Need to use weakreference so we return the same object if its still in scope
     */
    public Instance getInstance(String name) {
        // Look for existing instance
        for(Instance instance: registeredInstances) {
            if (instance.name.equals(name)) {
                return instance;
            }
        }

        // Create new Object
        try {
            return new Instance(name);
        } catch (InstantiationException e) {
            return null;
        }
    }

    /**
     * Return a list of Instances
     *
     * Loop through all folders under the instancesFolder folder and look for ones that contain an instance.yml. Read this in
     * to add to the list of instances
     *
     * @return List of Instances
     */
    public Map<String, Instance> getInstances() {
        Map<String, Instance> instances = new HashMap<>();
        try (Stream<Path> paths = Files.list(getInstanceFolder())) {
            paths
                    .filter(Files::isDirectory)
                    .filter(p -> Files.exists(p.resolve("instance.yml")))
                    .forEach(p -> instances.put(p.getFileName().toString(), getInstance(p.getFileName().toString())));
        } catch (IOException ignored) {
        }

        return instances;
    }

    public void remove(Instance instance) throws IOException {
        assert(instance != null);
        assert(!instance.isRunning());

        // Remove the Directory
        try (Stream<Path> stream = Files.walk(instance.instanceFolder)) {
            stream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
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
//                    System.out.println("Source:" + sourcePath.toString() + ", Dest:" + target.resolve(template.location.relativize(sourcePath).toString()));

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
        instanceConfig.set("templateName", templateName);
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(instanceConfig, target.resolve("instance.yml").toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return getInstance(instanceName);
    }

    /**
     * Register instance with manager.
     * Gets assign a port and listed on Bungee
     */
    private void registerInstance(Instance instance) throws IndexOutOfBoundsException {
        Configuration config = plugin.getConfig();
        Integer portMin = config.getInt("ports.min", 26000);
        Integer portMax = config.getInt("ports.max", 26100);
        for(Integer port = portMin; port < portMax; port++) {
            if (!registeredPorts.contains(port)) {
                registeredPorts.add(port);
                registeredInstances.add(instance);

                // Register with Bungee
                ServerInfo info = plugin.getProxy().constructServerInfo(
                        instance.getName(),
                        new InetSocketAddress("127.0.0.1", port),
                        instance.getName(),
                        true);
                plugin.getProxy().getServers().put(instance.getName(), info);
                instance.port = port;
                return;
            }
        }

        throw new IndexOutOfBoundsException("No free ports");
    }

    /**
     * Unregister instance with manager.
     * Remove port assignement and unregister from Bungee
     */
    private void unregisterInstance(Instance instance) {
        // Unregister with Bungee
        plugin.getProxy().getServers().remove(instance.getName());

        // Unregister from ourselves
        registeredInstances.remove(instance);
        registeredPorts.remove(instance.getPort());
        instance.port = null;
    }

    /**
     * Definition of an Instance
     */
    public class Instance {
        private Configuration instanceConfig;
        private Configuration templateConfig;
        private Path instanceFolder;
        private String name;
        private Integer port;

        // Placeholders
        private Map<String,String> placeHolders;

        // Async IO
        private Process process;
        private BufferedReader reader;
        private BufferedWriter writer;

        private Instance(String name) throws InstantiationException {
            instanceFolder = getInstanceFolder().resolve(name);
            this.name = name;

            // Make sure Folder Exists
            if (!Files.exists(instanceFolder)) {
                throw new InstantiationException("Instance folder does not exist");
            }

            Path instanceConfigPath = instanceFolder.resolve("instance.yml");
            Path templateConfigPath = instanceFolder.resolve("template.yml");

            // Make sure config file exists
            if (!Files.exists(instanceConfigPath) || !Files.exists(templateConfigPath)) {
                throw new InstantiationException("Instance configuration does not exist");
            }

            try {
                instanceConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(instanceConfigPath.toFile());
                templateConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(templateConfigPath.toFile());
            } catch (IOException e) {
                throw new InstantiationError("Cannot load Instance configuration");
            }
        }

        /**
         * Save instanceConfig
         */
        private void saveConfig() {
            Path instanceConfigPath = instanceFolder.resolve("instance.yml");
            try {
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(instanceConfig, instanceConfigPath.toFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Return Placeholders
         */
        private Map<String, String> getPlaceHolders() {
            if (placeHolders == null) {
                placeHolders = new HashMap<>();

                // Add Defaults
                if (templateConfig.contains("placeHolders.defaults")) {
                    for (String k : templateConfig.getSection("placeHolders.defaults").getKeys()) {
                        placeHolders.put(k.toUpperCase(), templateConfig.getSection("placeHolders.defaults").getString(k));
                    }
                }

                // Add Instance Settings
                if (instanceConfig.contains("placeHolders")) {
                    for (String k : instanceConfig.getSection("placeHolders").getKeys()) {
                        placeHolders.put(k.toUpperCase(), instanceConfig.getSection("placeHolders").getString(k));
                    }
                }
            }

            // Refresh Builtins
            placeHolders.put("MB_SERVER_IP", "127.0.0.1");
            placeHolders.put("MB_SERVER_PORT", port == null?"unknown":port.toString());
            placeHolders.put("MB_SERVER_NAME", name);

            return placeHolders;
        }

        /**
         * Start Instance
         */
        public void start() throws RuntimeException {
            // Get a new port
            try {
                registerInstance(this);
            } catch (IndexOutOfBoundsException e) {
                throw new RuntimeException("Unable to register instance: " + e.getMessage());
            }

            // Build Template Files
            SimpleTemplate st = new SimpleTemplate(getPlaceHolders());
            updateTemplates(st);

            // Update instanceConfig
            instanceConfig.set("hadFirstRun", true);
            saveConfig();


            // Execute
            System.out.println("[" + name + "] " + "Starting Instance by executing: " + st.replace(templateConfig.getString("start.execute")));
            ProcessBuilder builder = new ProcessBuilder(st.replace(templateConfig.getString("start.execute")).split(" "));
            builder.redirectErrorStream(true);
            builder.directory(instanceFolder.toFile());
            try {
                process = builder.start();
            } catch (IOException e) {
                process = null;
                e.printStackTrace();
                return;
            }

            OutputStream stdin = process.getOutputStream();
            InputStream stdout = process.getInputStream();

            reader = new BufferedReader(new InputStreamReader(stdout));
            writer = new BufferedWriter(new OutputStreamWriter(stdin));

            plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
                @Override
                public void run() {
                    try {
                        for (String line = null; ((line = reader.readLine()) != null); ) {
                            System.out.println("[" + name + "] " + line);
                        }
                    } catch (IOException ignored) {
                    } finally {
                        try {
                            reader.close();
                            reader = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    // Stop ourself
                    _stop();
                }
            });

            // If we have startup commands lets schedule that
            if (templateConfig.getStringList("start.commands").size() > 0) {
                System.out.println("[" + name + "] Waiting to send Start Commands");
                plugin.getProxy().getScheduler().schedule(plugin, new Runnable() {
                    @Override
                    public void run() {
                        if (isRunning()) {
                            for (String cmd : templateConfig.getStringList("start.commands")) {
                                try {
                                    System.out.println("[" + name + "] Sending Command: " + cmd);
                                    writer.write(cmd + "\n");
                                    writer.flush();
                                } catch (IOException e) {
                                    break;
                                }
                            }
                        }
                    }
                }, templateConfig.getInt("start.delay", 30), TimeUnit.SECONDS);
            }
        }

        /**
         * Update Template files with placeholder values
         */
        private void updateTemplates(SimpleTemplate st) {
            // Statics for first run
            if (!instanceConfig.getBoolean("hadFirstRun")) {
                for(String fileName:  templateConfig.getStringList("templates.static")) {
                    try {
                        st.replace(instanceFolder.resolve(fileName + ".template"), instanceFolder.resolve(fileName));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            // Update Dynamics
            for(String fileName:  templateConfig.getStringList("templates.dynamic")) {
                try {
                    st.replace(instanceFolder.resolve(fileName + ".template"), instanceFolder.resolve(fileName));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Stop Internal
         */
        private void _stop() {
            // If reader stream is not closed, lets try to shutdown properly
            if (reader != null && writer != null) {
                if (templateConfig.contains("stop.commands")) {
                    for (String command: templateConfig.getStringList("stop.commands")) {
                        try {
                            System.out.println("[" + name + "] " + "Sending command: " + command);
                            writer.write(command + "\n");
                            writer.flush();
                        } catch (IOException e) {
                            break;
                        }
                    }
                }

                if (reader != null) {
                    System.out.println("[" + name + "] " + "Waiting for Instance to shutdown");
                    int maxTime = templateConfig.getInt("stop.delay", 5);
                    while (reader != null) {
                        try {
                            Thread.sleep(1000);
                            maxTime -= 1;
                            if (maxTime < 1) {
                                break;
                            }
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            }

            // Terminate task if needed
            if (reader != null) {
                System.err.print("[" + name + "] " + "Terminating Instance");
                process.destroy();
                process = null;
            }

            System.out.println("Instance Shut Down");
            unregisterInstance(this);
            process = null;
        }

        /**
         * Stop Instance
         */
        public void stop() {
            plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
                @Override
                public void run() {
                    _stop();
                }

            });

        }

        /**
         * Remove Instance
         */
        public void remove() {

        }

        /**
         * Is Instance running?
         */
        public boolean isRunning() {
            return process != null;
        }

        public Integer getPort() {
            return port;
        }

        public String getName() {
            return name;
        }

    }
}