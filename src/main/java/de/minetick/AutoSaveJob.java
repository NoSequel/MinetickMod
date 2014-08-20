package de.minetick;

import java.lang.reflect.Field;

import org.bukkit.Bukkit;
import org.bukkit.event.world.WorldSaveEvent;

import net.minecraft.server.ExceptionWorldConflict;
import net.minecraft.server.FileIOThread;
import net.minecraft.server.IProgressUpdate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegionFileCache;
import net.minecraft.server.WorldServer;

public class AutoSaveJob {

    public enum JobDetail {
        CLEAR_REGION_CACHE
    }

    private WorldServer worldserver;
    private boolean lastQueuedWorld;
    private boolean clearRegionCache;

    public AutoSaveJob() {
        this.worldserver = null;
        this.clearRegionCache = false;
        this.lastQueuedWorld = false;
    }

    public AutoSaveJob(WorldServer worldserver) {
        this();
        this.worldserver = worldserver;
    }

    public AutoSaveJob(JobDetail detail) {
        this();
        if(detail.equals(JobDetail.CLEAR_REGION_CACHE)) {
            this.clearRegionCache = true;
        }
    }

    public void markAsLastQueuedWorld() {
        if(this.worldserver != null) {
            this.lastQueuedWorld = true;
        }
    }

    /**
     * 
     * @return true if the job shall be removed from the autosave queue
     * @throws ExceptionWorldConflict
     */

    public boolean process() throws ExceptionWorldConflict {
        if(this.worldserver != null) {
            MinecraftServer.av().info("[AutoSave] Saving world " + this.worldserver.getWorld().getName());
            this.worldserver.save(true, (IProgressUpdate) null);
            WorldSaveEvent event = new WorldSaveEvent(this.worldserver.getWorld());
            Bukkit.getPluginManager().callEvent(event);
            this.worldserver = null;
            if(this.lastQueuedWorld) {
                this.setFileIONoDelay(true);
            }
        } else if(this.clearRegionCache) {
            if(this.isFileIOThreadDone()) {
                MinecraftServer.av().info("[AutoSave] Clearing RegionFileCache ...");
                RegionFileCache.a();
                this.setFileIONoDelay(false);
            } else {
                return false;
            }
        }
        return true;
    }

    private boolean isFileIOThreadDone() {
        boolean done = false;
        String error = null;
        try {
            Field field_c = FileIOThread.class.getDeclaredField("c");
            Field field_d = FileIOThread.class.getDeclaredField("d");
            field_c.setAccessible(true);
            field_d.setAccessible(true);
            done = (field_c.getLong(FileIOThread.a) == field_d.getLong(FileIOThread.a));
            field_c.setAccessible(false);
            field_d.setAccessible(false);
        } catch(NoSuchFieldException e) {
            error = e.toString();
        } catch(IllegalAccessException e) {
            error = e.toString();
        } catch(IllegalArgumentException e) {
            error = e.toString();
        }
        if(error != null) {
            MinecraftServer.av().error("Incompatibility to FileIOThread in method de.minetick.AutoSaveJob:isFileIOThreadDone()");
            MinecraftServer.av().error(error);
        }
        return done;
    }

    private void setFileIONoDelay(boolean enabled) {
        String error = null;
        try {
            Field field1 = FileIOThread.class.getDeclaredField("e");
            field1.setAccessible(true);
            field1.setBoolean(FileIOThread.a, enabled);
            field1.setAccessible(false);
        } catch(NoSuchFieldException e) {
            error = e.toString();
        } catch(IllegalAccessException e) {
            error = e.toString();
        } catch(IllegalArgumentException e) {
            error = e.toString();
        }
        if(error != null) {
            MinecraftServer.av().error("Incompatibility to FileIOThread in method de.minetick.AutoSaveJob:setFileIONoDelay(boolean enabled)");
            MinecraftServer.av().error(error);
        }
    }
}
