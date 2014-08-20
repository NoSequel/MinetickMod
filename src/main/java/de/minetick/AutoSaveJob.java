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
        WORLD_SAVE,
        WORLD_SAVEEVENT,
    }

    private WorldServer worldserver;
    private JobDetail jobDetail;

    public AutoSaveJob(JobDetail detail, WorldServer worldserver) {
        this.jobDetail = detail;
        this.worldserver = worldserver;
    }

    /**
     * 
     * @return true if the job shall be removed from the autosave queue
     * @throws ExceptionWorldConflict
     */

    public boolean process() throws ExceptionWorldConflict {
        if(this.isJob(JobDetail.WORLD_SAVE) && this.worldserver != null) {
            MinecraftServer.av().info("[AutoSave] Saving world " + this.worldserver.getWorld().getName());
            this.worldserver.save(true, (IProgressUpdate) null);
            this.setFileIONoDelay(true);
        } else if(this.isJob(JobDetail.WORLD_SAVEEVENT) && this.worldserver != null) {
            if(this.isFileIOThreadDone()) {
                this.setFileIONoDelay(false);
                RegionFileCache.a();
                WorldSaveEvent event = new WorldSaveEvent(this.worldserver.getWorld());
                Bukkit.getPluginManager().callEvent(event);
            } else {
                return false;
            }
        }
        this.worldserver = null;
        return true;
    }

    private boolean isJob(JobDetail detail) {
        if(this.jobDetail != null) {
            return this.jobDetail.equals(detail);
        }
        return false;
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
