package me.yamakaja.jarinjector.bootstrap

import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Created by Yamakaja on 05.06.17.
 */

fun main(args: Array<String>) {

    if (args.size < 2) {
        println("Usage: java -jar <injector.jar> <jar-to-edit> <replacement>+")
        return
    }

    val file = File.createTempFile("injector-", ".jar")
    file.deleteOnExit()

    Files.copy(Thread.currentThread().contextClassLoader.getResource("injector.jar").openStream(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)

    val classLoader = URLClassLoader(arrayOf(file.toURI().toURL(),
            File(args[0]).toURI().toURL()))

    val replacements = HashMap<String, String>(args.size)
    args.slice(1..args.size - 1).forEach {
        val parts = it.split("=")
        replacements[parts[0]] = parts[1]
    }

    Class.forName("me.yamakaja.jarinjector.JarInjector", true, classLoader)
            .declaredConstructors[0].newInstance(args[0], replacements)
}
