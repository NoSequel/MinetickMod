package de.minetick.pathsearch;

import net.minecraft.server.Block;
import net.minecraft.server.Entity;
import net.minecraft.server.IBlockAccess;
import net.minecraft.server.Material;
import net.minecraft.server.MathHelper;
import net.minecraft.server.PathPoint;
import net.minecraft.server.Pathfinder;

public class MinetickPathfinder extends Pathfinder {

    private IBlockAccess iblockaccess;

    public MinetickPathfinder(IBlockAccess iblockaccess, boolean flag, boolean flag1, boolean flag2, boolean flag3) {
        super(iblockaccess, flag, flag1, flag2, flag3);
        this.iblockaccess = iblockaccess;
    }

    @Override
    public int a(Entity entity, int i, int j, int k, PathPoint pathpoint) {
        return this.a_nonstatic(entity, i, j, k, pathpoint, this.g, this.f, this.e);
    }

    // Copied from World
    private int e(int i, int j, int k) {
        int l = this.iblockaccess.getTypeId(i, j, k);

        return Block.byId[l] != null ? Block.byId[l].d() : -1;
    }
    
    /*
        This non-static variant of the original method in Pathfinder uses the assigned ChunkCache @iblockaccess 
        instead of the world object in order to get the necessary block types
    */  
    public int a_nonstatic(Entity entity, int i, int j, int k, PathPoint pathpoint, boolean flag, boolean flag1, boolean flag2) {
        boolean flag3 = false;

        for (int l = i; l < i + pathpoint.a; ++l) {
            for (int i1 = j; i1 < j + pathpoint.b; ++i1) {
                for (int j1 = k; j1 < k + pathpoint.c; ++j1) {
                    int k1 = this.iblockaccess.getTypeId(l, i1, j1);

                    if (k1 > 0) {
                        if (k1 == Block.TRAP_DOOR.id) {
                            flag3 = true;
                        } else if (k1 != Block.WATER.id && k1 != Block.STATIONARY_WATER.id) {
                            if (!flag2 && k1 == Block.WOODEN_DOOR.id) {
                                return 0;
                            }
                        } else {
                            if (flag) {
                                return -1;
                            }

                            flag3 = true;
                        }

                        Block block = Block.byId[k1];
                        int l1 = block.d();

                        if (this.e(l, i1, j1) == 9) {
                            int i2 = MathHelper.floor(entity.locX);
                            int j2 = MathHelper.floor(entity.locY);
                            int k2 = MathHelper.floor(entity.locZ);

                            if (this.e(i2, j2, k2) != 9 && this.e(i2, j2 - 1, k2) != 9) {
                                return -3;
                            }
                        } else if (!block.b(this.iblockaccess, l, i1, j1) && (!flag1 || k1 != Block.WOODEN_DOOR.id)) {
                            if (l1 == 11 || k1 == Block.FENCE_GATE.id || l1 == 32) {
                                return -3;
                            }

                            if (k1 == Block.TRAP_DOOR.id) {
                                return -4;
                            }

                            Material material = block.material;

                            if (material != Material.LAVA) {
                                return 0;
                            }

                            if (!entity.J()) {
                                return -2;
                            }
                        }
                    }
                }
            }
        }

        return flag3 ? 2 : 1;
    }
}
