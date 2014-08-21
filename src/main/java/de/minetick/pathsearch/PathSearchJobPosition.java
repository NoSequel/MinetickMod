package de.minetick.pathsearch;

import net.minecraft.server.EntityCreature;
import net.minecraft.server.PathEntity;
import net.minecraft.server.Pathfinder;

public class PathSearchJobPosition extends PathSearchJob {

    private int x, y, z;

    public PathSearchJobPosition(EntityCreature entity, int x, int y, int z, float range, boolean b1, boolean b2, boolean b3, boolean b4) {
        super(entity, range, b1, b2, b3, b4);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void run() {
        if(!this.issued) {
            this.issued = true;
            PathEntity pathentity = (new MinetickPathfinder(this.chunkCache, this.b1, this.b2, this.b3, this.b4)).a(entity, this.x, this.y, this.z, this.range);
            this.entity.setPathEntityByPosition(pathentity);
            this.cleanup();
        }
    }
}
