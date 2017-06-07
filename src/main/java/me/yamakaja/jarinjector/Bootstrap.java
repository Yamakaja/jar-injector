package me.yamakaja.jarinjector;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * Created by Yamakaja on 07.06.17.
 */
public class Bootstrap {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java -jar <injector.jar> <jar-to-edit> <replacement>+");
            return;
        }

        Map<String, String> replacements = new HashMap<>();

        for (int i = 1; i < args.length; i++)
            try {
                String[] split = args[i].split("=");
                replacements.put(split[0], split[1]);
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("Invalid format: \"" + args[i] + "\"\n" +
                        "Correct format: <placeholder>=<replacement>");
                return;
            }

        File file = new File(args[0]);

        if (!file.exists()) {
            System.err.println("\"" + args[0] + "\" does not exist!");
            return;
        }

        if (file.isDirectory()) {
            System.err.println("\"" + args[0] + "\" is a directory!");
            return;
        }


        try {
            File newFile = File.createTempFile("injection-tmp-", ".jar");
            newFile.deleteOnExit();
            JarFile jarFile = new JarFile(file);
            JarOutputStream jarOutputStream = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(newFile)), jarFile.getManifest());

            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements())
                processEntry(jarOutputStream, jarFile, entries.nextElement(), replacements);

            jarOutputStream.flush();
            jarOutputStream.close();

            Files.move(newFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void processEntry(JarOutputStream jarOutputStream, JarFile jarFile, JarEntry jarEntry, Map<String, String> replacements) {
        if (jarEntry.getName().equals("META-INF/MANIFEST.MF"))
            return;

        JarEntry outputEntry = new JarEntry(jarEntry.getName());

        try (BufferedInputStream inputStream = new BufferedInputStream(jarFile.getInputStream(jarEntry))) {

            if (!jarEntry.getName().endsWith(".class")) {
                jarOutputStream.putNextEntry(jarEntry);

                int count;
                byte[] buffer = new byte[1024];

                while ((count = inputStream.read(buffer)) > 0)
                    jarOutputStream.write(buffer, 0, count);

            } else {
                ByteArrayOutputStream resultStream = new ByteArrayOutputStream(inputStream.available());

                new ConstantPoolParser(jarEntry.getName(), new DataInputStream(inputStream), new DataOutputStream(resultStream), replacements);

                byte[] data = resultStream.toByteArray();
                resultStream.close();

                outputEntry.setSize(data.length);
                jarOutputStream.putNextEntry(outputEntry);
                jarOutputStream.write(data);
            }

            jarOutputStream.closeEntry();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
