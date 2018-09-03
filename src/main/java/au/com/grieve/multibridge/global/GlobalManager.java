package au.com.grieve.multibridge.global;

import au.com.grieve.multibridge.MultiBridge;
import au.com.grieve.multibridge.instance.Instance;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class GlobalManager {
    private final MultiBridge plugin;
    private Configuration globalConfig;

    public GlobalManager(MultiBridge plugin) {
        this.plugin = plugin;

        loadConfig();
    }

    private void loadConfig() {
        try {
            globalConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(plugin.getDataFolder().toPath().resolve("global.yml").toFile());
        } catch (IOException e) {
            globalConfig = new Configuration();
        }
    }

    private void saveConfig() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(globalConfig, plugin.getDataFolder().toPath().resolve("global.yml").toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reloadConfig() {
        loadConfig();
    }

    public String getTag(String key) {
        return globalConfig.getString("tags." + key, null);
    }

    public void setTag(String key, String value) {
        globalConfig.set("tags." + key, value);
        saveConfig();

        // Update Instances
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            for(Instance instance: plugin.getInstanceManager().getInstances().values()) {
                instance.reloadConfig();
            }
        });
    }

    public void clearTag(String key) {
        globalConfig.set("tags." + key, null);
        saveConfig();

        // Update Instances
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            for(Instance instance: plugin.getInstanceManager().getInstances().values()) {
                instance.reloadConfig();
            }
        });
    }

    public Map<String, String> getTags() {
        Map<String, String> tags = new HashMap<>();

        for(String key: globalConfig.getSection("tags").getKeys()) {
            tags.put(key, globalConfig.getSection("tags").getString(key));
        }
        return tags;
    }

}
