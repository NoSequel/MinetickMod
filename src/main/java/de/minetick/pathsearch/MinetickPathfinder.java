package de.minetick.pathsearch;

import net.minecraft.server.Block;
import net.minecraft.server.Blocks;
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

    /*
        This non-static variant of the original method in Pathfinder uses the assigned ChunkCache @iblockaccess 
        instead of the world object in order to get the necessary block types
    */  
    public int a_nonstatic(Entity entity, int i, int j, int k, PathPoint pathpoint, boolean flag, boolean flag1, boolean flag2) {
        boolean flag3 = false;

        for (int l = i; l < i + pathpoint.a; ++l) {
            for (int i1 = j; i1 < j + pathpoint.b; ++i1) {
                for (int j1 = k; j1 < k + pathpoint.c; ++j1) {
                    Block block = this.iblockaccess.getType(l, i1, j1);

                    if (block.getMaterial() != Material.AIR) {
                        if (block == Blocks.TRAP_DOOR) {
                            flag3 = true;
                        } else if (block != Blocks.WATER && block != Blocks.STATIONARY_WATER) {
                            if (!flag2 && block == Blocks.WOODEN_DOOR) {
                                return 0;
                            }
                        } else {
                            if (flag) {
                                return -1;
                            }

                            flag3 = true;
                        }

                        int k1 = block.b();

                        if (this.iblockaccess.getType(l, i1, j1).b() == 9) {
                            int l1 = MathHelper.floor(entity.locX);
                            int i2 = MathHelper.floor(entity.locY);
                            int j2 = MathHelper.floor(entity.locZ);

                            if (this.iblockaccess.getType(l1, i2, j2).b() != 9 && this.iblockaccess.getType(l1, i2 - 1, j2).b() != 9) {
                                return -3;
                            }
                        } else if (!block.b(entity.world, l, i1, j1) && (!flag1 || block != Blocks.WOODEN_DOOR)) {
                            if (k1 == 11 || block == Blocks.FENCE_GATE || k1 == 32) {
                                return -3;
                            }

                            if (block == Blocks.TRAP_DOOR) {
                                return -4;
                            }

                            Material material = block.getMaterial();

                            if (material != Material.LAVA) {
                                return 0;
                            }

                            if (!entity.O()) {
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
