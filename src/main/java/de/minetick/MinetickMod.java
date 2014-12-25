package de.minetick;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
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
import java.util.logging.Logger;

import net.minecraft.server.Entity;
import net.minecraft.server.EntityArrow;
import net.minecraft.server.EntityComplexPart;
import net.minecraft.server.EntityEnderCrystal;
import net.minecraft.server.EntityEnderDragon;
import net.minecraft.server.EntityFireball;
import net.minecraft.server.EntityFireworks;
import net.minecraft.server.EntityGhast;
import net.minecraft.server.EntityInsentient;
import net.minecraft.server.EntityLiving;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.EntityProjectile;
import net.minecraft.server.EntityWeather;
import net.minecraft.server.EntityWither;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.NBTCompressedStreamTools;
import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.Packet51MapChunk;
import net.minecraft.server.Packet56MapChunkBulk;
import net.minecraft.server.PlayerChunkMap;
import net.minecraft.server.WorldServer;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.EntityType;

import de.minetick.MinetickChunkCoordComparator.ChunkPriority;
import de.minetick.antixray.AntiXRay;
import de.minetick.modcommands.AntiXRayCommand;
import de.minetick.modcommands.PacketCompressionCommand;
import de.minetick.modcommands.PacketsPerTickCommand;
import de.minetick.modcommands.TPSCommand;
import de.minetick.modcommands.ThreadPoolsCommand;
import de.minetick.modcommands.WorldStatsCommand;
import de.minetick.packetbuilder.PacketBuilderThreadPool;
import de.minetick.pathsearch.PathSearchJob;
import de.minetick.pathsearch.PathSearchThrottlerThread;
import de.minetick.profiler.Profiler;

public class MinetickMod {

    private PacketBuilderThreadPool packetBuilderPool;
    private int availableProcessors;
    private int antixrayPoolSize;
    private int packetbuilderPoolSize;
    private Profiler profiler;
    private int timerDelay = 45;
    private ScheduledExecutorService timerService = Executors.newScheduledThreadPool(2, new MinetickThreadFactory(Thread.NORM_PRIORITY + 2, "MinetickMod_TimerService"));
    private ExecutorService nbtFileService = Executors.newCachedThreadPool(new MinetickThreadFactory(Thread.NORM_PRIORITY - 2, "MinetickMod_NBTFileSaver"));
    private ExecutorService worldTickerService = Executors.newCachedThreadPool(new MinetickThreadFactory(Thread.NORM_PRIORITY + 1, "MinetickMod_WorldTicker"));
    private PathSearchThrottlerThread pathSearchThrottler;
    private LockObject worldTickerLock = new LockObject();
    private ScheduledFuture<Object> tickTimerTask;
    private TickTimer tickTimerObject;
    private TickCounter tickCounterObject;
    private List<Integer> ticksPerSecond;
    private int ticksCounter = 0;
    private HashSet<String> notGeneratingWorlds;
    private int maxEntityLifeTime = 10;
    private HashSet<EntityType> entitiesToDelete;
    private HashSet<EntityType> entitiesWithOffloadedPathSearches;
    private Logger logger = Logger.getLogger("Minecraft");
    private int[] activationRange = new int[] { 16, 64, 88 };
    private int minimumPathSearchOffloadDistance = 8;
    private File configFile;
    private FileConfiguration modConfig;

    private static boolean initDone = false;
    private static MinetickMod instance;
    public static final int defaultPacketCompression = 7;

    public MinetickMod() {
        this.availableProcessors = Runtime.getRuntime().availableProcessors();
        this.tickTimerObject = new TickTimer();
        this.tickCounterObject = new TickCounter();
        this.ticksPerSecond = Collections.synchronizedList(new LinkedList<Integer>());
        this.timerService.scheduleAtFixedRate(this.tickCounterObject, 1, 1, TimeUnit.SECONDS);
        this.notGeneratingWorlds = new HashSet<String>();
        this.entitiesToDelete = new HashSet<EntityType>();
        this.entitiesWithOffloadedPathSearches = new HashSet<EntityType>();
        this.pathSearchThrottler = new PathSearchThrottlerThread();
        this.configFile = new File("viewdistance.yml");
        instance = this;
    }

