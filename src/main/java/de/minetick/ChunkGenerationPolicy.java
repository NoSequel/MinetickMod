package de.minetick;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.WorldType;

public class ChunkGenerationPolicy {

    private static HashMap<WorldType, Double> rateMap = new HashMap<WorldType, Double>();

    private int generatedChunks;
    private int ticks;

    public ChunkGenerationPolicy() {
        this.reset();
    }

    public void generatedChunk() {
        this.generatedChunks++;
    }

    public boolean isChunkGenerationCurrentlyAllowed(WorldType type) {
        if(this.ticks > 0) {
            Double entry = rateMap.get(type);
            double maxRate;
            if(entry == null) {
                maxRate = getDefaultRate(type);
            } else {
                maxRate = entry.doubleValue();
            }

            double currentRate = (double) this.generatedChunks / (double) this.ticks;
            if(currentRate < maxRate) {
                return true;
            }
        }
        return false;
    }

    public void reset() {
        this.generatedChunks = 0;
        this.ticks = 0;
    }

    public void newTick() {
        if(this.ticks >= 20 * 5) {
            this.reset();
        }
        this.ticks++;
    }

    public static void setRatesFromConfig(Map<WorldType, Double> rates) {
        rateMap.putAll(rates);
    }

    public static double getDefaultRate(WorldType type) {
        return type.equals(WorldType.FLAT) ? 1.0D : 0.5D;
    }
}
