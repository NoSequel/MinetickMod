package de.minetick;

import java.util.Observable;

import net.minecraft.server.WorldServer;

import de.minetick.profiler.Profile;
import de.minetick.profiler.Profiler;
import de.minetick.profiler.WorldProfile;
import de.minetick.profiler.WorldProfile.WorldProfileSection;

public class WorldTicker extends Observable implements Runnable {
    private boolean active;
    private WorldServer worldToTick;
    private String worldName = "None yet";
    private Object waitObj;
    private LockObject lock;

    private Profiler profiler;

    public WorldTicker(Profiler prof, LockObject lock) {
        this.profiler = prof;
        this.active = true;
        this.worldToTick = null;
        this.waitObj = new Object();
        this.lock = lock;
    }

    @Override
    public void run() {
        while(this.active) {
            if(this.worldToTick == null) {
                synchronized(this.waitObj) {
                    try {
                        this.waitObj.wait();
                    } catch (InterruptedException e) {}
                }
            } else {
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

                this.worldToTick = null;
                this.worldName = "None";
                this.setChanged();
                this.notifyObservers();
            }
        }	
    }

    public void startWorld(WorldServer ws) {
        this.worldToTick = ws;
        this.worldName = this.worldToTick.getWorld().getName();
        synchronized(this.waitObj) {
            this.waitObj.notifyAll();
        }
    }

    public void shutdown() {
        this.active = false;
        synchronized(this.waitObj) {
            this.waitObj.notifyAll();
        }
    }

    public String getLastTickedWorld() {
        return this.worldName;
    }

    public boolean isBusy() {
        return (this.worldToTick != null);
    }
}
