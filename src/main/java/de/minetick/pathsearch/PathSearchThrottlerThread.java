package de.minetick.pathsearch;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import de.minetick.MinetickThreadFactory;

public class PathSearchThrottlerThread implements Runnable {

    private static final int POOL_SIZE = 2;
    private ExecutorService pathFinder = Executors.newScheduledThreadPool(POOL_SIZE, new MinetickThreadFactory(Thread.MIN_PRIORITY, "MinetickMod_PathFinder"));
    private Future<?>[] activeJobs;
    private Object waitObject;
    private LinkedHashMap<PathSearchJob, PathSearchJob> filter;
    private Thread thread;
    private boolean running;

    public PathSearchThrottlerThread() {
        this.waitObject = new Object();
        this.filter = new LinkedHashMap<PathSearchJob, PathSearchJob>();
        this.activeJobs = new Future<?>[POOL_SIZE * 8];
        this.thread = new Thread(this);
        this.running = true;
        this.thread.setName("MinetickMod_PathSearchThrottlerThread");
        this.thread.setPriority(Thread.MIN_PRIORITY + 1);
        this.thread.start();
    }

    public void queuePathSearch(PathSearchJob job) {
        synchronized(this.filter) {
            if(this.filter.size() < 1000) {
                this.filter.put(job, job);
                this.wakeUp();
            }
        }
    }

    public void wakeUp() {
        synchronized(this.waitObject) {
            this.waitObject.notifyAll();
        }
    }

    public void shutdown() {
        this.running = false;
        this.pathFinder.shutdownNow();
    }

    private boolean checkPendingJobs() {
        boolean nothingToDo = this.filter.isEmpty();
        for(int i = 0; i < this.activeJobs.length; i++) {
            Future<?> f = this.activeJobs[i];
            if(f == null || f.isDone()) {
                this.activeJobs[i] = null;
                if(!this.filter.isEmpty()) {
                    PathSearchJob job = null;
                    synchronized(this.filter) {
                        Iterator<Entry<PathSearchJob, PathSearchJob>> iter = this.filter.entrySet().iterator();
                        if(iter.hasNext()) {
                            job = iter.next().getValue();
                            iter.remove();
                        }
                    }
                    if(job != null) {
                        try {
                            this.activeJobs[i] = this.pathFinder.submit(job);
                            nothingToDo = false;
                        } catch (RejectedExecutionException exception) {
                            
                        }
                    }
                }
            } else {
                nothingToDo = false;
            }
        }
        return nothingToDo;
    }

    @Override
    public void run() {
        int i = 0;
        while(this.running) {
            i++;
            boolean isBusy = this.checkPendingJobs();
            if(i > 10) {
                i = 0;
                synchronized(this.waitObject) {
                    try {
                        this.waitObject.wait(isBusy ? 1L : 10L);
                    } catch (InterruptedException e) {}
                }
            }
        }
    }
}
