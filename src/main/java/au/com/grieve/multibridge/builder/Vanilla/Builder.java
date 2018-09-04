package au.com.grieve.multibridge.builder.Vanilla;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

/**
 * Vanilla Builder
 */
public class Builder {

    VanillaBuilder manager;
    Path cacheFolder;
    Path workingFolder;

    Builder(VanillaBuilder manager) throws IOException {
        this.manager = manager;

        cacheFolder = manager.getPlugin().getDataFolder().toPath().resolve("cache");
        workingFolder = cacheFolder.resolve("temp-" + String.valueOf(new Date().getTime()));
        Files.createDirectories(workingFolder);
    }


}
