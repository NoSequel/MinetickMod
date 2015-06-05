package de.minetick;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.EntityType;

import de.minetick.MinetickChunkCoordComparator.ChunkPriority;
import de.minetick.antixray.AntiXRay;
import de.minetick.modcommands.AntiXRayCommand;
import de.minetick.modcommands.LoadedChunksCommand;
import de.minetick.modcommands.PacketCompressionCommand;
import de.minetick.modcommands.PacketsPerTickCommand;
import de.minetick.modcommands.ReloadSettingsCommand;
import de.minetick.modcommands.SetEntityActivationRange;
import de.minetick.modcommands.TPSCommand;
import de.minetick.modcommands.ThreadListCommand;
import de.minetick.modcommands.ThreadPoolsCommand;
import de.minetick.modcommands.WorldStatsCommand;
import de.minetick.packetbuilder.PacketBuilderThreadPool;
import de.minetick.pathsearch.PathSearchJob;
import de.minetick.pathsearch.PathSearchThrottlerThread;
import de.minetick.profiler.Profiler;

import net.minecraft.server.Block;
import net.minecraft.server.Entity;
import net.minecraft.server.EntityArrow;
import net.minecraft.server.EntityComplexPart;
import net.minecraft.server.EntityEnderCrystal;
import net.minecraft.server.EntityEnderDragon;
import net.minecraft.server.EntityFireball;
import net.minecraft.server.EntityFireworks;
import net.minecraft.server.EntityGhast;
import net.minecraft.server.EntityHuman;
import net.minecraft.server.EntityInsentient;
import net.minecraft.server.EntityLiving;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.EntityProjectile;
import net.minecraft.server.EntityWeather;
import net.minecraft.server.EntityWither;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.NBTCompressedStreamTools;
import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.NetworkManager;
import net.minecraft.server.PacketPlayOutMapChunk;
import net.minecraft.server.PacketPlayOutMapChunkBulk;
import net.minecraft.server.PlayerChunkMap;
import net.minecraft.server.WorldServer;

public class MinetickMod {

    private TickTimer tickTimerObject;
    private int timerDelay = 45;
    private ScheduledExecutorService timerService = Executors.newScheduledThreadPool(2, new MinetickThreadFactory(Thread.NORM_PRIORITY + 2, "MinetickMod_TimerService"));
    private ExecutorService nbtFileService = Executors.newSingleThreadScheduledExecutor(new MinetickThreadFactory(Thread.NORM_PRIORITY - 2, "MinetickMod_NBTFileSaver"));
    private ExecutorService worldTickerService = Executors.newCachedThreadPool(new MinetickThreadFactory(Thread.NORM_PRIORITY + 1, "MinetickMod_WorldTicker"));
    private PathSearchThrottlerThread pathSearchThrottler;
    private LockObject worldTickerLock = new LockObject();
    private ScheduledFuture<Object> tickTimerTask;
    private static MinetickMod instance;
    private Profiler profiler;
    private boolean initDone = false;
    private TickCounter tickCounterObject;
    private List<Integer> ticksPerSecond;
    private int ticksCounter = 0;
    private PacketBuilderThreadPool packetBuilderPool;
    private final Logger log = LogManager.getLogger();
    private MinetickModConfig mainConfig;
    private boolean failedToLoadConfig = false;

    public MinetickMod() {
        this.tickTimerObject = new TickTimer();
        this.tickCounterObject = new TickCounter();
        this.ticksPerSecond = Collections.synchronizedList(new LinkedList<Integer>());
        this.timerService.scheduleAtFixedRate(this.tickCounterObject, 1, 1, TimeUnit.SECONDS);
        instance = this;
        try {
            this.mainConfig = new MinetickModConfig(new File("minetickmod.yml"));
            this.pathSearchThrottler = new PathSearchThrottlerThread(this.mainConfig.getPathSearchPoolSize());
        } catch (IOException e) {
            this.failedToLoadConfig = true;
        } catch (InvalidConfigurationException e) {
            this.failedToLoadConfig = true;
        } catch (NullPointerException e) {
            // Bukkit's logger is not yet initialized at this point and throws an NPE when trying to log an yaml load fail
            this.failedToLoadConfig = true;
        }
    }

