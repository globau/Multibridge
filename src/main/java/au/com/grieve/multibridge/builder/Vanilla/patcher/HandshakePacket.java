package au.com.grieve.multibridge.builder.Vanilla.patcher;

import org.objectweb.asm.*;

/**
 * Handshake Packet
 * Source: https://github.com/ME1312/VanillaCord/blob/1.12/src/main/java/uk/co/thinkofdeath/vanillacord/HandshakePacket.java
 */

public class HandshakePacket extends ClassVisitor {
    public HandshakePacket(ClassWriter classWriter) {
        super(Opcodes.ASM5, classWriter);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return new MethodVisitor(Opcodes.ASM5, super.visitMethod(access, name, desc, signature, exceptions)) {
            @Override
            public void visitIntInsn(int opcode, int operand) {
                if (opcode == Opcodes.SIPUSH && operand == 255) {
                    super.visitIntInsn(opcode, Short.MAX_VALUE);
                } else {
                    super.visitIntInsn(opcode, operand);
                }
            }
        };
    }
}