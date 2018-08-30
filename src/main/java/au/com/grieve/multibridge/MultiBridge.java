package au.com.grieve.multibridge;

import au.com.grieve.multibridge.commands.MultiBridgeCommand;
import au.com.grieve.multibridge.instance.InstanceManager;
import au.com.grieve.multibridge.template.TemplateManager;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.nio.file.Files;

public class MultiBridge extends Plugin {
    private Configuration config;
    private TemplateManager templateManager;
    private InstanceManager instanceManager;

    @Override
    public void onEnable() {
        // Load Config
        try {
            loadConfig();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load/save configuration file", e);
        }

        // Load Managers
        templateManager = new TemplateManager(this);
        instanceManager = new InstanceManager(this);

        // Register Commands
        getProxy().getPluginManager().registerCommand(this, new MultiBridgeCommand(this));

    }

    @Override
    public void onDisable() {
        // Close Instances
        instanceManager.stopAll();
    }

    private void loadConfig() throws IOException {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        File file = new File(getDataFolder(), "config.yml");

        if (!file.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            }
        }

        Configuration defaults = ConfigurationProvider.getProvider(YamlConfiguration.class).load(getResourceAsStream("config.yml"));
        config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file, defaults);


//        // Defaults
//        Map<String, String> defaults = new HashMap<String, String>()
//
//        if (!config.contains("instancesFolder")) {
//            config.set("instancesFolder", "live/instances");
//        }
    }

    /**
     * Return the Template Manager
     */
    public TemplateManager getTemplateManager() {
        return templateManager;
    }

    /**
     * Return the Instance Manager
     */
    public InstanceManager getInstanceManager() {
        return instanceManager;
    }

    /**
     * Return the Config
     */
    public Configuration getConfig() {
        return config;
    }

}
