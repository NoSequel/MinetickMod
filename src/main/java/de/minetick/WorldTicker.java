package de.minetick;

import net.minecraft.server.WorldServer;

import de.minetick.profiler.Profiler;
import de.minetick.profiler.WorldProfile;
import de.minetick.profiler.WorldProfile.WorldProfileSection;

public class WorldTicker implements Runnable {
    private WorldServer worldToTick;
    private String worldName;
    private static Object updatePlayersLock = new Object();

    private Profiler profiler;

    public WorldTicker(WorldServer world, Profiler prof) {
        this.profiler = prof;
        this.worldToTick = world;
        this.worldName = this.worldToTick.getWorld().getName();
    }

    @Override
    public void run() {
        this.tickWorld(true);
    }

    public void tickWorld(boolean threaded) {
        WorldProfile worldProfile = this.getWorldProfile();
        try {
            this.worldToTick.tickEntities();
        } catch (Throwable throwable1) {
            System.out.println(throwable1.getMessage());
            throwable1.printStackTrace();
        }
        worldProfile.start(WorldProfileSection.UPDATE_PLAYERS);
        synchronized(updatePlayersLock) {
            this.worldToTick.getTracker().updatePlayers();
        }
        worldProfile.stop(WorldProfileSection.UPDATE_PLAYERS);

        if(threaded) {
            this.loadAndGenerateChunks(threaded);
        }
        worldProfile.setCurrentPlayerNumber(this.worldToTick.players.size());
        if(threaded) {
            this.worldToTick.setLastTickAvg(worldProfile.getLastThreadAvg());
        }
    }

    public void loadAndGenerateChunks(boolean threaded) {
        WorldProfile worldProfile = this.getWorldProfile();
        worldProfile.start(WorldProfileSection.CHUNK_LOADING);
        this.worldToTick.loadAndGenerateChunks();
        worldProfile.stop(WorldProfileSection.CHUNK_LOADING);
        if(!threaded) {
            this.worldToTick.setLastTickAvg(worldProfile.getLastAvg());
        }
    }

    public WorldProfile getWorldProfile() {
        return this.profiler.getWorldProfile(this.worldName);
    }
}
