package me.yamakaja.jarinjector

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Created by Yamakaja on 04.06.17.
 */

class ReplacementClassVisitor(visitor: ClassVisitor, val replacements: Map<String, String>) : ClassVisitor(Opcodes.ASM5, visitor) {

    override fun visitMethod(access: Int, name: String?, desc: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor {
        return ReplacementMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions), replacements)
    }

}

class ReplacementMethodVisitor(visitor: MethodVisitor, val replacements: Map<String, String>) : MethodVisitor(Opcodes.ASM5, visitor) {

    override fun visitLdcInsn(cst: Any?) {
        var output = cst
        if (cst is String)
            replacements.forEach {
                output = cst.replace(it.key, it.value)
            }

        super.visitLdcInsn(output)
    }

}
