package de.minetick.pathsearch;

import java.util.HashMap;
import java.util.HashSet;
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

    private void issueSearch(int x, int y, int z, float range, boolean j, boolean k, boolean l, boolean m) {
        MinetickMod.queuePathSearch(new PathSearchJobNavigationPosition(this.a, x, y, z, range, j, k, l, m));
    }

    @Override
    public void setPathEntityByTarget(Entity target, PathEntity pathentity) {
        UUID id = target.getUniqueID();
        this.searchCache.put(id, new SearchCacheEntryEntity(this.a, target, pathentity));
    }

    private BlockVector createBlockVectorForPosition(int x, int y, int z) {
        return new BlockVector(x, y, z);
    }

    @Override
    public void setPathEntityByPosition(int x, int y, int z, PathEntity pathentity) {
        this.lastPositionSearch = new SearchCacheEntryPosition(this.a, x, y, z, pathentity);
    }

    @Override
    public PathEntity a(Entity entity) {
        if(!this.offloadSearches || this.a.e(entity) < this.minimumDistanceForOffloadingSquared) {
            return super.a(entity);
        }
        UUID id = entity.getUniqueID();
        if(!this.l()) {
            return null;
        }
        SearchCacheEntry entry = null;
        if(this.searchCache.containsKey(id)) {
            entry = this.searchCache.get(id);
            if(entry.isStillValid()) {
                return entry.getPathEntity();
            } else {
                this.searchCache.remove(id);
            }
        }

        this.issueSearch(entity, this.d(), this.j, this.k, this.l, this.m);
        if(entry != null) {
            return entry.getPathEntity();
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
        if(this.positionSearchCache.containsKey(type)) {
            entry = this.positionSearchCache.get(type);
            if(entry.isStillValid()) {
                this.issueSearch(type, x, y, z, this.d(), this.j, this.k, this.l, this.m);
                return entry.getPathEntity();
            } else {
                this.positionSearchCache.remove(type);
            }
        }

        this.issueSearch(x, y, z, this.d(), this.j, this.k, this.l, this.m);
        if(this.lastPositionSearch != null) {
            return this.lastPositionSearch.getPathEntity();
        }
        return null;
    }

    @Override
    public boolean a(PositionPathSearchType type, double d0, double d1, double d2, double d3) {
        PathEntity pathentity = this.a(type, (double) MathHelper.floor(d0), (double) ((int) d1), (double) MathHelper.floor(d2));

        return this.a(pathentity, d3);
    }
}
