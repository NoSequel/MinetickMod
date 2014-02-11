package de.minetick.profiler;

import java.util.LinkedList;
import java.util.List;

public class WorldProfile extends Profile {

    public final Profile doTick;
    public final Profile tickEntities;
    public final Profile tickTileEntities;
    public final Profile updatePlayers;
    public final Profile chunkLoading;

    public WorldProfile(int size, String ident, int avgMaxCount, boolean writeToFile, int writeInterval, int writeSteps) {
        super(size, ident, avgMaxCount, writeToFile, writeInterval, writeSteps);
        this.doTick = new Profile(size, ident + "_doTick", avgMaxCount, false, writeInterval, writeSteps);
        this.tickEntities = new Profile(size, ident + "_tickEntities", avgMaxCount, false, writeInterval, writeSteps);
        this.tickTileEntities = new Profile(size, ident + "_tickTileEntities", avgMaxCount, false, writeInterval, writeSteps);
        this.updatePlayers = new Profile(size, ident + "_updatePlayers", avgMaxCount, false, writeInterval, writeSteps);
        this.chunkLoading = new Profile(size, ident + "_chunkLoading", avgMaxCount, false, writeInterval, writeSteps);
    }

    @Override
    public long getLastAvg() {
        return this.doTick.getLastAvg() + this.tickEntities.getLastAvg() + this.tickTileEntities.getLastAvg() +
               this.updatePlayers.getLastAvg() + this.chunkLoading.getLastAvg();
    }

    public long getLastThreadAvg() {
        return this.tickEntities.getLastAvg() + this.tickTileEntities.getLastAvg() +
               this.updatePlayers.getLastAvg() + this.chunkLoading.getLastAvg();
    }

    public void start(WorldProfileSection section) {
        switch(section) {
        case DO_TICK:
            this.doTick.start();
            return;
        case TICK_ENTITIES:
            this.tickEntities.start();
            return;
        case TICK_TILEENTITIES:
            this.tickTileEntities.start();
            return;
        case UPDATE_PLAYERS:
            this.updatePlayers.start();
            return;
        case CHUNK_LOADING:
            this.chunkLoading.start();
            return;
        default:
            return;
        }
    }

    public void stop(WorldProfileSection section) {
        switch(section) {
        case DO_TICK:
            this.doTick.stop();
            return;
        case TICK_ENTITIES:
            this.tickEntities.stop();
            return;
        case TICK_TILEENTITIES:
            this.tickTileEntities.stop();
            return;
        case UPDATE_PLAYERS:
            this.updatePlayers.stop();
            return;
        case CHUNK_LOADING:
            this.chunkLoading.stop();
            return;
        default:
            return;
        }
    }

    @Override
    public void newTick(int index, int cnt) {
        this.doTick.newTick(index, cnt);
        this.tickEntities.newTick(index, cnt);
        this.tickTileEntities.newTick(index, cnt);
        this.updatePlayers.newTick(index, cnt);
        this.chunkLoading.newTick(index, cnt);
        if(cnt > this.counter) {
            this.counter = cnt;

            this.calcRecord(false);
            this.gatherRecords();
            if(this.writeEnabled) {
                if((this.counter % this.writeStep) == 0) {
                    this.writeToFile();
                }
            }
        }
    }

    private void gatherRecords() {
        List<String> data = new LinkedList<String>();
        data.add("T: " + this.getLastAvgFloat());
        data.add("B: " + this.doTick.getLastAvgFloat());
        data.add("E: " + this.tickEntities.getLastAvgFloat());
        data.add("TE: " + this.tickTileEntities.getLastAvgFloat());
        data.add("PU: " + this.updatePlayers.getLastAvgFloat());
        data.add("C: " + this.chunkLoading.getLastAvgFloat());
        data.add("P: " + this.getPlayerAvg());
        
        this.addToOutput(this.counter, this.currentTime(), data);
    }

    public enum WorldProfileSection {
        DO_TICK,
        TICK_ENTITIES,
        TICK_TILEENTITIES,
        UPDATE_PLAYERS,
        CHUNK_LOADING;
    }
}
