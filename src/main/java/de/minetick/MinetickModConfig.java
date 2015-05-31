package de.minetick;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.server.Block;
import net.minecraft.server.EntityInsentient;
import net.minecraft.server.EntityLiving;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PacketPlayOutMapChunk;
import net.minecraft.server.PacketPlayOutMapChunkBulk;
import net.minecraft.server.PlayerChunkMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import com.google.common.base.Charsets;

import de.minetick.MinetickChunkCoordComparator.ChunkPriority;
import de.minetick.antixray.AntiXRay;
import de.minetick.packetbuilder.PacketBuilderThreadPool;

public class MinetickModConfig {

    private final Logger logger = LogManager.getLogger();
    private File configFile;
    private FileConfiguration configuration;
    private int[] activationRange = new int[] { 16, 64, 88 };
    private HashMap<Block, Integer> customOreRates = new HashMap<Block, Integer>();
    private HashSet<String> notGeneratingWorlds = new HashSet<String>();
    private HashSet<EntityType> entitiesWithOffloadedPathSearches = new HashSet<EntityType>();
    private HashSet<EntityType> entitiesToDelete = new HashSet<EntityType>();
    private File viewdistanceConfigFile;
    private FileConfiguration viewdistanceConfig;

    public MinetickModConfig(File configFile) {
        this.configFile = configFile;
        this.configuration = this.loadConfig(configFile);
        try {
            this.configuration.save(configFile);
        } catch(IOException e) {
            logger.error("[MinetickMod] Could not save config file minetickmod.yml!");
            logger.error(e.toString());
        }

        this.loadConfigContent();

        this.viewdistanceConfigFile = new File("viewdistance.yml");
        try {
            if(!this.viewdistanceConfigFile.exists()) {
                this.viewdistanceConfigFile.createNewFile();
            }
            this.viewdistanceConfig = YamlConfiguration.loadConfiguration(this.viewdistanceConfigFile);
        } catch (IOException e) {
            logger.error("[MinetickMod] Could not load the stored player view distances from viewdistance.yml: " + e.toString());
            e.printStackTrace();
        }
    }

    public void reload() {
        this.configuration = this.loadConfig(this.configFile);
        this.loadConfigContent();
    }

    private void loadConfigContent() {
        this.loadActivationRange(this.getActivationRange(this.activationRange));
        this.loadCustomOreRates(this.getCustomOreRates());
        this.loadNotGeneratingWorlds(this.getNotGeneratingWorlds());
        this.loadEntitiesWithOffloadedPathSearches(this.getEntitiesWithOffloadedPathSearches());
        this.loadEntitiesToDelete(this.getEntitiesWithLimitedLifeTime());
        this.applyPacketChunkRates(ChunkPriority.values());
        this.applyPacketCompression(this.getCompressionLevel());
        ChunkGenerationPolicy.setRatesFromConfig(this.getMaxChunkGenerationRates());
        AntiXRay.setWorldsFromConfig(this.getOrebfuscatedWorlds());
        PacketBuilderThreadPool.adjustPoolSize(this.getPacketBuilderPoolSize());
        PlayerChunkManager.packetsPerTick = this.getPacketsPerTick();
    }

    private void loadEntitiesToDelete(List<String> entitiesToDelete) {
        this.entitiesToDelete.clear();
        for(String name: entitiesToDelete) {
            try {
                EntityType type = EntityType.valueOf(name.toUpperCase());
                this.entitiesToDelete.add(type);
            } catch (IllegalArgumentException e) {
                logger.warn("[MinetickMod] Settings: Skipping \"" + name + "\", as it is not a constant in org.bukkit.entity.EntityType!");
            }
        }
    }

    private void loadEntitiesWithOffloadedPathSearches(List<String> entities) {
        this.entitiesWithOffloadedPathSearches.clear();
        for(String name: entities) {
            try {
                EntityType type = EntityType.valueOf(name.toUpperCase());
                this.entitiesWithOffloadedPathSearches.add(type);
            } catch (IllegalArgumentException e) {
                logger.warn("[MinetickMod] Settings: Skipping \"" + name + "\", as it is not a constant in org.bukkit.entity.EntityType!");
            }
        }
    }

    private void loadNotGeneratingWorlds(List<String> worlds) {
        this.notGeneratingWorlds.clear();
        for(String w: worlds) {
            this.notGeneratingWorlds.add(w.toLowerCase());
        }
    }

