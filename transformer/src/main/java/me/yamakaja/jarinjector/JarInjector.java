package me.yamakaja.jarinjector;

import java.util.Map;

/**
 * Created by Yamakaja on 05.06.17.
 */
public class JarInjector {

    public JarInjector(String jar, Map<String,String> replacements) {
        new JarInj(jar, replacements).inject();
    }

}
