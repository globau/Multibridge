package au.com.grieve.multibridge.template;

import au.com.grieve.multibridge.MultiBridge;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TemplateManager {
    private final MultiBridge plugin;

    public TemplateManager(MultiBridge plugin) {
        this.plugin = plugin;
    }

    /**
     * Return our Template Folder
     */
    @SuppressWarnings("WeakerAccess")
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
     * Download a zipped Template from a URL to a new template folder
     */
    public Template downloadTemplate(String name, URL url) throws IOException {
        Path target = getTemplateFolder().resolve(name);
        if (Files.exists(target)) {
            throw new IOException("Folder '" + target.toString() + "' already exists.");
        }

        try (ZipInputStream zipStream = new ZipInputStream(url.openStream())) {
            byte[] buffer = new byte[2048];
            ZipEntry entry;

            Files.createDirectories(target);

            while ((entry = zipStream.getNextEntry()) != null) {
                Path entryPath = target.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    try (FileOutputStream outputStream = new FileOutputStream(target.resolve(entry.getName()).toFile())) {
                        int len;
                        while ((len = zipStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, len);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw e;
                    }
                }
            }
        }

        return getTemplate(name);
    }

}
