package de.minetick;

import net.minecraft.server.WorldServer;

import de.minetick.profiler.Profiler;
import de.minetick.profiler.WorldProfile;
import de.minetick.profiler.WorldProfile.WorldProfileSection;

public class WorldTicker implements Runnable {
    private WorldServer worldToTick;
    private String worldName;
    private LockObject lock;

    private Profiler profiler;

    public WorldTicker(WorldServer world, Profiler prof, LockObject lock) {
        this.profiler = prof;
        this.worldToTick = world;
        this.worldName = this.worldToTick.getWorld().getName();
        this.lock = lock;
    }

    @Override
    public void run() {
        WorldProfile worldProfile = this.profiler.getWorldProfile(this.worldName);
        worldProfile.start();
        try {
            this.worldToTick.tickEntities();
        } catch (Throwable throwable1) {
            System.out.println(throwable1.getMessage());
            throwable1.printStackTrace();
        }
        worldProfile.start(WorldProfileSection.UPDATE_PLAYERS);
        synchronized(this.lock.updatePlayersLock) {
            this.worldToTick.getTracker().updatePlayers();
        }
        worldProfile.stop(WorldProfileSection.UPDATE_PLAYERS);

        worldProfile.start(WorldProfileSection.CHUNK_LOADING);
        this.worldToTick.loadAndGenerateChunks();
        worldProfile.stop(WorldProfileSection.CHUNK_LOADING);
        worldProfile.stop();
        worldProfile.setCurrentPlayerNumber(this.worldToTick.players.size());
        this.worldToTick.setLastTickAvg(worldProfile.getLastThreadAvg());
    }
}
