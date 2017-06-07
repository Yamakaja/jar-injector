package me.yamakaja.jarinjector.bootstrap

import me.yamakaja.jarinjector.ConstantPoolParser
import java.io.*
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

/**
 * Created by Yamakaja on 05.06.17.
 */

fun main(args: Array<String>) {

    if (args.size < 2) {
        println("Usage: java -jar <injector.jar> <jar-to-edit> <replacement>+")
        return
    }

    val replacements = HashMap<String, String>(args.size)
    args.slice(1..args.size - 1).forEach {
        val parts = it.split("=")
        replacements[parts[0]] = parts[1]
    }

    val jarFile = JarFile(File(args[0]))
    val newFile = File("${args[0]}.injected")
    val output: JarOutputStream = JarOutputStream(FileOutputStream(newFile), jarFile.manifest)

    val enum = jarFile.entries()

    while (enum.hasMoreElements())
        processEntry(output, jarFile, enum.nextElement(), replacements)

    output.flush()
    output.close()

    Files.move(newFile.toPath(), File(args[0]).toPath(), StandardCopyOption.REPLACE_EXISTING)

}

fun processEntry(output: JarOutputStream, jar: JarFile, entry: JarEntry, replacements: Map<String, String>) {

    val outputEntry = JarEntry(entry.name)

    if (entry.name == "META-INF/MANIFEST.MF")
        return

    BufferedInputStream(jar.getInputStream(entry)).use {

        if (!entry.name.endsWith(".class")) {

            output.putNextEntry(entry)
            val buffer = ByteArray(1024)
            var count: Int
            while (true) {
                count = it.read(buffer)

                if (count < 0)
                    break

                output.write(buffer, 0, count)
            }
        } else {
            val resultStream = ByteArrayOutputStream(it.available())

            ConstantPoolParser(entry.name, DataInputStream(BufferedInputStream(it)), DataOutputStream(resultStream), replacements)

            val data = resultStream.toByteArray()
            resultStream.close()

            outputEntry.size = data.size.toLong()
            output.putNextEntry(outputEntry)
            output.write(data)
        }

        output.closeEntry()
    }
}
