package de.minetick.pathsearch;

import de.minetick.pathsearch.cache.SearchCacheEntryPosition;
import net.minecraft.server.ChunkCache;
import net.minecraft.server.EntityCreature;
import net.minecraft.server.EntityInsentient;
import net.minecraft.server.PathEntity;

public class PathSearchJobNavigationPosition extends PathSearchJob {

    private int x, y, z;
    private PositionPathSearchType type;

    public PathSearchJobNavigationPosition(PositionPathSearchType type, EntityInsentient entity, int x, int y, int z, float range, boolean b1, boolean b2, boolean b3, boolean b4) {
        super(entity, range, b1, b2, b3, b4);
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void run() {
        if(!this.issued) {
            this.issued = true;
            this.pathEntity = (new MinetickPathfinder(this.chunkCache, this.b1, this.b2, this.b3, this.b4)).a(entity, this.x, this.y, this.z, this.range);
            this.entity.getNavigation().setPathEntityByPosition(this.x, this.y, this.z, this.pathEntity);
            this.cleanup();
        }
    }

    public PositionPathSearchType getCacheEntryKey() {
        return this.type;
    }

    public SearchCacheEntryPosition getCacheEntryValue() {
        return new SearchCacheEntryPosition(this.entity, this.x, this.y, this.z, this.pathEntity);
    }

    @Override
    public int hashCode() {
        return this.type.hashCode() ^ (this.entity.getUniqueID().hashCode() << 4);
    }

    @Override
    public boolean equals(Object o) {
        if(o == null || !(o instanceof PathSearchJobNavigationPosition)) {
            return false;
        }
        PathSearchJobNavigationPosition other = (PathSearchJobNavigationPosition) o;
        return this.type.equals(other.type) && this.entity.getUniqueID().equals(other.entity.getUniqueID());
    }
}
