package au.com.grieve.multibridge.template;

import au.com.grieve.multibridge.MultiBridge;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TemplateManager {
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
                    .forEach(p -> {
                        Template template = getTemplate(p.getFileName().toString());
                        if (template != null) {
                            templates.put(p.getFileName().toString(), template);
                        }
                    });
        } catch (IOException ignored) {
        }

        return templates;
    }

    /**
     * Return a template
     */
    public Template getTemplate(String name) {
        Path templateFolder = getTemplateFolder().resolve(name);

        try {
            return new Template(templateFolder);
        } catch (InstantiationException e) {
            // @TODO maybe throw another exception
            return null;
        }
    }

    /**
     * Download a Template from a URL
     */
    public Template downloadTemplate(String name, URL url) throws IOException {
        throw new IOException("Not implemented yet");
    }
}
