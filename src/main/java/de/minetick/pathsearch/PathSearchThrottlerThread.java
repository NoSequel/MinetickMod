package de.minetick.pathsearch;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.minetick.MinetickThreadFactory;

public class PathSearchThrottlerThread extends ThreadPoolExecutor {

    private int queueLimit;
    private LinkedHashMap<PathSearchJob, PathSearchJob> filter;
    private static PathSearchThrottlerThread instance;

    public PathSearchThrottlerThread(int poolSize) {
        super(poolSize, poolSize, 1L, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>(), new MinetickThreadFactory(Thread.MIN_PRIORITY, "MinetickMod_PathFinder"));
        instance = this;
        adjustPoolSize(poolSize);
        this.filter = new LinkedHashMap<PathSearchJob, PathSearchJob>();
    }

    public void queuePathSearch(PathSearchJob newJob) {
        if(newJob != null) {
            synchronized(this.filter) {
                if(this.filter.containsKey(newJob) || this.filter.size() < 1000) {
                    this.filter.put(newJob, newJob);
                }
            }
        }
        PathSearchJob jobToExecute = null;
        synchronized(this.filter) {
            Iterator<Entry<PathSearchJob, PathSearchJob>> iter = this.filter.entrySet().iterator();
            while(iter.hasNext() && this.getQueue().size() < this.queueLimit) {
                jobToExecute = iter.next().getValue();
                iter.remove();
                if(jobToExecute != null) {
                    this.execute(jobToExecute);
                }
                if(newJob != null) {
                    break;
                }
            }
        }
    }

    @Override
    public void shutdown() {
        this.getQueue().clear();
        super.shutdown();
    }

    @Override
    protected void afterExecute(Runnable runnable, Throwable throwable) {
        super.afterExecute(runnable, throwable);
        this.queuePathSearch(null);
    }

    public static void adjustPoolSize(int size) {
        if(instance != null) {
            if(size > instance.getMaximumPoolSize()) {
                instance.setMaximumPoolSize(size);
                instance.setCorePoolSize(size);
            } else if(size < instance.getMaximumPoolSize()) {
                instance.setCorePoolSize(size);
                instance.setMaximumPoolSize(size);
            }
            instance.queueLimit = size * 8;
        }
    }
}