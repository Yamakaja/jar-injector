package me.yamakaja.jarinjector;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * Created by Yamakaja on 07.06.17.
 */
public class Bootstrap {

    public static boolean debug = System.getProperty("jarinjector.debug") != null;

    public static void main(String[] args) {
        long time = System.currentTimeMillis();

        if (args.length < 3) {
            System.out.println("Usage: java -jar <injector.jar> <jar-to-edit> <replacement>+");
            return;
        }

        if (args.length % 2 != 1) {
            System.err.println("You have to provide a replacement for each target!");
        }

        Map<String, String> replacements = new ConcurrentHashMap<>();

        for (int i = 1; i < args.length; i += 2)
            try {
                replacements.put(args[i], args[i + 1]);
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("Invalid format: \"" + args[i] + "\"\n" +
                        "Correct format: <placeholder> <replacement>");
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

        if (debug) {
            System.out.println("Initialization: " + (System.currentTimeMillis() - time) + "ms");
            time = System.currentTimeMillis();
        }

        JobManager jobManager = new JobManager(replacements);

        try {
            File newFile = File.createTempFile("injection-tmp-", ".jar");
            newFile.deleteOnExit();
            JarFile jarFile = new JarFile(file);
            JarOutputStream jarOutputStream = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(newFile)), jarFile.getManifest());

            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements())
                processEntry(jobManager, jarFile, entries.nextElement(), jarOutputStream, replacements);

            if (debug) {
                System.out.println("Job creation: " + (System.currentTimeMillis() - time) + "ms");
                time = System.currentTimeMillis();
            }

            jobManager.awaitTermination();

            if (debug) {
                System.out.println("Waiting for jobs to finish: " + (System.currentTimeMillis() - time) + "ms");
                time = System.currentTimeMillis();
            }

            for (InjectionJob job : jobManager.getJobs()) {
                JarEntry entry = new JarEntry(job.getName());

                byte[] data = job.getOutputStream().toByteArray();
                entry.setSize(data.length);

                jarOutputStream.putNextEntry(entry);
                jarOutputStream.write(data);
                jarOutputStream.closeEntry();
            }

            if (debug)
                System.out.println("Writing parsed data: " + (System.currentTimeMillis() - time) + "ms");

            jarOutputStream.flush();
            jarOutputStream.close();

            Files.move(newFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (debug)
            System.out.println(ConstantPoolParser.timeCounter.doubleValue() / 1000000 + "ms were spent parsing class files!");
    }

    private static void processEntry(JobManager jobManager, JarFile jarFile, JarEntry jarEntry, JarOutputStream jarOutputStream, Map<String, String> replacements) {
        if (jarEntry.getName().equals("META-INF/MANIFEST.MF"))
            return;

        try (DataInputStream inputStream = new DataInputStream(new BufferedInputStream(jarFile.getInputStream(jarEntry)))) {

            if (!jarEntry.getName().endsWith(".class")) {
                jarOutputStream.putNextEntry(jarEntry);

                int count;
                byte[] buffer = new byte[1024];

                while ((count = inputStream.read(buffer)) > 0)
                    jarOutputStream.write(buffer, 0, count);

                jarOutputStream.closeEntry();
            } else
                jobManager.scheduleJob(new InjectionJob(jarEntry.getName(), inputStream, replacements));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    private static void processEntry(JarOutputStream jarOutputStream, JarFile jarFile, JarEntry jarEntry, Map<String, String> replacements) {
//        if (jarEntry.getName().equals("META-INF/MANIFEST.MF"))
//            return;
//
//        JarEntry outputEntry = new JarEntry(jarEntry.getName());
//
//        try (BufferedInputStream inputStream = new BufferedInputStream(jarFile.getInputStream(jarEntry))) {
//
//            if (!jarEntry.getName().endsWith(".class")) {
//                jarOutputStream.putNextEntry(jarEntry);
//
//                int count;
//                byte[] buffer = new byte[1024];
//
//                while ((count = inputStream.read(buffer)) > 0)
//                    jarOutputStream.write(buffer, 0, count);
//
//            } else {
//                ByteArrayOutputStream resultStream = new ByteArrayOutputStream(inputStream.available());
//
//                new ConstantPoolParser(jarEntry.getName(), new DataInputStream(inputStream), new DataOutputStream(resultStream), replacements);
//
//                byte[] data = resultStream.toByteArray();
//                resultStream.close();
//
//                outputEntry.setSize(data.length);
//                jarOutputStream.putNextEntry(outputEntry);
//                jarOutputStream.write(data);
//            }
//
//            jarOutputStream.closeEntry();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

}
