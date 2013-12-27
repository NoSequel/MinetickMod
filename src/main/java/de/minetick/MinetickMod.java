package de.minetick;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.bukkit.craftbukkit.CraftServer;

import de.minetick.profiler.Profiler;

import net.minecraft.server.Entity;
import net.minecraft.server.EntityArrow;
import net.minecraft.server.EntityEnderCrystal;
import net.minecraft.server.EntityEnderDragon;
import net.minecraft.server.EntityFireball;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.EntityProjectile;
import net.minecraft.server.EntityWither;
import net.minecraft.server.MinecraftServer;

public class MinetickMod {

    private TickTimer tickTimerObject;
    private int timerDelay = 45;
    private ScheduledExecutorService timerService = Executors.newScheduledThreadPool(2);
    private ScheduledFuture<Object> tickTimerTask;
    private static MinetickMod instance;
    private Profiler profiler;
    private ThreadPool threadPool;
    private boolean initDone = false;

    public MinetickMod() {
        this.tickTimerObject = new TickTimer();
        instance = this;
    }
    
    public void init() {
        if(!this.initDone) {
            this.initDone = true;
            CraftServer craftserver = MinecraftServer.getServer().server;
            this.profiler = new Profiler(craftserver.getMinetickModProfilerLogInterval(),
                    craftserver.getMinetickModProfilerWriteEnabled(),
                    craftserver.getMinetickModProfilerWriteInterval());
            this.threadPool = new ThreadPool(this.profiler);
        }
    }

    public Profiler getProfiler() {
        return this.profiler;
    }

    public ThreadPool getThreadPool() {
        return this.threadPool;
    }

    public static boolean isImportantEntity(Entity entity) {
        return (entity instanceof EntityArrow || entity instanceof EntityPlayer || 
                entity instanceof EntityProjectile || entity instanceof EntityFireball ||
                entity instanceof EntityWither || entity instanceof EntityEnderCrystal ||
                entity instanceof EntityEnderDragon);
    }
    
    public void shutdown() {
        this.timerService.shutdown();
    }

    public void checkTickTime(long tickTime) {         
        if(tickTime > 45000000L) {
            if(this.timerDelay > 20) {
                this.timerDelay--;
            }
        } else if(tickTime < 35000000L && this.timerDelay < 45) {
            this.timerDelay++;
        }
    }

    public void startTickTimerTask() {
        this.tickTimerTask = instance.timerService.schedule(this.tickTimerObject, this.timerDelay, TimeUnit.MILLISECONDS);
    }

    public void cancelTimerTask(boolean flag) {
        this.tickTimerTask.cancel(false);
    }

    private class TickTimer implements Callable<Object> {
        public Object call() {
            MinecraftServer.getServer().cancelHeavyCalculationsForAllWorlds(true);
            return null;
        }
    }
}