    public void init() {
        if(!initDone) {
            initDone = true;
            CraftServer craftserver = MinecraftServer.getServer().server;
            craftserver.getCommandMap().register("tps", "MinetickMod", new TPSCommand("tps"));
            craftserver.getCommandMap().register("antixray", "MinetickMod", new AntiXRayCommand("antixray"));
            craftserver.getCommandMap().register("threadpools", "MinetickMod", new ThreadPoolsCommand("threadpools"));
            craftserver.getCommandMap().register("packetspertick", "MinetickMod", new PacketsPerTickCommand("packetspertick"));
            craftserver.getCommandMap().register("packetcompression", "MinetickMod", new PacketCompressionCommand("packetcompression"));
            craftserver.getCommandMap().register("worldstats", "MinetickMod", new WorldStatsCommand("worldstats"));

            this.profiler = new Profiler(craftserver.getMinetickModProfilerLogInterval(),
                                         craftserver.getMinetickModProfilerWriteEnabled(),
                                         craftserver.getMinetickModProfilerWriteInterval());
            AntiXRay.setWorldsFromConfig(craftserver.getMinetickModOrebfuscatedWorlds());
            ChunkGenerationPolicy.setRatesFromConfig(craftserver.getMinetickModMaxChunkGenerationRates());
            int axrps = craftserver.getMinetickModAntiXRayPoolSize();
            if(axrps <= 0 || axrps > 64) {
                axrps = this.availableProcessors;
            }
            this.antixrayPoolSize = axrps;
            AntiXRay.adjustThreadPoolSize(this.antixrayPoolSize);
            int pbps = craftserver.getMinetickModPacketBuilderPoolSize();
            if(pbps <= 0 || pbps > 64) {
                pbps = this.availableProcessors;
            }
            this.packetbuilderPoolSize = pbps;
            this.packetBuilderPool = new PacketBuilderThreadPool(this.packetbuilderPoolSize);
            int level = craftserver.getMinetickModCompressionLevel();
            if(level < 1 || level > 9) {
                level = defaultPacketCompression;
            }
            Packet51MapChunk.changeCompressionLevel(level);
            Packet56MapChunkBulk.changeCompressionLevel(level);
            int packets = craftserver.getMinetickModPacketsPerTick();
            if(packets < 1 || packets > 20) {
                packets = 1;
            }
            PlayerChunkManager.packetsPerTick = packets;
            craftserver.getMinetickModPacketChunkRates(ChunkPriority.values());

            List<String> worlds = craftserver.getMinetickModNotGeneratingWorlds();
            for(String w: worlds) {
                this.notGeneratingWorlds.add(w.toLowerCase());
            }
            this.maxEntityLifeTime = craftserver.getMinetickModMaxEntityLifeTime();
            List<String> entitiesToDelete = craftserver.getMinetickModEntitiesWithLimitedLifeTime();
            for(String name: entitiesToDelete) {
                try {
                    EntityType type = EntityType.valueOf(name.toUpperCase());
                    this.entitiesToDelete.add(type);
                } catch (IllegalArgumentException e) {
                    logger.warning("[MinetickMod] Settings: Skipping \"" + name + "\", as it is not a constant in org.bukkit.entity.EntityType!");
                }
            }

            this.loadActivationRange(craftserver.getMinetickModActivationRange(this.activationRange));

            List<String> entitiesWithOffloadedPathSearches = craftserver.getMinetickModEntitiesWithOffloadedPathSearches();
            for(String name: entitiesWithOffloadedPathSearches) {
                try {
                    EntityType type = EntityType.valueOf(name.toUpperCase());
                    this.entitiesWithOffloadedPathSearches.add(type);
                } catch (IllegalArgumentException e) {
                    logger.warning("[MinetickMod] Settings: Skipping \"" + name + "\", as it is not a constant in org.bukkit.entity.EntityType!");
                }
            }
            this.minimumPathSearchOffloadDistance = craftserver.getMinetickModMinimumTargetDistanceForOffloading(this.minimumPathSearchOffloadDistance);

            try {
                if(!this.configFile.exists()) {
                    this.configFile.createNewFile();
                }
                this.modConfig = YamlConfiguration.loadConfiguration(this.configFile);
            } catch (IOException e) {
                log.error(e.toString());
                e.printStackTrace();
            }
        }
    }

