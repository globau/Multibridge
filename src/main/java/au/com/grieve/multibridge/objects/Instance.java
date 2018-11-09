package au.com.grieve.multibridge.objects;

import au.com.grieve.multibridge.managers.InstanceManager;
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
        STARTING,
        STARTED,
        STOPPING,
        STOPPED,
        BUSY,
    }

    // Variables
    private InstanceManager manager;
    private Configuration instanceConfig;
    private Configuration templateConfig;
    private Path instanceFolder;
    private String name;
    private Integer port;
    private boolean bungeeRegistered = false;
    private State state = State.STOPPED;

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

        // Register ourselves as a Listener
        manager.getPlugin().getProxy().getPluginManager().registerListener(manager.getPlugin(), this);

        loadConfig();

        // Handle StartMode
        switch(getStartMode()) {
            case SERVER_START:
                manager.getPlugin().getProxy().getScheduler().schedule(manager.getPlugin(), () -> {
                    System.out.println("[" + name + "] " + "Auto-Starting: Server Start");
                    try {
                        start();
                    } catch (IOException e) {
                        System.err.println("[" + name + "] " + "Failed to Start: " + e.getMessage());
                    }
                }, getStartDelay(), TimeUnit.SECONDS);
                break;
        }

        updateAuto();
    }

    /**
     * Cleanup Instance before destroying
     */
    public void cleanUp() {

        // Unregister ourself as a Listener
        manager.getPlugin().getProxy().getPluginManager().unregisterListener(this);
    }

    private void loadConfig() {
        Path instanceConfigPath = instanceFolder.resolve("instance.yml");
        Path templateConfigPath = instanceFolder.resolve("template.yml");

        try {
            templateConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(templateConfigPath.toFile());
        } catch (IOException e) {
            templateConfig = new Configuration();
        }

        try {
            instanceConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(instanceConfigPath.toFile());
        } catch (IOException e) {
            instanceConfig = new Configuration();
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
     * Return Tags set on this Instance
     */
    public Map<String, String> getLocalTags() {
        Map<String, String> tags = new HashMap<>();
        // Add Instance Settings
        if (instanceConfig.contains("tags")) {
            for (String k : instanceConfig.getSection("tags").getKeys()) {
                tags.put(k.toUpperCase(), instanceConfig.getSection("tags").getString(k));
            }
        }
        return tags;
    }

    /**
     * Get tag set on this instance
     */
    public String getLocalTag(String key) {
        return instanceConfig.getString("tags." + key.toUpperCase());
    }

    /**
     * Get effective tag on this instance
     */
    public Map<String, String> getTags() {
        Map<String, String> tags = new HashMap<>();

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

        // Refresh Builtins
        tags.put("MB_SERVER_IP", "127.0.0.1");
        tags.put("MB_SERVER_PORT", port == null?"unknown":port.toString());
        tags.put("MB_SERVER_NAME", name);

        return tags;
    }

    /**
     * Get Tags this instance requires to start
     */
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
     * Set Auto
     */
    public void setAuto(Boolean auto) {
        instanceConfig.set("auto.enabled", auto);
        saveConfig();
        updateAuto();
    }

    private void updateAuto() {
        if (getState() == State.STOPPED) {
            if (getAuto()) {
                registerBungee();
            } else {
                unregisterBungee();
            }
        }
    }

    public boolean getAuto() {
        return instanceConfig.getBoolean("auto.enabled", false);
    }

    /**
     * Start Instance
     */
    public void start() throws IOException {
        // Make sure we can start
        switch(getState()) {
            case STARTING:
                throw new IOException("Already Starting");
            case STARTED:
                throw new IOException("Already Started");
            case STOPPING:
                throw new IOException("Busy Stopping");
            case BUSY:
                throw new IOException("Instance is Busy");
        }

        List<String> missingTags = getMissingRequiredTags();
        if (missingTags.size() > 0) {
            throw new IOException("Missing required tags: " + String.join(", ", missingTags));
        }

        try {
            // Update State to STARTING
            setState(State.STARTING);

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
            process = builder.start();

            OutputStream stdin = process.getOutputStream();
            InputStream stdout = process.getInputStream();

            reader = new BufferedReader(new InputStreamReader(stdout));
            writer = new BufferedWriter(new OutputStreamWriter(stdin));

            manager.getPlugin().getProxy().getScheduler().runAsync(manager.getPlugin(), () -> {
                try {
                    // Get list of triggers
                    Map<Pattern, Configuration> triggers = new HashMap<>();
                    if (templateConfig.contains("triggers")) {
                        Configuration section = templateConfig.getSection("triggers");
                        triggers = section.getKeys().stream()
                                .filter(s -> section.getSection(s).getString("match", null) != null)
                                .collect(Collectors.toMap(s -> Pattern.compile(section.getSection(s).getString("match")), section::getSection));
                    }

                    for (String line; ((line = reader.readLine()) != null); ) {
                        // Check trigger
                        for (Pattern p : triggers.keySet()) {
                            Matcher m = p.matcher(line);
                            while (m.find()) {
                                System.out.println("[" + name + "] Found Match (" + p.pattern() + ").");
                                for (String cmd : triggers.get(p).getStringList("commands")) {
                                    for (int i = 1; i <= m.groupCount(); i++) {
                                        cmd = cmd.replace("$" + i, m.group(i));
                                    }

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

                        System.out.println("[" + name + "] " + line);
                    }
                } catch (IOException ignored) {
                } finally {
                    try {
                        reader.close();
                        reader = null;
                    } catch (IOException ignored) {
                    }
                }

                System.out.println("[" + name + "] " + "Instance Shut Down");

                process = null;
                reader = null;
                writer = null;

                setState(State.STOPPED);
                updateAuto();
            });

            // Wait for Server to respond to a ping
            System.out.println("[" + name + "] " + "Waiting for Instance to become available");

            manager.getPlugin().getProxy().getScheduler().schedule(manager.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    Runnable pingRunnable = this;

                    ServerInfo server = manager.getPlugin().getProxy().getServers().get(getName());

                    if (server == null) {
                        return;
                    }

                    server.ping((serverPing, ex) -> {
                        if (getState() == State.STARTING) {
                            if (serverPing == null) {
                                // Failed. Schedule to try again if we are still starting
                                manager.getPlugin().getProxy().getScheduler().schedule(manager.getPlugin(), pingRunnable, 2, TimeUnit.SECONDS);
                                return;
                            }

                            System.out.println("[" + name + "] " + "Instance has started");

                            // Instance has started
                            setState(State.STARTED);

                            // If we have startup commands lets schedule that now
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
                    });
                }
            }, 2, TimeUnit.SECONDS);

        } catch (Throwable e) {
            // Clean up if an error occurs
            process = null;
            setState(State.STOPPED);
            throw e;
        }
    }

    /**
     * Stop Instance
     */
    public void stop() throws IOException {
        // Make sure we can stop
        switch(getState()) {
            case STOPPING:
                throw new IOException("Already Stopping");
            case STOPPED:
                throw new IOException("Already Stopped");
            case BUSY:
                throw new IOException("Instance is Busy");
        }

        try {

            // Update out State
            setState(State.STOPPING);

            // Send Stop Commands
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

            // Maximum time to wait
            Date date = new Date();
            long graceTime = date.getTime() + templateConfig.getInt("stop.delay", 30);

            System.out.println("[" + name + "] " + "Waiting for Instance to shutdown");
            manager.getPlugin().getProxy().getScheduler().schedule(manager.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    if (getState() == State.STOPPING) {
                        if (date.getTime() < graceTime) {
                            manager.getPlugin().getProxy().getScheduler().schedule(manager.getPlugin(), this, 2, TimeUnit.SECONDS);
                            return;
                        }
                        System.err.print("[" + name + "] " + "Murdering Instance");
                        process.destroy();
                        return;
                    }
                    System.out.println("[" + name + "] " + "Instance Cleanly Shut Down");
                }
            }, 2, TimeUnit.SECONDS);

        } catch (Throwable e) {
            setState(State.STOPPED);
            throw e;
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
     * Remove Instance
     */
    @SuppressWarnings("unused")
    public Integer getPort() {
        return port;
    }

    public String getName() {
        return name;
    }

    /**
     * Get Tag
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    public String getTag(String key) {
        return getTag(key, null);
    }

    @SuppressWarnings("WeakerAccess")
    public String getTag(String key, String def) {
        return getTags().getOrDefault(key, def);
    }

    @SuppressWarnings("WeakerAccess")
    public int getTagInt(String key, int def) {
        try {
            return Integer.parseInt(getTag(key, String.valueOf(def)));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    @SuppressWarnings({"WeakerAccess", "unused"})
    public int getTagInt(String key) {
        return getTagInt(key, 0);
    }

    @SuppressWarnings("WeakerAccess")
    public boolean getTagBoolean(String key) {
        return getTagBoolean(key, false);
    }

    @SuppressWarnings("WeakerAccess")
    public boolean getTagBoolean(String key, boolean def) {
        return Boolean.parseBoolean(getTag(key, String.valueOf(def)));
    }

    public void setTag(String key, String value) {
        instanceConfig.set("tags." + key, value);
        saveConfig();
    }

    public void clearTag(String key) {
        instanceConfig.set("tags." + key, null);
        saveConfig();
    }

    /**
     * Return Startup Type
     */
    public StartMode getStartMode() {
        try {
            return StartMode.valueOf(instanceConfig.getString("auto.start.mode", "MANUAL"));
        } catch (IllegalArgumentException e) {
            return StartMode.MANUAL;
        }
    }

    public void setStartMode(StartMode mode) {
        instanceConfig.set("auto.start.mode", mode.toString());
        saveConfig();
    }

    /**
     * Return Startup Delay
     */
    public int getStartDelay() {
        return instanceConfig.getInt("auto.start.delay", 0);
    }

    public void setStartDelay(int delay) {
        instanceConfig.set("auto.start.delay", delay);
        saveConfig();
    }

    /**
     * Get Stop Type
     */
    public StopMode getStopMode() {
        try {
            return StopMode.valueOf(instanceConfig.getString("auto.stop.mode", "MANUAL"));
        } catch (IllegalArgumentException e) {
            return StopMode.MANUAL;
        }
    }

    public void setStopMode(StopMode mode) {
        instanceConfig.set("auto.stop.mode", mode.toString());
        saveConfig();
    }


    /**
     * Get Stop Delay
     */
    public int getStopDelay() {
        return instanceConfig.getInt("auto.stop.delay");
    }

    public void setStopDelay(int delay) {
        instanceConfig.set("auto.stop.delay", delay);
        saveConfig();
    }

    private List<String> getMissingRequiredTags() {
        Map<String, String> tags = getTags();
        List<String> missingTags = new ArrayList<>();
        for(String requiredTag: getRequiredTags()) {
            if (!tags.containsKey(requiredTag)) {
                missingTags.add(requiredTag);
            }
        }
        return missingTags;
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

    private void setState(State state) {
        this.state = state;
    }

    @SuppressWarnings("unused")
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
    public void reloadConfig() {
        loadConfig();
    }

    /**
     * Check if we need to start before a player logs into the server
     */
    @EventHandler
    public void onPreLogin(PreLoginEvent event) {
        if (getAuto() && getState() == State.STOPPED && getStartMode() == StartMode.SERVER_JOIN) {
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
            if (getAuto() && getState() == State.STOPPED && (getStartMode() == StartMode.INSTANCE_JOIN || getStartMode() == StartMode.SERVER_JOIN)) {
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

                    // Wait for Server to be up
                    manager.getPlugin().getProxy().getScheduler().schedule(manager.getPlugin(), new Runnable() {
                        @Override
                        public void run() {
                            switch(getState()) {
                                case STARTING:
                                    if (date.getTime() - startTime > (getStartDelay()*1000)) {
                                        System.err.println("[" + name + "] " + "Failed to connect to Instance: Timed out");
                                        break;
                                    }
                                    manager.getPlugin().getProxy().getScheduler().schedule(manager.getPlugin(), this, 2, TimeUnit.SECONDS);
                                    break;
                                case STARTED:
                                    // Send player to Server
                                    event.getPlayer().connect(event.getTarget());
                                    break;
                            }
                        }
                    }, 2, TimeUnit.SECONDS);
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
            if (getState() == State.STARTED && getStopMode() == StopMode.SERVER_EMPTY) {
                manager.getPlugin().getProxy().getScheduler().schedule(manager.getPlugin(), () -> {
                    System.err.println("Players: " + String.valueOf(manager.getPlugin().getProxy().getPlayers().size()));
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
        if (event.getTarget().getName().equals(name) && event.getTarget().getPlayers().size() < 2) {
            if (getState() == State.STARTED  && getStopMode() == StopMode.INSTANCE_EMPTY) {
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