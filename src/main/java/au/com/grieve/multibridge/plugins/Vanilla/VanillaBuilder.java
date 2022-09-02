package au.com.grieve.multibridge.plugins.Vanilla;

/*
 * Makes use of VanillaCord
 *
 * https://github.com/ME1312/VanillaCord
 */

import au.com.grieve.multibridge.MultiBridge;
import au.com.grieve.multibridge.interfaces.InstanceBuilder;
import au.com.grieve.multibridge.objects.Instance;
import au.com.grieve.multibridge.plugins.Vanilla.util.Version;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonStreamParser;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Map;
import net.md_5.bungee.config.Configuration;

public class VanillaBuilder implements InstanceBuilder {

  private final MultiBridge plugin;

  public VanillaBuilder(MultiBridge plugin) {
    this.plugin = plugin;
  }

  /** Called when an instance is first built. */
  public void build(Instance instance) throws IOException {
    Configuration config = instance.getTemplateConfig();

    if (!config.contains("build.vanilla")) {
      return;
    }

    String version = config.getString("build.vanilla.version", "latest");
    String output = config.getString("build.vanilla.output", "server.jar");

    Path cacheFolder = plugin.getDataFolder().toPath().resolve("cache");
    Path workingFolder = cacheFolder.resolve("temp-" + String.valueOf(new Date().getTime()));
    Files.createDirectories(workingFolder);

    System.out.println(
        "[VanillaBuilder] [" + instance.getName() + "] Loading Vanilla Manifest for: " + version);
    JsonObject vanillaManifest = getVanillaManifest(version);

    assert vanillaManifest != null;
    Path patchedServerPath =
        cacheFolder.resolve(
            "vanilla-patched-" + vanillaManifest.get("id").getAsString() + ".server.jar");

    // If the patched server does not exist we need to patch vanilla
    if (!Files.exists(patchedServerPath)) {
      Path vanillaServerPath =
          cacheFolder.resolve(
              "original-" + vanillaManifest.get("id").getAsString() + ".server.jar");

      // If vanillaServer does not exist we download it first
      try {
        if (!Files.exists(vanillaServerPath)
            || sha1(vanillaServerPath).equals(vanillaManifest.get("sha1").getAsString())) {
          System.out.println(
              "[VanillaBuilder] [" + instance.getName() + "] Downloading Vanilla Server");
          downloadFile(
              new URL(
                  vanillaManifest
                      .getAsJsonObject("downloads")
                      .getAsJsonObject("server")
                      .get("url")
                      .getAsString()),
              vanillaServerPath);
        }
      } catch (NoSuchAlgorithmException e) {
        throw new IOException("Can't find SHA1 algorithm");
      }

      // Patch vanilla
      patchServer(cacheFolder, workingFolder, version, vanillaServerPath, patchedServerPath);
    }

    // Copy file to output
    Files.copy(patchedServerPath, instance.getInstanceFolder().resolve(output));
    System.out.println("[VanillaBuilder] [" + instance.getName() + "] Finished");
  }

  /** Get Vanilla Server Manifest */
  private JsonObject getVanillaManifest(String version) throws IOException {
    JsonObject versionManifest =
        getJson(new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json"));

    if (versionManifest == null) {
      throw new IOException("Unable to load Version Manifest");
    }

    if (version.equalsIgnoreCase("latest")) {
      version = versionManifest.getAsJsonObject("latest").get("release").getAsString();
    }

    for (JsonElement element : versionManifest.getAsJsonArray("versions")) {
      JsonObject v = element.getAsJsonObject();
      if (v.get("id").getAsString().equalsIgnoreCase(version)) {
        return getJson(new URL(v.get("url").getAsString()));
      }
    }

    return null;
  }

  /** Patch the Vanilla File to work behind BungeeCord. */
  private void patchServer(
      Path cacheFolder, Path workingFolder, String version, Path input, Path output)
      throws IOException, IllegalArgumentException {
    JsonObject patchVersionManifest =
        getJson(
            new URL(
                "https://raw.githubusercontent.com/ME1312/VanillaCord/master/version_manifest.json"));

    JsonObject patchObject = null;
    Version patchVersion = null;
    Version serverVersion = new Version(version);

    assert patchVersionManifest != null;
    for (Map.Entry<String, JsonElement> entry : patchVersionManifest.entrySet()) {
      Version v = new Version(entry.getValue().getAsJsonObject().get("id").getAsString());
      if ((patchVersion == null || patchVersion.compareTo(v) < 0)
          && serverVersion.compareTo(v) >= 0) {
        patchVersion = v;
        patchObject = entry.getValue().getAsJsonObject();
      }
    }
    if (patchVersion == null)
      throw new IllegalArgumentException("Could not find patches for: " + version);

    JsonObject patchProfile = getJson(new URL(patchObject.get("url").getAsString()));
    assert patchProfile != null;
    patchVersion = new Version(patchProfile.get("id").getAsString());

    // Download VanillaCord
    Path patcherFile = cacheFolder.resolve("vanillacord-" + patchVersion + ".jar");
    if (!Files.exists(patcherFile)) {
      Path tempPatcherFile = workingFolder.resolve("vanillacord-" + patchVersion + ".jar");
      downloadFile(
          new URL(patchProfile.getAsJsonObject("download").get("url").getAsString()),
          tempPatcherFile);
      Files.move(tempPatcherFile, patcherFile);
    }

    // Copy Original Server to location expected by VanillaCord
    Files.createDirectory(workingFolder.resolve("in"));
    Files.createDirectory(workingFolder.resolve("out"));
    Files.copy(patcherFile, workingFolder.resolve("vanillacord.jar"));
    Files.copy(input, workingFolder.resolve("in").resolve(version + ".jar"));

    // Execute
    ProcessBuilder builder = new ProcessBuilder("java", "-jar", "vanillacord.jar", version);
    builder.redirectErrorStream(true);
    builder.directory(workingFolder.toFile());
    Process process = builder.start();

    //        OutputStream stdin = process.getOutputStream();
    InputStream stdout = process.getInputStream();

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(stdout))) {
      //            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
      for (String line; ((line = reader.readLine()) != null); ) {
        System.out.println(line);
      }
    } catch (IOException ignored) {
    }

    // Copy Patched file to output
    Files.copy(workingFolder.resolve("out").resolve(version + "-bungee.jar"), output);
  }

  /** Download a file */
  private void downloadFile(URL input, Path output) throws IOException {
    try {
      ReadableByteChannel readableByteChannel = Channels.newChannel(input.openStream());
      FileOutputStream fileOutputStream = new FileOutputStream(output.toFile());
      FileChannel fileChannel = fileOutputStream.getChannel();
      fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
    } catch (Throwable e) {
      Files.delete(output);
      throw e;
    }
  }

  /**
   * Calculate the SHA1 of a file
   *
   * @param path File to check
   * @return SHA-1 Checksum
   */
  private String sha1(Path path) throws IOException, NoSuchAlgorithmException {
    MessageDigest sha1;
    sha1 = MessageDigest.getInstance("SHA-1");
    try (InputStream input = Files.newInputStream(path)) {
      byte[] buffer = new byte[8192];
      int len;
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

  /** Return a JSON Object for a URL */
  private JsonObject getJson(URL url) throws IOException {
    JsonStreamParser jsonParser =
        new JsonStreamParser(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));
    if (jsonParser.hasNext()) {
      return jsonParser.next().getAsJsonObject();
    }
    return null;
  }
}
