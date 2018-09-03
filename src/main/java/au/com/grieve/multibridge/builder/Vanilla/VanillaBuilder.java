package au.com.grieve.multibridge.builder.Vanilla;

/*
 * Please note that the Vanilla Patching code here came mostly from VanillaCord GitHub respository of ME1312 who forked it from maximvarentsov
 * who forked it from Thinkofname. As there was no easy way to use the code as a library I've re-written it slightly for this plugin but
 * wanted to give due credit. I've also subsequently discovered ME1312 has also written a Bungeecord management plugin which looks pretty
 * good called SubServer.
 *
 * https://github.com/ME1312/VanillaCord
 */

import au.com.grieve.multibridge.MultiBridge;
import au.com.grieve.multibridge.api.event.BuildEvent;
import au.com.grieve.multibridge.api.event.ReadyEvent;
import au.com.grieve.multibridge.builder.Vanilla.util.URLOverrideClassLoader;
import au.com.grieve.multibridge.builder.Vanilla.util.Version;
import com.google.gson.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class VanillaBuilder implements Listener {

    private final MultiBridge plugin;

    public VanillaBuilder(MultiBridge plugin) {
        this.plugin = plugin;
    }

    // Server Definition
    private class Server {
        private final String version;
        private final Path path;

        public Server(String version, Path path) {
            this.version = version;
            this.path = path;
        }

        public String getVersion() {
            return version;
        }

        public Path getPath() {
            return path;
        }
    }


    /**
     * Called when an instance is first built.
     */
    @EventHandler
    public void onBuild(BuildEvent event) {
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            Configuration config = event.getInstance().getTemplateConfig();
            if (!config.contains("build.vanilla")) {
                return;
            }

            String version = config.getString("build.vanilla.version", "latest");
            String output = config.getString("build.vanilla.output", "server.jar");

            Server server;

            try {
                System.out.println("[VanillaBuilder] [" + event.getInstance().getName() + "] Downloading Original Minecraft Server: " + version);
                server = getServer(version);
                patchServer(server, event.getInstance().getInstanceFolder().resolve(output));
                System.out.println("[VanillaBuilder] [" + event.getInstance().getName() + "] Finished: " + server.getPath().getFileName());
            } catch (IOException e) {
                System.err.println("[VanillaBuilder] [" + event.getInstance().getName() + "] Failed: " + e.getMessage());
                return;
            }

            event.getInstance().update();
        });
    }

    /**
     * Called to check if an instance is ready for this builder
     */
    @EventHandler
    public void onReady(ReadyEvent event) {
        event.setReady(false);
    }

    /**
     * Download Minecraft Server to cache
     */
    private Server getServer(String version) throws IOException {
        Path cacheFolder = plugin.getDataFolder().toPath().resolve("cache");
        if (!Files.exists(cacheFolder)) {
            Files.createDirectories(cacheFolder);
        }

        // Get Manifest
        JsonObject manifest = getManifest(version);

        if (manifest == null) {
            throw new IOException("Could not find server version for: " + version);
        }

        JsonObject server = manifest.get("downloads").getAsJsonObject().get("server").getAsJsonObject();

        Path serverFile = cacheFolder.resolve("original-" + manifest.get("id").getAsString() + ".server.jar");

        // Does serverFile exists and its SHA1 matches? We can stop.
        try {
            if (Files.exists(serverFile) && sha1(serverFile).equals(server.get("sha1").getAsString())) {
                return new Server(manifest.get("id").getAsString(), serverFile);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Can't find SHA1 algorithm");
        }

        // Download the file
        downloadFile(new URL(server.get("url").getAsString()), serverFile);

        return new Server(manifest.get("id").getAsString(), serverFile);
    }

    /**
     * Download a file
     */
    private void downloadFile(URL input, Path output) throws IOException {
        try {
            ReadableByteChannel readableByteChannel = Channels.newChannel(input.openStream());
            FileOutputStream fileOutputStream = new FileOutputStream(output.toFile());
            FileChannel fileChannel = fileOutputStream.getChannel();
            fileChannel
                    .transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        } catch (Throwable e) {
            Files.delete(output);
            throw e;
        }
    }

    /**
     * Calculate the SHA1 of a file
     * @param path File to check
     * @return SHA-1 Checksum
     */
    private String sha1(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest sha1;
        sha1 = MessageDigest.getInstance("SHA-1");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int len = 0;
            while ((len = input.read(buffer)) > 0) {
                sha1.update(buffer, 0, len);
            }

            byte[] digest = sha1.digest();
            StringBuilder output = new StringBuilder();
            for (byte aDigest : digest) {
                output.append(Integer.toString((aDigest & 0xff) + 0x100, 16).substring(1));
            }
            return output.toString();
        }
    }

    /**
     * Return the Manifest for the server version
     * @param version Version of Minecraft, can be 'latest'
     */
    private JsonObject getManifest(String version) {
        JsonObject versionManifest;
        try {
            versionManifest = getJson(new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json"));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        JsonObject latest = versionManifest.get("latest").getAsJsonObject();
        JsonArray versions = versionManifest.get("versions").getAsJsonArray();

        if (version.equalsIgnoreCase("latest")) {
            version = latest.get("release").getAsString();
        }

        for (JsonElement element : versions) {
            JsonObject v = element.getAsJsonObject();
            if (v.get("id").getAsString().equalsIgnoreCase(version)) {
                try {
                    return getJson(new URL(v.get("url").getAsString()));
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }

        return null;
    }

    /**
     * Return a JSON Object for a URL
     */
    private JsonObject getJson(URL url) throws IOException {
        JsonStreamParser jsonParser = new JsonStreamParser(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));
        if (jsonParser.hasNext()) {
            return jsonParser.next().getAsJsonObject();
        }
        return null;
    }

    /**
     * Patch the Vanilla File to work behind BungeeCord.
     */
    private void patchServer(Server server, Path output) throws IOException, IllegalArgumentException {
        Path cacheFolder = plugin.getDataFolder().toPath().resolve("cache");
        if (!Files.exists(cacheFolder)) {
            Files.createDirectories(cacheFolder);
        }

        JsonObject patchVersionManifest = getJson(new URL("https://raw.githubusercontent.com/ME1312/VanillaCord/master/version_manifest.json"));
        if (patchVersionManifest == null) {
            throw new IOException("Unable to downoad Patch Version Manifest");
        }

        JsonObject patchObject = null;
        Version patchVersion = null;
        Version serverVersion = new Version(server.getVersion());

        for (Map.Entry<String, JsonElement> entry : patchVersionManifest.entrySet()) {
            Version version = new Version(entry.getValue().getAsJsonObject().get("id").getAsString());
            if ((patchVersion == null  || patchVersion.compareTo(version) < 0)
                    && serverVersion.compareTo(version) >= 0) {
                patchVersion = version;
                patchObject = entry.getValue().getAsJsonObject();
            }
        } if (patchVersion == null) throw new IllegalArgumentException("Could not find patches for " + server.getVersion());

        JsonObject patchProfile = getJson(new URL(patchObject.get("url").getAsString()));
        patchVersion = new Version(patchProfile.get("id").getAsString());

        // Download VanillaCord
        Path patcherFile = cacheFolder.resolve("vanillacord-" + patchVersion + ".jar");
        if (!Files.exists(patcherFile)) {
            downloadFile(new URL(patchProfile.getAsJsonObject("download").get("url").getAsString()), patcherFile);
        }

        // Copy Original Server to location expected by VanillaCord
        Files.createDirectory(cacheFolder.resolve("in"));
        Files.copy(server.getPath(), cacheFolder.resolve("in").resolve(server.getVersion() + ".jar"));

        URLOverrideClassLoader loader = new URLOverrideClassLoader(new URL[]{patcherFile.toUri().toURL(), server.getPath().toUri().toURL()});
        try {
            loader.loadClass(patchProfile.get("launch").getAsString()).getDeclaredMethod("main", String[].class).invoke(null, (Object) new String[]{server.getVersion()});
        } catch (Exception e) {
            throw new IOException("Failed to patch: " + e.getMessage());
        }

        // Copy Patched file to our location
        Files.copy(cacheFolder.resolve("out").resolve(server.getVersion() + "-bungee.jar"), cacheFolder.resolve("vanilla-patched-" + server.getVersion() + ".jar"));
    }


}