    public void init() {
        if(!this.initDone) {
            this.initDone = true;
            if(this.failedToLoadConfig) {
                throw new IllegalStateException("MinetickMod's config file minetickmod.yml could not be loaded. Check it for syntax errors.");
            }
            CraftServer craftserver = MinecraftServer.getServer().server;
            craftserver.getCommandMap().register("tps", "MinetickMod", new TPSCommand("tps"));
            craftserver.getCommandMap().register("packetspertick", "MinetickMod", new PacketsPerTickCommand("packetspertick"));
            craftserver.getCommandMap().register("packetcompression", "MinetickMod", new PacketCompressionCommand("packetcompression"));
            craftserver.getCommandMap().register("antixray", "MinetickMod", new AntiXRayCommand("antixray"));
            craftserver.getCommandMap().register("threadpools", "MinetickMod", new ThreadPoolsCommand("threadpools"));
            craftserver.getCommandMap().register("worldstats", "MinetickMod", new WorldStatsCommand("worldstats"));
            craftserver.getCommandMap().register("threadlist", "MinetickMod", new ThreadListCommand("threadlist"));
            craftserver.getCommandMap().register("loadedchunks", "MinetickMod", new LoadedChunksCommand("loadedchunks"));
            craftserver.getCommandMap().register("setentityactivationrange", "MinetickMod", new SetEntityActivationRange("setentityactivationrange"));
            craftserver.getCommandMap().register("minetickmod-reload", "MinetickMod", new ReloadSettingsCommand("minetickmod-reload"));
            this.profiler = new Profiler(this.mainConfig.getProfilerLogInterval(), this.mainConfig.getProfilerWriteEnabled(), this.mainConfig.getProfilerWriteInterval());
            this.packetBuilderPool = new PacketBuilderThreadPool(this.mainConfig.getPacketBuilderPoolSize());
        }
    }

    public Profiler getProfiler() {
        return this.profiler;
    }

    public static Profiler getProfilerStatic() {
        return instance.getProfiler();
    }

    public static boolean isImportantEntity(Entity entity) {
        return (entity instanceof EntityArrow || entity instanceof EntityPlayer || 
                entity instanceof EntityProjectile || entity instanceof EntityFireball ||
                entity instanceof EntityWither || entity instanceof EntityEnderCrystal ||
                entity instanceof EntityEnderDragon || entity instanceof EntityGhast ||
                entity instanceof EntityFireworks || entity instanceof EntityComplexPart ||
                entity instanceof EntityWeather);
    }

    public void shutdown() {
        this.timerService.shutdown();
        if(this.pathSearchThrottler != null) {
            this.pathSearchThrottler.shutdown();
        }
        PacketBuilderThreadPool.shutdownStatic();
        NetworkManager.setKeepConnectionsAlive(false);
        this.nbtFileService.shutdown();
        while(!this.nbtFileService.isTerminated()) {
            try {
                if(!this.nbtFileService.awaitTermination(3, TimeUnit.MINUTES)) {
                    log.warn("MinetickMod is still waiting for NBT Files to be saved.");
                }
            } catch(InterruptedException e) {}
        }
        if(getConfig() != null) {
            getConfig().saveViewDistances();
        }
    }

    public void checkTickTime(long tickTime) {         
        if(tickTime > 45000000L) {
            if(this.timerDelay > 40) {
                this.timerDelay--;
            }
        } else if(this.timerDelay < 45) {
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

    private class TickCounter implements Runnable {
        @Override
        public void run() {
            ticksPerSecond.add(ticksCounter);
            ticksCounter = 0;
            if(ticksPerSecond.size() > 30) {
                ticksPerSecond.remove(0);
            }
        }
    }

    public void increaseTickCounter() {
        this.ticksCounter++;
    }

    public static Integer[] getTicksPerSecond() {
        return instance.ticksPerSecond.toArray(new Integer[0]);
    }

    public static void saveNBTFileStatic(NBTTagCompound compound, File file) {
        instance.saveNBTFile(compound, file);
    }

    public void saveNBTFile(NBTTagCompound compound, File file) {
        this.nbtFileService.submit(new NBTFileSaver(compound, file));
    }

    private class NBTFileSaver implements Callable<Object> {

        private NBTTagCompound compound;
        private File file;

        public NBTFileSaver(NBTTagCompound compound, File file) {
            this.compound = compound;
            this.file = file;
        }

        public Object call() {
            FileOutputStream fileoutputstream = null;
            try {
                long start = System.currentTimeMillis();
                fileoutputstream = new FileOutputStream(this.file);
                NBTCompressedStreamTools.a(this.compound, (OutputStream) fileoutputstream);
                long duration = System.currentTimeMillis() - start;
                if(duration > 1000L) {
                    log.info("Saving the file \"" + this.file.getAbsolutePath() + "\" took " + ((float)(duration/100L) / 10.0F) + " seconds.");
                }
            } catch (Exception e) {
                log.error("Error \""+ e.getMessage() +"\" while saving file: " + this.file.getAbsolutePath());
                e.printStackTrace();
            } finally {
                if(fileoutputstream != null) {
                    try {
                        fileoutputstream.close();
                    } catch (IOException e) {}
                }
            }
            this.compound = null;
            this.file = null;
            return null;
        }
    }

    public Future<?> tickWorld(WorldServer worldServer) {
        return this.worldTickerService.submit(new WorldTicker(worldServer, this.profiler, this.worldTickerLock));
    }

    public static void queuePathSearch(PathSearchJob pathSearchJob) {
        instance.pathSearchThrottler.queuePathSearch(pathSearchJob);
    }

    public static MinetickModConfig getConfig() {
        return instance.mainConfig;
    }

    public static int getCustomOreRates(Block block, int def) {
        if(instance != null && getConfig() != null) {
            return instance.mainConfig.getCustomOreRates(block, def);
        }
        return def;
    }
}
