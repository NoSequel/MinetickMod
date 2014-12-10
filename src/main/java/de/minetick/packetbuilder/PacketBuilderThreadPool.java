package de.minetick.packetbuilder;

import java.util.ArrayList;

public class PacketBuilderThreadPool {

    private boolean active;
    private ArrayList<PacketBuilderThread> threads;
    private static PacketBuilderThreadPool instance;
    private int jobAssignmentCounter = 0;
    private Object lockObject = new Object();

    public PacketBuilderThreadPool(int poolsize) {
        instance = this;
        this.threads = new ArrayList<PacketBuilderThread>();
        adjustPoolSize(poolsize);
        this.active = true;
    }

    public static void addJobStatic(PacketBuilderJobInterface job) {
        if(instance != null) {
            instance.addJob(job);
        }
    }

    public void addJob(PacketBuilderJobInterface job) {
        if(this.active && !this.threads.isEmpty()) {
            boolean jobAdded = false;
            synchronized(this.lockObject) {
                int attempts = 0;
                while(!jobAdded && attempts < this.threads.size()) {
                    attempts++;
                    this.jobAssignmentCounter = (this.jobAssignmentCounter + 1) % this.threads.size();
                    jobAdded = this.threads.get(this.jobAssignmentCounter).addJob(job);
                }
            }
        }
    }

    public static void shutdownStatic() {
        if(instance != null) {
            instance.shutdown();
        }
    }

    private void shutdown() {
        this.active = false;
        for(PacketBuilderThread thread : this.threads) {
            thread.shutdown();
        }
    }

    private static int capInputSize(int size) {
        int newSize = Math.max(1, size);
        newSize = Math.min(newSize, 32);
        return newSize;
    }

    public static void adjustPoolSize(int size) {
        if(instance != null) {
            int newSize = capInputSize(size);
            while(instance.threads.size() < newSize) {
                instance.threads.add(new PacketBuilderThread());
            }
            while(instance.threads.size() > newSize) {
                PacketBuilderThread thread = instance.threads.remove(instance.threads.size() - 1);
                thread.shutdown();
            }
        }
    }
}
