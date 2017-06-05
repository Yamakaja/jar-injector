package me.yamakaja.jarinjector

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

/**
 * Created by Yamakaja on 04.06.17.
 */
class JarInj(val jar: String, val replacements: Map<String, String>) {

    val jarFile = JarFile(jar)
    val file = File("$jar.injected")
    var output: JarOutputStream = JarOutputStream(FileOutputStream(file), jarFile.manifest)

    fun inject() {
        replacements.forEach {
            println("${it.key} -> ${it.value}")
        }

        val enum = jarFile.entries()

        while (enum.hasMoreElements())
            processEntry(jarFile, enum.nextElement())

        output.close()

        Files.move(file.toPath(), File(jar).toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    fun processEntry(jar: JarFile, entry: JarEntry) {
        val jarEntry = JarEntry(entry.name)

        if (entry.name == "META-INF/MANIFEST.MF")
            return

        jar.getInputStream(entry).use {
            if (!entry.name.endsWith(".class")) {
                output.putNextEntry(entry)
                val buffer = ByteArray(1024, { 0 })
                var count: Int
                while (true) {
                    count = it.read(buffer)

                    if (count < 0)
                        break

                    output.write(buffer, 0, count)
                }
            } else {
                val bas = ByteArrayOutputStream()
                val buffer = ByteArray(1024)
                var count: Int

                while (true) {
                    count = it.read(buffer)

                    if (count < 0)
                        break

                    bas.write(buffer, 0, count)
                }

                val originalData = bas.toByteArray()

                jarEntry.size = originalData.size.toLong()

                try {
                    val classReader = ClassReader(originalData)
                    val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES)
                    val classInjector = ReplacementClassVisitor(classWriter, replacements)
                    classReader.accept(classInjector, 0)

                    val data = classWriter.toByteArray()
                    jarEntry.size = data.size.toLong()
                    output.putNextEntry(jarEntry)
                    output.write(data)

                } catch (ex: RuntimeException) {
                    println("[CNFE] Failed to transform ${entry.name}: Missing dependency")
                    output.putNextEntry(entry)
                    output.write(originalData)

                } catch (npe: NullPointerException) {
                    println("[NPE] Failed to transform ${entry.name}")
                    output.putNextEntry(entry)
                    output.write(originalData)

                } catch (ncdfe: NoClassDefFoundError) {
                    println("[NCDFE] Failed to transform ${entry.name}: Missing dependency")
                    output.putNextEntry(entry)
                    output.write(originalData)

                }

            }
        }
        output.closeEntry()

    }

}
