package de.minetick.packetbuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.minetick.MinetickThreadFactory;

public class PacketBuilderThread {

    private int id;
    private static volatile int threadCounter = 0;
    private ExecutorService thread;
    private PacketBuilderBuffer buildBuffer;

    public PacketBuilderThread() {
        this.id = threadCounter++;
        this.thread = Executors.newSingleThreadExecutor(new MinetickThreadFactory(Thread.NORM_PRIORITY - 2, "MinetickMod_PacketBuilder-" + this.id));
        this.buildBuffer = new PacketBuilderBuffer();
    }

    public boolean addJob(PacketBuilderJobInterface job) {
        if(!this.thread.isShutdown()) {
            job.assignBuildBuffer(this.buildBuffer);
            this.thread.submit(job);
            return true;
        }
        return false;
    }

    public void shutdown() {
        this.thread.shutdown();
    }
}
