package au.com.grieve.multibridge.objects;

import java.io.IOException;
import java.nio.file.Path;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class Template {
  private final Path templateFolder;
  private Configuration templateConfig;

  public Template(Path templateFolder) throws InstantiationException {
    this.templateFolder = templateFolder;

    try {
      loadConfig();
    } catch (IOException e) {
      throw new InstantiationException(e.getMessage());
    }
  }

  public void loadConfig() throws IOException {
    Path templateConfigPath = templateFolder.resolve("template.yml");

    try {
      templateConfig =
          ConfigurationProvider.getProvider(YamlConfiguration.class)
              .load(templateConfigPath.toFile());
    } catch (IOException e) {
      throw new IOException(
          "Cannot load template.yml. Is " + templateFolder.toString() + " a template?");
    }
  }

  public Path getTemplateFolder() {
    return templateFolder;
  }
}
