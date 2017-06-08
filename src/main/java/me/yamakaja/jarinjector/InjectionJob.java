package me.yamakaja.jarinjector;

import java.io.*;
import java.util.Map;

/**
 * Created by Yamakaja on 07.06.17.
 */
public class InjectionJob implements Runnable {

    private String name;
    private DataInputStream inputStream;
    private Map<String, String> replacements;
    private ByteArrayOutputStream outputStream;

    public InjectionJob(String name, DataInputStream inputStream, Map<String, String> replacements) {
        this.name = name;
        this.replacements = replacements;
        this.outputStream = new ByteArrayOutputStream();

        try {
            ByteArrayOutputStream intermediateStream = new ByteArrayOutputStream(inputStream.available());

            byte[] buffer = new byte[1024];
            int count;

            while ((count = inputStream.read(buffer)) > 0)
                intermediateStream.write(buffer, 0, count);

            this.inputStream = new DataInputStream(new ByteArrayInputStream(intermediateStream.toByteArray()));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        new ConstantPoolParser(name, new DataInputStream(inputStream), new DataOutputStream(outputStream), replacements);
    }

    public String getName() {
        return name;
    }

    public ByteArrayOutputStream getOutputStream() {
        return outputStream;
    }
}
