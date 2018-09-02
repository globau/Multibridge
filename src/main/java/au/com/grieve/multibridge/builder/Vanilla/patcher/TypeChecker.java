package au.com.grieve.multibridge.builder.Vanilla.patcher;

import org.objectweb.asm.*;

/**
 * TypeChecker
 * Source: https://github.com/ME1312/VanillaCord/blob/1.12/src/main/java/uk/co/thinkofdeath/vanillacord/TypeChecker.java
 */
public class TypeChecker extends ClassVisitor {

    private boolean handshakeListener;
    String hsName;
    String hsDesc;
    private boolean loginListener;

    public TypeChecker() {
        super(Opcodes.ASM5);
    }

    @Override
    public MethodVisitor visitMethod(int access, final String name, final String desc, String signature, String[] exceptions) {
        return new MethodVisitor(api) {

            @Override
            public void visitLdcInsn(Object cst) {
                if (cst instanceof String && ((String) cst).startsWith("multiplayer.disconnect.outdated_server")) {
                    handshakeListener = true;
                    hsName = name;
                    hsDesc = desc;
                }
                if ("Unexpected hello packet".equals(cst)) {
                    loginListener = true;
                }
            }
        };
    }

    public boolean isHandshakeListener() {
        return handshakeListener;
    }

    public boolean isLoginListener() {
        return loginListener;
    }
}