    private FileConfiguration loadConfig(File file) {
        YamlConfiguration config;
        if(file.exists() && file.isFile()) {
            config = YamlConfiguration.loadConfiguration(file);
            config.options().copyDefaults(true);
        } else {
            config = new YamlConfiguration();
            config.options().copyDefaults(true);
            File bukkitFile = new File("bukkit.yml");
            if(bukkitFile.exists() && bukkitFile.isFile()) {
                FileConfiguration bukkitConfig = YamlConfiguration.loadConfiguration(bukkitFile);
                String sectionName = "minetickmod";
                if(bukkitConfig.contains(sectionName)) {
                    ConfigurationSection section = bukkitConfig.getConfigurationSection(sectionName);
                    config.set(sectionName, section);
                    bukkitConfig.set(sectionName, null);
                    try {
                        bukkitConfig.save(bukkitFile);
                    } catch(IOException e) {}
                }
            }
        }
        InputStreamReader defConfigStream = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("configurations/minetickmod.yml"), Charsets.UTF_8);
        if(defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            config.setDefaults(defConfig);
        }
        return config;
    }

    public boolean getProfilerWriteEnabled() {
        return configuration.getBoolean("minetickmod.profiler-write-log");
    }

    public int getProfilerWriteInterval() {
        return configuration.getInt("minetickmod.profiler-write-interval", 30);
    }

    public int getProfilerLogInterval() {
        return configuration.getInt("minetickmod.profiler-log-interval", 10);
    }

    public List<String> getOrebfuscatedWorlds() {
        return configuration.getStringList("minetickmod.orebfuscatedWorlds");
    }

    public int getPacketBuilderPoolSize() {
        int threadcount = configuration.getInt("minetickmod.packetBuilderThreadPoolSize", 1);
        return Math.min(1, Math.max(threadcount, 32));
    }

    public int getCompressionLevel() {
        int level = configuration.getInt("minetickmod.packetCompressionLevel", 7);
        return Math.min(1, Math.max(level, 9));
    }

    public int getPacketsPerTick() {
        return configuration.getInt("minetickmod.packetsPerTick", 1);
    }

    public List<String> getNotGeneratingWorlds() {
        return configuration.getStringList("minetickmod.notGeneratingWorlds");
    }

    public int getMaxEntityLifeTime() {
        return configuration.getInt("minetickmod.maxEntityLifeTime", 10);
    }

    public List<String> getEntitiesWithLimitedLifeTime() {
        return configuration.getStringList("minetickmod.entitiesWithLimitedLifeTime");
    }

    public List<String> getCustomOreRates() {
        return configuration.getStringList("minetickmod.customOreRates");
    }

    public Map<org.bukkit.WorldType, Double> getMaxChunkGenerationRates() {
        ConfigurationSection section = configuration.getConfigurationSection("minetickmod.maxChunkGenerationRates");
        Set<String> subkeys = section.getKeys(false);
        Map<org.bukkit.WorldType, Double> rateMap = new HashMap<org.bukkit.WorldType, Double>();
        for(String key: subkeys) {
            org.bukkit.WorldType type = org.bukkit.WorldType.getByName(key);
            if(type != null) {
                double rate = section.getDouble(key, ChunkGenerationPolicy.getDefaultRate(type));
                if(rate < 0.25D) { rate = 0.25D; }
                rateMap.put(type, rate);
            } else {
                this.logger.warn("[MinetickMod] The WorldType '" + key + "' was not recognized in the setting minetickmod.maxChunkGenerationRates");
            }
        }
        return rateMap;
    }

    public int[] getActivationRange(int[] defaultRanges) {
        int low = configuration.getInt("minetickmod.activationRange.low", defaultRanges[0]);
        int high = configuration.getInt("minetickmod.activationRange.high", defaultRanges[1]);
        int max = configuration.getInt("minetickmod.activationRange.max", defaultRanges[2]);
        return new int[] {low, high, max};
    }

    public List<String> getEntitiesWithOffloadedPathSearches() {
        return configuration.getStringList("minetickmod.entitiesWithOffloadedPathSearches");
    }

    public int getMinimumTargetDistanceForOffloading() {
        return configuration.getInt("minetickmod.minimumTargetDistanceForOffloadedPathSearches", 8);
    }

    public void applyPacketChunkRates(ChunkPriority[] values) {
        ConfigurationSection section = configuration.getConfigurationSection("minetickmod.packetChunkRates");
        Set<String> subkeys = section.getKeys(false);
        for(String key: subkeys) {
            int chunkCount = section.getInt(key);
            if(chunkCount >= 1 && chunkCount <= PacketPlayOutMapChunkBulk.c()) {
                ChunkPriority priority = ChunkPriority.findEntry(key);
                if(priority != null) {
                    priority.setChunksPerPacket(chunkCount);
                } else {
                    this.logger.warn("[MinetickMod] The config entry minetickmod.packetChunkRates." + key + " is invalid. Removing it from the config.");
                    section.set(key, null);
                }
            }
        }
    }

    private void applyPacketCompression(int compressionLevel) {
        PacketPlayOutMapChunk.changeCompressionLevel(compressionLevel);
        PacketPlayOutMapChunkBulk.changeCompressionLevel(compressionLevel);
    }

    public boolean isBungeeCordSupportEnabled() {
        return configuration.getBoolean("minetickmod.bungeeCordSupport");
    }

    private void loadActivationRange(int[] ranges) {
        setActivationRange(ranges[0], ranges[1], ranges[2]);
    }

    private void loadCustomOreRates(List<String> customOreRates) {
        this.customOreRates.clear();
        for(String str: customOreRates) {
            String[] split = str.split(":");
            if(split.length == 2) {
                String block = split[0].trim().toLowerCase();
                String value = split[1].trim();
                int v = 0;
                try {
                    v = Integer.parseInt(value);
                } catch (NumberFormatException nfe) {
                    logger.warn("[MinetickMod] Could not parse integer " + value + " of config entry: " + str);
                    continue;
                }
                Block b = Block.b(block);
                if(b != null) {
                    this.customOreRates.put(b, v);
                } else {
                    logger.warn("[MinetickMod] Block " + block + " not recognised of config entry: " + str);
                }
            } else {
                logger.warn("[MinetickMod] Config entry \"" + str + "\" doesnt have the expected format: Block:value");
            }
        }
    }

    public boolean setActivationRange(int low, int high, int max) {
        if(low > 4 && low < 144 && high >= low && high < 144) {
            this.activationRange[0] = low;
            this.activationRange[1] = high;
            if(max > 0) {
                this.activationRange[2] = max;
            }
            return true;
        }
        return false;
    }

    public int[] getActivationRange() {
        return this.activationRange;
    }

    public double getEntityDeleteRange() {
        double max = (double) this.activationRange[2];
        return max * max;
    }

    public int getCustomOreRates(Block block, int def) {
        Integer out = null;
        if(this.customOreRates != null && (out = this.customOreRates.get(block)) != null) {
            return out.intValue();
        }
        return def;
    }

    public int getMinimumViewDistance() {
        return 3;
    }

    public boolean setPlayerViewDistance(String playerName, int viewDistance) {
        if(this.viewdistanceConfig != null) {
            this.viewdistanceConfig.set(playerName.toLowerCase(), Math.max(viewDistance, this.getMinimumViewDistance()));
            return true;
        }
        return false;
    }

    public int getPlayerViewDistance(String playerName, PlayerChunkMap map) {
        int defaultVD = map.getViewDistance();
        if(this.viewdistanceConfig != null) {
            int playerVD = this.viewdistanceConfig.getInt(playerName.toLowerCase(), defaultVD);
            playerVD = Math.max(playerVD, this.getMinimumViewDistance());
            return Math.min(playerVD, defaultVD);
        }
        return defaultVD;
    }

    protected void saveViewDistances() {
        try {
            if(this.viewdistanceConfig != null) {
                this.viewdistanceConfig.save(this.viewdistanceConfigFile);
            }
        } catch (IOException e){
            logger.error("Exception while saving view distance settings");
            e.printStackTrace();
        }
    }

    public boolean doesWorldNotGenerateChunks(String worldName) {
        return this.notGeneratingWorlds.contains(worldName.toLowerCase());
    }

    public boolean isPathSearchOffloadedFor(EntityInsentient entity) {
        return this.entitiesWithOffloadedPathSearches.contains(entity.getBukkitEntity().getType());
    }

    public boolean isEntityAllowedToBeDeleted(EntityLiving entity) {
        return !MinetickMod.isImportantEntity(entity) && this.entitiesToDelete.contains(entity.getBukkitEntity().getType());
    }
}
