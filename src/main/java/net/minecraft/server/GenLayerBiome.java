package net.minecraft.server;

public class GenLayerBiome extends GenLayer {

    private BiomeBase[] c;
    private BiomeBase[] d;
    private BiomeBase[] e;
    private BiomeBase[] f;

    public GenLayerBiome(long i, GenLayer genlayer, WorldType worldtype) {
        super(i);
        /*
        this.c = new BiomeBase[] { BiomeBase.DESERT, BiomeBase.DESERT, BiomeBase.DESERT, BiomeBase.SAVANNA, BiomeBase.SAVANNA, BiomeBase.PLAINS};
        this.d = new BiomeBase[] { BiomeBase.FOREST, BiomeBase.ROOFED_FOREST, BiomeBase.EXTREME_HILLS, BiomeBase.PLAINS, BiomeBase.BIRCH_FOREST, BiomeBase.SWAMPLAND};
        this.e = new BiomeBase[] { BiomeBase.FOREST, BiomeBase.EXTREME_HILLS, BiomeBase.TAIGA, BiomeBase.PLAINS};
        this.f = new BiomeBase[] { BiomeBase.ICE_PLAINS, BiomeBase.ICE_PLAINS, BiomeBase.ICE_PLAINS, BiomeBase.COLD_TAIGA};
        this.a = genlayer;
        if (worldtype == WorldType.NORMAL_1_1) {
            this.c = new BiomeBase[] { BiomeBase.DESERT, BiomeBase.FOREST, BiomeBase.EXTREME_HILLS, BiomeBase.SWAMPLAND, BiomeBase.PLAINS, BiomeBase.TAIGA};
        }
        */
        // Poweruser start
        BiomeBaseObject biomeBaseObj = genlayer.biomeBaseObj;
        this.c = new BiomeBase[] { biomeBaseObj.DESERT, biomeBaseObj.DESERT, biomeBaseObj.DESERT, biomeBaseObj.SAVANNA, biomeBaseObj.SAVANNA, biomeBaseObj.PLAINS};
        this.d = new BiomeBase[] { biomeBaseObj.FOREST, biomeBaseObj.ROOFED_FOREST, biomeBaseObj.EXTREME_HILLS, biomeBaseObj.PLAINS, biomeBaseObj.BIRCH_FOREST, biomeBaseObj.SWAMPLAND};
        this.e = new BiomeBase[] { biomeBaseObj.FOREST, biomeBaseObj.EXTREME_HILLS, biomeBaseObj.TAIGA, biomeBaseObj.PLAINS};
        this.f = new BiomeBase[] { biomeBaseObj.ICE_PLAINS, biomeBaseObj.ICE_PLAINS, biomeBaseObj.ICE_PLAINS, biomeBaseObj.COLD_TAIGA};
        this.a = genlayer;
        if (worldtype == WorldType.NORMAL_1_1) {
            this.c = new BiomeBase[] { biomeBaseObj.DESERT, biomeBaseObj.FOREST, biomeBaseObj.EXTREME_HILLS, biomeBaseObj.SWAMPLAND, biomeBaseObj.PLAINS, biomeBaseObj.TAIGA};
        }
        // Poweruser end
    }

    public int[] a(int i, int j, int k, int l) {
        int[] aint = this.a.a(i, j, k, l);
        int[] aint1 = this.intCache.a(k * l); // Poweruser

        for (int i1 = 0; i1 < l; ++i1) {
            for (int j1 = 0; j1 < k; ++j1) {
                this.a((long) (j1 + i), (long) (i1 + j));
                int k1 = aint[j1 + i1 * k];
                int l1 = (k1 & 3840) >> 8;

                k1 &= -3841;
                if (b(k1)) {
                    aint1[j1 + i1 * k] = k1;
                //} else if (k1 == BiomeBase.MUSHROOM_ISLAND.id) {
                } else if (k1 == BiomeIDEnum.MUSHROOM_ISLAND.id) { // Poweruser
                    aint1[j1 + i1 * k] = k1;
                } else if (k1 == 1) {
                    if (l1 > 0) {
                        if (this.a(3) == 0) {
                            //aint1[j1 + i1 * k] = BiomeBase.MESA_PLATEAU.id;
                            aint1[j1 + i1 * k] = BiomeIDEnum.MESA_PLATEAU.id; // Poweruser
                        } else {
                            //aint1[j1 + i1 * k] = BiomeBase.MESA_PLATEAU_F.id;
                            aint1[j1 + i1 * k] = BiomeIDEnum.MESA_PLATEAU_F.id; // Poweruser
                        }
                    } else {
                        aint1[j1 + i1 * k] = this.c[this.a(this.c.length)].id;
                    }
                } else if (k1 == 2) {
                    if (l1 > 0) {
                        //aint1[j1 + i1 * k] = BiomeBase.JUNGLE.id;
                        aint1[j1 + i1 * k] = BiomeIDEnum.JUNGLE.id; // Poweruser
                    } else {
                        aint1[j1 + i1 * k] = this.d[this.a(this.d.length)].id;
                    }
                } else if (k1 == 3) {
                    if (l1 > 0) {
                        //aint1[j1 + i1 * k] = BiomeBase.MEGA_TAIGA.id;
                        aint1[j1 + i1 * k] = BiomeIDEnum.MEGA_TAIGA.id; // Poweruser
                    } else {
                        aint1[j1 + i1 * k] = this.e[this.a(this.e.length)].id;
                    }
                } else if (k1 == 4) {
                    aint1[j1 + i1 * k] = this.f[this.a(this.f.length)].id;
                } else {
                    //aint1[j1 + i1 * k] = BiomeBase.MUSHROOM_ISLAND.id;
                    aint1[j1 + i1 * k] = BiomeIDEnum.MUSHROOM_ISLAND.id;
                }
            }
        }

        return aint1;
    }
}
