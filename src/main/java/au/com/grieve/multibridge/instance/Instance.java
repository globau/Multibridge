package au.com.grieve.multibridge.instance;

import au.com.grieve.multibridge.api.event.BuildEvent;
import au.com.grieve.multibridge.api.event.ReadyEvent;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
        UNKNOWN,
        PENDING,
        STOPPED,
        WAITING,
        STARTED,
        ERROR
    }

    // Variables
    private InstanceManager manager;
    private Configuration instanceConfig;
    private Configuration templateConfig;
    private Path instanceFolder;
    private String name;
    private Integer port;
    private boolean bungeeRegistered = false;
    private State state = State.UNKNOWN;

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

        // Register ourself as a Listener
        manager.getPlugin().getProxy().getPluginManager().registerListener(manager.getPlugin(), this);

        try {
            loadConfig();
        } catch (IOException e) {
            throw new InstantiationException(e.getMessage());
        }

        // Update State
        update();

        // Trigger Build Event if in pending state
        if (getState() == State.PENDING) {
            build();
        }

        // Auto-start if needed
        if (getStartMode() == StartMode.SERVER_START && getState() == State.STOPPED) {
            manager.getPlugin().getProxy().getScheduler().schedule(manager.getPlugin(), () -> {
                System.out.println("[" + name + "] " + "Auto-Starting: Server Start");
                try {
                    start();
                } catch (IOException e) {
                    System.out.println("[" + name + "] " + "Failed to Start: " + e.getMessage());
                }
            }, getStartDelay(), TimeUnit.SECONDS);

        }
    }

    @Override
    public void finalize() {
        unregisterBungee();
    }

    /**
     * Build instance
     */
    public void build() {
        manager.getPlugin().getProxy().getPluginManager().callEvent(new BuildEvent(this));
    }

    private void loadConfig() throws IOException {
        Path instanceConfigPath = instanceFolder.resolve("instance.yml");
        Path templateConfigPath = instanceFolder.resolve("template.yml");

        try {
            templateConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(templateConfigPath.toFile());
        } catch (IOException e) {
            throw new IOException("Cannot load Instance template.yml");
        }

        try {
            instanceConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(instanceConfigPath.toFile());
        } catch (IOException e) {
            instanceConfig = new Configuration();
        }
    }

    /**
     * Update our State
     */
    public void update() {
        boolean ready = manager.getPlugin().getProxy().getPluginManager().callEvent(new ReadyEvent(this)).getReady();
        boolean running = process != null;
        StartMode startMode = getStartMode();

        if (running) {
            state = State.STARTED;
        } else {
            // Not running

            if (ready) {
                if (startMode == StartMode.INSTANCE_JOIN) {
                    if (!hasRequiredTags()) {
                        state = State.ERROR;
                    } else {
                        if (!bungeeRegistered) {
                            System.out.println("[" + name + "]: Registering with BungeeCord");
                            registerBungee();
                        }
                        state = State.WAITING;
                    }
                } else {
                    if (bungeeRegistered) {
                        System.out.print("[" + name + "]: Unregistering with BungeeCord");
                    }
                    state = State.STOPPED;
                }
            } else {
                if (bungeeRegistered) {
                    System.out.print("[" + name + "]: Unregistering with BungeeCord");
                }
                state = State.PENDING;
            }
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
    public Map<String, String> getLocalTags() {
        tags = new HashMap<>();
        // Add Instance Settings
        if (instanceConfig.contains("tags")) {
            for (String k : instanceConfig.getSection("tags").getKeys()) {
                tags.put(k.toUpperCase(), instanceConfig.getSection("tags").getString(k));
            }
        }
        return tags;
    }

    public String getLocalTag(String key) {
        return instanceConfig.getString("tags." + key.toUpperCase());
    }

    public Map<String, String> getTags() {
        return getTags(true);
    }

    public Map<String, String> getTags(boolean refresh) {
        if (tags == null || refresh) {
            tags = new HashMap<>();

            // Add Defaults from Template
            if (templateConfig.contains("tags.defaults")) {
                for (String k : templateConfig.getSection("tags.defaults").getKeys()) {
                    tags.put(k.toUpperCase(), templateConfig.getSection("tags.defaults").getString(k));
                }
            }

            // Add Globals. Overrides above
            for (Map.Entry<String, String> e : manager.getPlugin().getGlobalManager().getTags().entrySet()) {
                tags.put(e.getKey(), e.getValue());
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

    public List<String> getRequiredTags() {
        return templateConfig.getStringList("tags.required");
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
    void unregisterBungee() {
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
    public void start() throws IOException {
        // Update our State
        update();

        // Must be either stopped or waiting
        State state = getState();
        if (state != State.STOPPED && state != State.WAITING) {
            throw new IOException("Can't Start Instance");
        }

        // Register with Bungee
        registerBungee();

        // Build Template Files
        SimpleTemplate st = new SimpleTemplate(tags);
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
            throw e;
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

            // Update
            update();
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
            setTag("MB_FIRST_RUN", "true");
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
    public void stopNow() throws IOException {
        // Only valid if our state is STARTED
        update();

        if (getState() == State.STARTED) {
            throw new IOException("Not Started");
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
        while (process != null) {
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
        if (process != null) {
            System.err.print("[" + name + "] " + "Murdering Instance");
            process.destroy();
        } else {
            System.out.println("[" + name + "] " + "Instance Cleanly Shut Down");
        }

    }

    /**
     * Stop Instance
     */
    public void stop() throws IOException {
        //manager.getPlugin().getProxy().getScheduler().runAsync(manager.getPlugin(), () -> stopNow());
        stopNow();

    }

    /**
     * Remove Instance
     */
    public void remove() throws IOException {
        manager.remove(this);
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
        update();
    }

    public void clearTag(String key) {
        instanceConfig.set("tags." + key, null);
        saveConfig();
        update();
    }

    /**
     * Return Startup Type
     */
    public StartMode getStartMode() {
        try {
            return StartMode.valueOf(getTag("MB_START_MODE", "MANUAL"));
        } catch (IllegalArgumentException e) {
            return null;
        }
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
        setTag("MB_START_DELAY", String.valueOf(delay));
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
        setTag("MB_STOP_DELAY", String.valueOf(delay));
    }

    public boolean hasRequiredTags() {
        Map<String, String> tags = getTags();
        for(String requiredTag: getRequiredTags()) {
            if (!tags.containsKey(requiredTag)) {
                return false;
            }
        }
        return true;
    }

    public Path getInstanceFolder() {
        return instanceFolder;
    }

    /**
     * Get State
     */
    public State getState() {
       return state;
    }

    public Configuration getInstanceConfig() {
        return instanceConfig;
    }

    public Configuration getTemplateConfig() {
        return templateConfig;
    }

    public boolean isRunning() {
        return process != null;
    }

    /**
     * Reload Config
     */
    public void reloadConfig() throws IOException {
        loadConfig();
        update();
    }

    /**
     * Check if we need to start before a player logs into the server
     */
    @EventHandler
    public void onPreLogin(PreLoginEvent event) {
        if (getState() != State.STARTED && getStartMode() == StartMode.SERVER_JOIN) {
            manager.getPlugin().getProxy().getScheduler().schedule(manager.getPlugin(), () -> {
                System.out.println("[" + name + "] " + "Auto-Starting: Server Join");
                try {
                    start();
                } catch (IOException e) {
                    System.out.println("[" + name + "] " + "Failed to Start: " + e.getMessage());
                }
            }, getStartDelay(), TimeUnit.SECONDS);
        }
    }

    /**
     * Check if we need to start before a player connects to our instance
     */
    @EventHandler
    public void onServerConnectEvent(ServerConnectEvent event) {
        if (event.getTarget().getName().equalsIgnoreCase(name)) {
            if (getState() != State.STARTED && getStartMode() == StartMode.INSTANCE_JOIN) {
                // Cancel the event and wait for it to really come up
                event.setCancelled(true);

                manager.getPlugin().getProxy().getScheduler().runAsync(manager.getPlugin(), () -> {
                    System.out.println("[" + name + "] " + "Auto-Starting: Instance Join");
                    try {
                        start();
                    } catch (IOException e) {
                        System.out.println("[" + name + "] " + "Failed to Start: " + e.getMessage());
                        return;
                    }

                    // Get current time
                    Date date = new Date();
                    long startTime = date.getTime();

                    manager.getPlugin().getProxy().getScheduler().schedule(manager.getPlugin(), new Runnable() {
                        @Override
                        public void run() {
                            Runnable that = this;
                            event.getTarget().ping((serverPing, ex) -> {
                                if (serverPing == null) {
                                    // Failed. Schedule to try again in a second if we have not run out of time
                                    if (date.getTime() - startTime > (getStartDelay()*1000)) {
                                        System.err.println("[" + name + "] " + "Failed to connect to Instance: Timed out");
                                        return;
                                    }

                                    if (!isRunning()) {
                                        System.err.println("[" + name + "] " + "Failed to connect to Instance: Shut down");
                                        return;
                                    }

                                    manager.getPlugin().getProxy().getScheduler().schedule(manager.getPlugin(), that, 2, TimeUnit.SECONDS);
                                    return;
                                }

                                event.getPlayer().connect(event.getTarget());
                            });
                        }
                    }, 1, TimeUnit.SECONDS);
                });
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
                        try {
                            stop();
                        } catch (IOException e) {
                            System.err.println("[" + name + "] " + "Failed to stop:" + e.getMessage());
                        }
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
                        try {
                            stop();
                        } catch (IOException e) {
                            System.err.println("[" + name + "] " + "Failed to stop:" + e.getMessage());
                        }
                    }
                }, getStopDelay(), TimeUnit.SECONDS);
            }
        }
    }

}