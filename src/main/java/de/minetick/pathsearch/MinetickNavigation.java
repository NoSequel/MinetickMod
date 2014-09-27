package de.minetick.pathsearch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.util.BlockVector;

import de.minetick.MinetickMod;
import de.minetick.pathsearch.cache.SearchCacheEntry;
import de.minetick.pathsearch.cache.SearchCacheEntryEntity;
import de.minetick.pathsearch.cache.SearchCacheEntryPosition;

import net.minecraft.server.Entity;
import net.minecraft.server.EntityInsentient;
import net.minecraft.server.MathHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Navigation;
import net.minecraft.server.PathEntity;
import net.minecraft.server.World;

public class MinetickNavigation extends Navigation {

    private HashMap<UUID, SearchCacheEntry> searchCache;
    private HashMap<PositionPathSearchType, SearchCacheEntryPosition> positionSearchCache;
    private boolean offloadSearches;
    private double minimumDistanceForOffloadingSquared;
    private int cleanUpDelay = 0;

    public MinetickNavigation(EntityInsentient entityinsentient, World world) {
        super(entityinsentient, world);
        this.searchCache = new HashMap<UUID, SearchCacheEntry>();
        this.positionSearchCache = new HashMap<PositionPathSearchType, SearchCacheEntryPosition>();
        this.offloadSearches = MinetickMod.isPathSearchOffloadedFor(entityinsentient);
        double minDist = MinetickMod.getMinimumPathSearchOffloadDistance();
        this.minimumDistanceForOffloadingSquared = minDist * minDist;
    }

    private void issueSearch(Entity target, float range, boolean j, boolean k, boolean l, boolean m) {
        MinetickMod.queuePathSearch(new PathSearchJobNavigationEntity(this.a, target, range, j, k, l, m));
    }

    private void issueSearch(PositionPathSearchType type, int x, int y, int z, float range, boolean j, boolean k, boolean l, boolean m) {
        MinetickMod.queuePathSearch(new PathSearchJobNavigationPosition(type, this.a, x, y, z, range, j, k, l, m));
    }

    @Override
    public void setPathEntity(PathSearchJobNavigationEntity pathSearch) {
        SearchCacheEntry entry = pathSearch.getCacheEntryValue();
        if(entry.didSearchSucceed()) {
            this.searchCache.put(pathSearch.getCacheEntryKey(), entry);
        }
    }

    private BlockVector createBlockVectorForPosition(int x, int y, int z) {
        return new BlockVector(x, y, z);
    }

    @Override
    public void setPathEntity(PathSearchJobNavigationPosition pathSearch) {
        SearchCacheEntryPosition entry = pathSearch.getCacheEntryValue();
        if(entry.didSearchSucceed()) {
            this.positionSearchCache.put(pathSearch.getCacheEntryKey(), entry);
        }
    }

    @Override
    public PathEntity a(Entity entity) {
        if(!this.offloadSearches || this.a.e(entity) < this.minimumDistanceForOffloadingSquared) {
            return super.a(entity);
        }
        if(!this.l()) {
            return null;
        }
        SearchCacheEntry entry = null;
        UUID id = entity.getUniqueID();
        boolean entryIsValid = false;
        if(this.searchCache.containsKey(id)) {
            entry = this.searchCache.get(id);
            if(entry.isStillValid()) {
                entryIsValid = true;
            } else {
                this.searchCache.remove(id);
            }
        }

        this.issueSearch(entity, this.d(), this.j, this.k, this.l, this.m);
        if(entry != null) {
            PathEntity pE = entry.getPathEntity();
            if(!entryIsValid && pE != null && !pE.b()) {
                pE.a();
            }
            return pE;
        }
        return null;
    }

    @Override
    public PathEntity a(double d0, double d1, double d2) {
        return this.a(PositionPathSearchType.ANYOTHER, d0, d1, d2);
    }

    @Override
    public PathEntity a(PositionPathSearchType type, double d0, double d1, double d2) {
        if(!this.offloadSearches || this.a.e(d0, d1, d2) < this.minimumDistanceForOffloadingSquared) {
            return super.a(d0, d1, d2);
        }
        if(!this.l()) {
            return null;
        }
        int x = MathHelper.floor(d0);
        int y = (int) d1;
        int z = MathHelper.floor(d2);

        SearchCacheEntryPosition entry = null;
        boolean entryIsValid = false;
        if(this.positionSearchCache.containsKey(type)) {
            entry = this.positionSearchCache.get(type);
            if(entry.isStillValid()) {
                entryIsValid = true;
            } else {
                this.positionSearchCache.remove(type);
            }
        }

        this.issueSearch(type, x, y, z, this.d(), this.j, this.k, this.l, this.m);
        if(entry != null) {
            PathEntity pE = entry.getPathEntity();
            if(!entryIsValid && pE != null && !pE.b()) {
                pE.a();
            }
            return pE;
        }
        return null;
    }

    @Override
    public boolean a(PositionPathSearchType type, double d0, double d1, double d2, double d3) {
        PathEntity pathentity = this.a(type, (double) MathHelper.floor(d0), (double) ((int) d1), (double) MathHelper.floor(d2));

        return this.a(pathentity, d3);
    }

    public void cleanUpExpiredSearches() {
        this.cleanUpDelay++;
        if(this.cleanUpDelay > 100) {
            this.cleanUpDelay = 0;
            Iterator<Entry<UUID, SearchCacheEntry>> iter = this.searchCache.entrySet().iterator();
            while(iter.hasNext()) {
                Entry<UUID, SearchCacheEntry> entry = iter.next();
                if(entry.getValue().hasExpired()) {
                    iter.remove();
                }
            }
            Iterator<Entry<PositionPathSearchType, SearchCacheEntryPosition>> iter2 = this.positionSearchCache.entrySet().iterator();
            while(iter2.hasNext()) {
                Entry<PositionPathSearchType, SearchCacheEntryPosition> entry = iter2.next();
                if(entry.getValue().hasExpired()) {
                    iter2.remove();
                }
            }
        }
    }
}
