package au.com.grieve.multibridge.template;

import au.com.grieve.multibridge.MultiBridge;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TemplateManager {
    /**
     * Definition of a Template
     */
    public class Template {
        public final Path location;
        public final Configuration config;

        public Template(Path location, Configuration config) {
            this.location = location;
            this.config = config;
        }

    }

    private final MultiBridge plugin;

    public TemplateManager(MultiBridge plugin) {
        this.plugin = plugin;
    }

    /**
     * Return our Template Folder
     */
    public Path getTemplateFolder() {
        Path path = Paths.get(plugin.getConfig().getString("templatesFolder"));

        // Prepend our DataFolder if this does not begin with a /
        return path.isAbsolute()?path:Paths.get(plugin.getDataFolder().toString(), path.toString());
    }

    /**
     * Return a list of Templates
     *
     * Loop through all folders under the templates folder and look for ones that contain a template.yml. Read this in
     * to add to the list of templates.
     *
     * @return List of Templates
     */
    public Map<String, Template> getTemplates() {
        Map<String, Template> templates = new HashMap<>();
        try (Stream<Path> paths = Files.list(getTemplateFolder())) {
            paths
                    .filter(Files::isDirectory)
                    .filter(p -> Files.exists(p.resolve("template.yml")))
                    .forEach(p -> templates.put(p.getFileName().toString(), getTemplate(p.getFileName().toString())));
        } catch (IOException ignored) {
        }

        return templates;
    }

    /**
     * Return a template
     */
    public Template getTemplate(String name) {
        Path templateFolder = getTemplateFolder().resolve(name);

        // Make sure Folder Exists
        if (!Files.exists(templateFolder)) {
            return null;
        }

        Path templateConfigFile = templateFolder.resolve("template.yml");

        // Make sure config file exists
        if (!Files.exists(templateConfigFile)) {
            return null;
        }

        Configuration config;
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(templateConfigFile.toFile());
        } catch (IOException e) {
            return null;
        }

        return new Template(templateFolder, config);

    }
}
