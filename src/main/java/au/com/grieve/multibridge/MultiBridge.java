package au.com.grieve.multibridge;

import au.com.grieve.multibridge.commands.MultiBridgeCommand;
import au.com.grieve.multibridge.managers.GlobalManager;
import au.com.grieve.multibridge.managers.InstanceManager;
import au.com.grieve.multibridge.managers.TemplateManager;
import au.com.grieve.multibridge.plugins.Vanilla.VanillaBuilder;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class MultiBridge extends Plugin {
    private Configuration config;
    private TemplateManager templateManager;
    private InstanceManager instanceManager;
    private GlobalManager globalManager;

    @Override
    public void onEnable() {
        // Load Config
        try {
            loadConfig();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load/save configuration file", e);
        }

        // Load Managers
        globalManager = new GlobalManager(this);
        templateManager = new TemplateManager(this);
        instanceManager = new InstanceManager(this);

        // Register Commands
        getProxy().getPluginManager().registerCommand(this, new MultiBridgeCommand(this));

        // Register Plugins
        instanceManager.registerBuilder(new VanillaBuilder(this));

    }

    @Override
    public void onDisable() {
        // Close Instances
        instanceManager.stopAll();
    }

    private void loadConfig() throws IOException {
        if (!getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
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
     * Return the Global Manager
     */
    public GlobalManager getGlobalManager() {
        return globalManager;
    }

    /**
     * Return the Config
     */
    public Configuration getConfig() {
        return config;
    }

}
