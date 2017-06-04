package me.yamakaja.jarinjector

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.zip.ZipOutputStream

/**
 * Created by Yamakaja on 04.06.17.
 */

fun main(args: Array<String>) {
    if (args.size < 2) {
        println("Usage: java -jar <jarfile> <jar-to-edit> <replacement>+")
        return
    }

    val replacements = HashMap<String, String>(args.size)
    args.slice(1..args.size - 1).forEach {
        val parts = it.split("=")
        replacements[parts[0]] = parts[1]
    }

    val injector = JarInjector(args[0], replacements)

    injector.inject()
}

class JarInjector(val jar: String, val replacements: Map<String, String>) {

    var output: ZipOutputStream = ZipOutputStream(FileOutputStream(File("$jar.injected")))

    fun inject() {
        replacements.forEach {
            println("${it.key} -> ${it.value}")
        }

        val jar = JarFile(jar)

        val enum = jar.entries()

        while (enum.hasMoreElements())
            processEntry(jar, enum.nextElement())

        output.close()
    }

    fun processEntry(jar: JarFile, entry: JarEntry) {

        var jarEntry = JarEntry(entry.name)

        if (entry.isDirectory)
            return

        jar.getInputStream(entry).use {
            output.putNextEntry(jarEntry)
            if (!entry.name.endsWith(".class")) {
                var buffer = ByteArray(1024, { 0 })
                var count: Int
                while (true) {
                    count = it.read(buffer)

                    if (count < 0)
                        return

                    output.write(buffer, 0, count)
                }
            } else {

                val classReader = ClassReader(it)
                val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES)
                val classInjector = ReplacementClassVisitor(classWriter, replacements)

                classReader.accept(classInjector, 0)

                val data = classWriter.toByteArray()
                jarEntry.size = data.size.toLong()
                output.write(data)
            }
            output.closeEntry()
        }

    }

}
