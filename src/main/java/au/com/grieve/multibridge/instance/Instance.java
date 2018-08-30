package au.com.grieve.multibridge.instance;

import au.com.grieve.multibridge.util.SimpleTemplate;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Definition of an Instance
 */
public class Instance implements Listener {
    // Constants
    public enum StartMode {
        MANUAL, SERVER_START, SERVER_JOIN, INSTANCE_JOIN
    }

    public enum StopMode {
        MANUAL, SERVER_EMPTY, INSTANCE_EMPTY
    }

    public enum State {
        STOPPED, WAITING, STARTED
    }

    // Variables
    private InstanceManager manager;
    private Configuration instanceConfig;
    private Configuration templateConfig;
    private Path instanceFolder;
    private String name;
    private Integer port;
    private boolean bungeeRegistered = false;

    // Tags
    private Map<String,String> tags;

    // Async IO
    private Process process;
    private BufferedReader reader;
    private BufferedWriter writer;

    public Instance(InstanceManager manager, Path instanceFolder) throws InstantiationException {
        this.manager = manager;
        this.instanceFolder = instanceFolder;
        this.name = instanceFolder.getFileName().toString();

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

        // Register ourself as a Listener
        manager.getPlugin().getProxy().getPluginManager().registerListener(manager.getPlugin(), this);

        // If our start mode is INSTANCE_JOIN we need to register with Bungee now
        if (getStartMode() == StartMode.INSTANCE_JOIN) {
            registerBungee();
        }

        // Auto-start if needed
        if (getStartMode() == StartMode.SERVER_START) {
            manager.getPlugin().getProxy().getScheduler().schedule(manager.getPlugin(), () -> {
                System.out.println("[" + name + "] " + "Auto-Starting: Server Start");
                start();
            }, getStartDelay(), TimeUnit.SECONDS);

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

        // Clear tags cache
        tags = null;
    }

    /**
     * Return Placeholders
     */
    private Map<String, String> getTags() {
        return getTags(false);
    }

    private Map<String, String> getTags(boolean refresh) {
        if (tags == null || refresh) {
            tags = new HashMap<>();

            // Add Defaults
            if (templateConfig.contains("tags.defaults")) {
                for (String k : templateConfig.getSection("tags.defaults").getKeys()) {
                    tags.put(k.toUpperCase(), templateConfig.getSection("tags.defaults").getString(k));
                }
            }

            // Add Instance Settings
            if (instanceConfig.contains("tags")) {
                for (String k : instanceConfig.getSection("tags").getKeys()) {
                    tags.put(k.toUpperCase(), instanceConfig.getSection("tags").getString(k));
                }
            }
        }

        // Refresh Builtins
        tags.put("MB_SERVER_IP", "127.0.0.1");
        tags.put("MB_SERVER_PORT", port == null?"unknown":port.toString());
        tags.put("MB_SERVER_NAME", name);

        return tags;
    }

    /**
     * Register with Bungeecord
     */
    private void registerBungee() {
        if (bungeeRegistered) {
            return;
        }

        port = manager.getPort();
        ServerInfo info = manager.getPlugin().getProxy().constructServerInfo(
                name,
                new InetSocketAddress("127.0.0.1", port),
                name,
                true);
        manager.getPlugin().getProxy().getServers().put(name, info);
        bungeeRegistered = true;
    }

    /**
     * Unregister with Bungeecord
     */
    private void unregisterBungee() {
        if (!bungeeRegistered) {
            return;
        }

        manager.getPlugin().getProxy().getServers().remove(name);
        manager.releasePort(port);
        port = null;
        bungeeRegistered = false;
    }


    /**
     * Start Instance
     */
    public void start() throws RuntimeException {
        if (isRunning()) {
            return;
        }

        // Register with Bungee
        registerBungee();

        // Build Template Files
        SimpleTemplate st = new SimpleTemplate(getTags());
        updateTemplates(st);

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

        manager.getPlugin().getProxy().getScheduler().runAsync(manager.getPlugin(), () -> {
            try {
                for (String line; ((line = reader.readLine()) != null); ) {
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

            System.out.println("[" + name + "] " + "Instance Shut Down");

            process = null;
            reader = null;
            writer = null;

            // Unregister with bungee if needed
            if (getStartMode() != StartMode.INSTANCE_JOIN) {
                unregisterBungee();
            }
        });

        // If we have startup commands lets schedule that
        if (templateConfig.getStringList("start.commands").size() > 0) {
            System.out.println("[" + name + "] Waiting to send Start Commands");
            manager.getPlugin().getProxy().getScheduler().schedule(manager.getPlugin(), () -> {
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
            }, templateConfig.getInt("start.delay", 30), TimeUnit.SECONDS);
        }
    }

    /**
     * Update Template files with placeholder values
     */
    private void updateTemplates(SimpleTemplate st) {
        // Statics for first run
        if (!getTagBoolean("MB_FIRST_RUN")) {
            for(String fileName:  templateConfig.getStringList("templates.static")) {
                try {
                    st.replace(instanceFolder.resolve(fileName + ".template"), instanceFolder.resolve(fileName));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Update instanceConfig
            setTag("MB_FIRST_RUN", true);
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
    public void stopNow() {
        // If we are not runnign we are done
        if (!isRunning()) {
            return;
        }

        if (reader != null && writer != null) {
            if (templateConfig.contains("stop.commands")) {
                for (String command : templateConfig.getStringList("stop.commands")) {
                    try {
                        System.out.println("[" + name + "] " + "Sending command: " + command);
                        writer.write(command + "\n");
                        writer.flush();
                    } catch (IOException e) {
                        break;
                    }
                }
            }
        }

        System.out.println("[" + name + "] " + "Waiting for Instance to shutdown");
        int maxTime = templateConfig.getInt("stop.delay", 5);
        while (isRunning()) {
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

        // Terminate task if needed
        if (isRunning()) {
            System.err.print("[" + name + "] " + "Murdering Instance");
            process.destroy();
        } else {
            System.out.println("[" + name + "] " + "Instance Cleanly Shut Down");
        }

    }

    /**
     * Stop Instance
     */
    public void stop() {
        if (!isRunning()) {
            return;
        }
        manager.getPlugin().getProxy().getScheduler().runAsync(manager.getPlugin(), new Runnable() {
            @Override
            public void run() {
                stopNow();
            }

        });

    }

    /**
     * Remove Instance
     */
    public void remove() throws IOException {
        manager.remove(this);
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

    /**
     * Get Tag
     */
    public String getTag(String key) {
        return getTag(key, null);
    }

    public String getTag(String key, String def) {
        return getTags().getOrDefault(key, def);
    }

    public int getTagInt(String key, int def) {
        try {
            return Integer.parseInt(getTag(key, String.valueOf(def)));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public int getTagInt(String key) {
        return getTagInt(key, 0);
    }

    public boolean getTagBoolean(String key) {
        return getTagBoolean(key, false);
    }

    public boolean getTagBoolean(String key, boolean def) {
        return Boolean.parseBoolean(getTag(key, String.valueOf(def)));
    }

    public void setTag(String key, String value) {
        instanceConfig.set("tags." + key, value);
        saveConfig();
    }

    public void setTag(String key, int value) {
        instanceConfig.set("tags." + key, value);
        saveConfig();
    }

    public void setTag(String key, boolean value) {
        instanceConfig.set("tags." + key, value);
        saveConfig();
    }

    /**
     * Return Startup Type
     */
    public StartMode getStartMode() {
        return StartMode.valueOf(getTag("MB_START_MODE", "MANUAL"));
    }

    public void setStartMode(StartMode mode) {
        setTag("MB_START_MODE", mode.toString());
    }

    /**
     * Return Startup Delay
     */
    public int getStartDelay() {
        return getTagInt("MB_START_DELAY", 0);
    }

    public void setStartDelay(int delay) {
        setTag("MB_START_DELAY", delay);
    }

    /**
     * Get Stop Type
     */
    public StopMode getStopMode() {
        return StopMode.valueOf(getTag("MB_STOP_MODE", "MANUAL").toUpperCase());
    }

    public void setStopMode(StopMode mode) {
        setTag("MB_STOP_MODE", mode.toString());
    }


    /**
     * Get Stop Delay
     */
    public int getStopDelay() {
        return getTagInt("MB_STOP_DELAY", 0);
    }

    public void setStopDelay(int delay) {
        setTag("MB_STOP_DELAY", delay);
    }

    /**
     * Get State
     */
    public State getState() {
        if (isRunning()) {
            return State.STARTED;
        }

        if (getStartMode() == StartMode.INSTANCE_JOIN && bungeeRegistered) {
            return State.WAITING;
        }

        return State.STOPPED;
    }


    /**
     * Check if we need to start before a player logs into the server
     */
    @EventHandler
    public void onPreLogin(PreLoginEvent event) {
        if (!isRunning() && getStartMode() == StartMode.SERVER_JOIN) {
            manager.getPlugin().getProxy().getScheduler().schedule(manager.getPlugin(), () -> {
                System.out.println("[" + name + "] " + "Auto-Starting: Server Join");
                start();
            }, getStartDelay(), TimeUnit.SECONDS);
        }
    }

    /**
     * Check if we need to start before a player connects to our instance
     */
    @EventHandler
    public void onServerConnectEvent(ServerConnectEvent event) {
        if (event.getTarget().getName().equals(name)) {
            if (!isRunning() && getStartMode() == StartMode.INSTANCE_JOIN) {
                System.out.println("[" + name + "] " + "Auto-Starting: Instance Join");
                start();

                // Delay the connection
                try {
                    Thread.sleep(getStartDelay() * 1000);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    /**
     * Check if the server is empty to shut down
     */
    @EventHandler
    public void onPlayerDisconnectEvent(PlayerDisconnectEvent event) {
        if (manager.getPlugin().getProxy().getPlayers().size() < 2) {
            if (isRunning() && getStopMode() == StopMode.SERVER_EMPTY) {
                manager.getPlugin().getProxy().getScheduler().schedule(manager.getPlugin(), () -> {
                    if (manager.getPlugin().getProxy().getPlayers().size() < 1) {
                        System.out.println("[" + name + "] " + "Auto-Stopping: Server Empty");
                        stop();
                    }

                }, getStopDelay(), TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Check if instance is empty to shut down
     */
    @EventHandler
    public void onServerDisconnectEvent(ServerDisconnectEvent event) {
        if (event.getTarget().getName().equals(name)) {
            if (isRunning() && getStopMode() == StopMode.INSTANCE_EMPTY && event.getTarget().getPlayers().size() < 2) {
                manager.getPlugin().getProxy().getScheduler().schedule(manager.getPlugin(), () -> {
                    if (event.getTarget().getPlayers().size() < 1) {
                        System.out.println("[" + name + "] " + "Auto-Stopping: Instance Empty");
                        stop();
                    }
                }, getStopDelay(), TimeUnit.SECONDS);
            }
        }
    }





}