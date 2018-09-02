package au.com.grieve.multibridge.builder.Vanilla.patcher;

import com.google.common.io.ByteStreams;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Patch {
    public static void patch(Path input, Path output) throws IOException {
        try (ZipInputStream zipInput = new ZipInputStream(Files.newInputStream(input));
             ZipOutputStream zipOutput = new ZipOutputStream(Files.newOutputStream(output));
             InputStream helper = Patch.class.getResourceAsStream("au/com/grieve/multibridge/builder/Vanilla/patcher/resources/BungeeHelper.class")) {

            Map<String, byte[]> classes = new HashMap<>();
            Set<String> fileSet = new HashSet<>();

            ZipEntry entry;
            while ((entry = zipInput.getNextEntry()) != null) {
                if (!entry.getName().endsWith(".class")) {
                    if (fileSet.add(entry.getName())) {
                        zipOutput.putNextEntry(entry);
                        ByteStreams.copy(zipInput, zipOutput);
                    }
                    continue;
                }

                byte[] classData = ByteStreams.toByteArray(zipInput);
                classes.put(entry.getName(), classData);
            }

            String handshakePacket = null;
            String loginListener = null;
            String networkManager = null;

            for (Map.Entry<String, byte[]> e : new HashMap<>(classes).entrySet()) {
                byte[] classData = e.getValue();
                ClassReader reader = new ClassReader(classData);
                TypeChecker typeChecker = new TypeChecker();
                reader.accept(typeChecker, 0);

                if (typeChecker.isHandshakeListener()) {
                    // Patch Handshake
                    reader = new ClassReader(classData);
                    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                    HandshakeListener handshakeListener = new HandshakeListener(writer, typeChecker);
                    reader.accept(handshakeListener, 0);
                    classData = writer.toByteArray();
                    handshakePacket = handshakeListener.getHandshake();
                    networkManager = handshakeListener.getNetworkManager();
                } else if (typeChecker.isLoginListener()) {
                    loginListener = e.getKey();
                }
                classes.put(e.getKey(), classData);
            }

            // Increase the hostname field size
            {
                byte[] clazz = classes.get(handshakePacket + ".class");
                ClassReader reader = new ClassReader(clazz);
                ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                reader.accept(new HandshakePacket(classWriter), 0);
                clazz = classWriter.toByteArray();
                classes.put(handshakePacket + ".class", clazz);
            }
            // Inject the profile injector and force offline mode
            {
                byte[] clazz = classes.get(loginListener);
                ClassReader reader = new ClassReader(clazz);
                ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                reader.accept(new LoginListener(classWriter, networkManager), 0);
                clazz = classWriter.toByteArray();
                classes.put(loginListener, clazz);
            }
            // Change the server brand
            {
                byte[] clazz = classes.get("net/minecraft/server/MinecraftServer.class");
                ClassReader classReader = new ClassReader(clazz);
                ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                classReader.accept(new MCBrand(classWriter), 0);
                clazz = classWriter.toByteArray();
                classes.put("net/minecraft/server/MinecraftServer.class", clazz);
            }

            // Save files to zip
            for (Map.Entry<String, byte[]> e : classes.entrySet()) {
                zipOutput.putNextEntry(new ZipEntry(e.getKey()));
                zipOutput.write(e.getValue());
            }

            // Add Helper
            zipOutput.putNextEntry(new ZipEntry("au/com/grieve/multibridge/builder/Vanilla/patcher/resources/BungeeHelper.class"));
            ByteStreams.copy(helper, zipOutput);
        }
    }
}
