package me.yamakaja.jarinjector;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by Yamakaja on 07.06.17.
 */
public class JobManager {

    private ExecutorService threadPool = Executors.newFixedThreadPool(8);
    private Set<InjectionJob> jobs = new HashSet<>();
    private Map<String, String> replacements;

    public JobManager(Map<String, String> replacements) {
        this.replacements = replacements;
    }

    public void scheduleJob(InjectionJob job) {
        jobs.add(job);
        threadPool.submit(job);
    }

    public void awaitTermination() {
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(1000000, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Set<InjectionJob> getJobs() {
        return jobs;
    }

    public Map<String, String> getReplacements() {
        return replacements;
    }

}
