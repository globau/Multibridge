package au.com.grieve.multibridge.builder.Vanilla.patcher;

import org.objectweb.asm.*;

public class MCBrand extends ClassVisitor {

    public MCBrand(ClassWriter classWriter) {
        super(Opcodes.ASM5, classWriter);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (name.equals("getServerModName")) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            mv.visitCode();
            mv.visitLdcInsn("VanillaCord");
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
            return null;
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }
}