    private void loadActivationRange(int[] ranges) {
        setActivationRange(ranges[0], ranges[1], ranges[2]);
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

    public void startTickTimerTask() {
        this.tickTimerTask = instance.timerService.schedule(this.tickTimerObject, this.timerDelay, TimeUnit.MILLISECONDS);
    }

    public void cancelTimerTask(boolean flag) {
        this.tickTimerTask.cancel(false);
    }

    public void increaseTickCounter() {
        this.ticksCounter++;
    }

    public static Integer[] getTicksPerSecond() {
        return instance.ticksPerSecond.toArray(new Integer[0]);
    }

    public static boolean doesWorldNotGenerateChunks(String worldName) {
        return instance.notGeneratingWorlds.contains(worldName.toLowerCase());
    }

    public void shutdown() {
        this.timerService.shutdown();
        this.pathSearchThrottler.shutdown();
        PacketBuilderThreadPool.shutdownStatic();
        AntiXRay.shutdown();
        this.nbtFileService.shutdown();
        while(!this.nbtFileService.isTerminated()) {
            try {
                if(!this.nbtFileService.awaitTermination(3, TimeUnit.MINUTES)) {
                    logger.warning("MinetickMod is still waiting for NBT Files to be saved.");
                }
            } catch(InterruptedException e) {}
        }
        try {
            if(this.modConfig != null) {
                this.modConfig.save(this.configFile);
            }
        } catch (IOException e){
            log.error("Exception while saving view distance settings");
            e.printStackTrace();
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

    private class TickTimer implements Callable<Object> {
        public Object call() {
            MinecraftServer.getServer().cancelHeavyCalculationsForAllWorlds(true);
            return null;
        }
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
            try {
                long start = System.currentTimeMillis();
                FileOutputStream fileoutputstream = new FileOutputStream(this.file);
                NBTCompressedStreamTools.a(this.compound, (OutputStream) fileoutputstream);
                fileoutputstream.close();
                long duration = System.currentTimeMillis() - start;
                if(duration > 1000L) {
                    logger.info("Saving the file \"" + this.file.getAbsolutePath() + "\" took " + ((float)(duration/100L) / 10.0F) + " seconds.");
                }
            } catch (Exception e) {
                logger.severe("Error \""+ e.getMessage() +"\" while saving file: " + this.file.getAbsolutePath());
                e.printStackTrace();
            }
            this.compound = null;
            this.file = null;
            return null;
        }
    }

    public static int getMaxEntityLifeTime() {
        return instance.maxEntityLifeTime;
    }

    public static boolean isEntityAllowedToBeDeleted(EntityLiving entity) {
        return !isImportantEntity(entity) && instance.entitiesToDelete.contains(entity.getBukkitEntity().getType());
    }

    public Future<?> tickWorld(WorldServer worldServer) {
        return this.worldTickerService.submit(new WorldTicker(worldServer, this.profiler, this.worldTickerLock));
    }

    public static void queuePathSearch(PathSearchJob pathSearchJob) {
        instance.pathSearchThrottler.queuePathSearch(pathSearchJob);
    }

    public static int[] getActivationRange() {
        return instance.activationRange;
    }

    public static double getEntityDeleteRange() {
        double max = (double) instance.activationRange[2];
        return max * max;
    }

    public static boolean setActivationRange(int low, int high, int max) {
        if(instance != null && low > 4 && low < 144 && high >= low && high < 144) {
            instance.activationRange[0] = low;
            instance.activationRange[1] = high;
            if(max > 0) {
                instance.activationRange[2] = max;
            }
            return true;
        }
        return false;
    }

    public static boolean isPathSearchOffloadedFor(EntityInsentient entity) {
        return instance.entitiesWithOffloadedPathSearches.contains(entity.getBukkitEntity().getType());
    }

    public static double getMinimumPathSearchOffloadDistance() {
        return instance.minimumPathSearchOffloadDistance;
    }

    public static int minimumViewDistance() {
        return 3;
    }

    public static boolean setPlayerViewDistance(String playerName, int viewDistance) {
        if(instance.modConfig != null) {
            instance.modConfig.set(playerName.toLowerCase(), Math.max(viewDistance, minimumViewDistance()));
            return true;
        }
        return false;
    }

    public static int getPlayerViewDistance(String playerName, PlayerChunkMap map) {
        int defaultVD = map.getViewDistance();
        if(instance.modConfig != null) {
            int playerVD = instance.modConfig.getInt(playerName.toLowerCase(), defaultVD);
            playerVD = Math.max(playerVD, minimumViewDistance());
            return Math.min(playerVD, defaultVD);
        }
        return defaultVD;
    }
}
