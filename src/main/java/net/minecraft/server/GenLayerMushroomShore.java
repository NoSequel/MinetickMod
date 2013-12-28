package net.minecraft.server;

public class GenLayerMushroomShore extends GenLayer {

    public GenLayerMushroomShore(long i, GenLayer genlayer) {
        super(i);
        this.a = genlayer;
    }

    public int[] a(int i, int j, int k, int l) {
        int[] aint = this.a.a(i - 1, j - 1, k + 2, l + 2);
        int[] aint1 = this.intCache.a(k * l); // Poweruser

        for (int i1 = 0; i1 < l; ++i1) {
            for (int j1 = 0; j1 < k; ++j1) {
                this.a((long) (j1 + i), (long) (i1 + j));
                int k1 = aint[j1 + 1 + (i1 + 1) * (k + 2)];
                //BiomeBase biomebase = BiomeBase.getBiome(k1);
                BiomeBase biomebase = this.biomeBaseObj.getBiome(k1); // Poweruser
                int l1;
                int i2;
                int j2;
                int k2;

                //if (k1 == BiomeBase.MUSHROOM_ISLAND.id) {
                if (k1 == BiomeIDEnum.MUSHROOM_ISLAND.id) { // Poweruser
                    l1 = aint[j1 + 1 + (i1 + 1 - 1) * (k + 2)];
                    i2 = aint[j1 + 1 + 1 + (i1 + 1) * (k + 2)];
                    j2 = aint[j1 + 1 - 1 + (i1 + 1) * (k + 2)];
                    k2 = aint[j1 + 1 + (i1 + 1 + 1) * (k + 2)];
                    //if (l1 != BiomeBase.OCEAN.id && i2 != BiomeBase.OCEAN.id && j2 != BiomeBase.OCEAN.id && k2 != BiomeBase.OCEAN.id) {
                    if (l1 != BiomeIDEnum.OCEAN.id && i2 != BiomeIDEnum.OCEAN.id && j2 != BiomeIDEnum.OCEAN.id && k2 != BiomeIDEnum.OCEAN.id) { // Poweruser
                        aint1[j1 + i1 * k] = k1;
                    } else {
                        //aint1[j1 + i1 * k] = BiomeBase.MUSHROOM_SHORE.id;
                        aint1[j1 + i1 * k] = BiomeIDEnum.MUSHROOM_SHORE.id; // Poweruser
                    }
                } else if (biomebase != null && biomebase.l() == BiomeJungle.class) {
                    l1 = aint[j1 + 1 + (i1 + 1 - 1) * (k + 2)];
                    i2 = aint[j1 + 1 + 1 + (i1 + 1) * (k + 2)];
                    j2 = aint[j1 + 1 - 1 + (i1 + 1) * (k + 2)];
                    k2 = aint[j1 + 1 + (i1 + 1 + 1) * (k + 2)];
                    if (this.c(l1) && this.c(i2) && this.c(j2) && this.c(k2)) {
                        if (!b(l1) && !b(i2) && !b(j2) && !b(k2)) {
                            aint1[j1 + i1 * k] = k1;
                        } else {
                            //aint1[j1 + i1 * k] = BiomeBase.BEACH.id;
                            aint1[j1 + i1 * k] = BiomeIDEnum.BEACH.id; // Poweruser
                        }
                    } else {
                        //aint1[j1 + i1 * k] = BiomeBase.JUNGLE_EDGE.id;
                        aint1[j1 + i1 * k] = BiomeIDEnum.JUNGLE_EDGE.id; // Poweruser
                    }
                //} else if (k1 != BiomeBase.EXTREME_HILLS.id && k1 != BiomeBase.EXTREME_HILLS_PLUS.id && k1 != BiomeBase.SMALL_MOUNTAINS.id) {
                } else if (k1 != BiomeIDEnum.EXTREME_HILLS.id && k1 != BiomeIDEnum.EXTREME_HILLS_PLUS.id && k1 != BiomeIDEnum.SMALL_MOUNTAINS.id) { // Poweruser
                    if (biomebase != null && biomebase.j()) {
                        //this.a(aint, aint1, j1, i1, k, k1, BiomeBase.COLD_BEACH.id);
                        this.a(aint, aint1, j1, i1, k, k1, BiomeIDEnum.COLD_BEACH.id); // Poweruser
                    //} else if (k1 != BiomeBase.MESA.id && k1 != BiomeBase.MESA_PLATEAU_F.id) {
                    } else if (k1 != BiomeIDEnum.MESA.id && k1 != BiomeIDEnum.MESA_PLATEAU_F.id) { // Poweruser
                        //if (k1 != BiomeBase.OCEAN.id && k1 != BiomeBase.DEEP_OCEAN.id && k1 != BiomeBase.RIVER.id && k1 != BiomeBase.SWAMPLAND.id) {
                        if (k1 != BiomeIDEnum.OCEAN.id && k1 != BiomeIDEnum.DEEP_OCEAN.id && k1 != BiomeIDEnum.RIVER.id && k1 != BiomeIDEnum.SWAMPLAND.id) { // Poweruser
                            l1 = aint[j1 + 1 + (i1 + 1 - 1) * (k + 2)];
                            i2 = aint[j1 + 1 + 1 + (i1 + 1) * (k + 2)];
                            j2 = aint[j1 + 1 - 1 + (i1 + 1) * (k + 2)];
                            k2 = aint[j1 + 1 + (i1 + 1 + 1) * (k + 2)];
                            if (!b(l1) && !b(i2) && !b(j2) && !b(k2)) {
                                aint1[j1 + i1 * k] = k1;
                            } else {
                                //aint1[j1 + i1 * k] = BiomeBase.BEACH.id;
                                aint1[j1 + i1 * k] = BiomeIDEnum.BEACH.id; // Poweruser
                            }
                        } else {
                            aint1[j1 + i1 * k] = k1;
                        }
                    } else {
                        l1 = aint[j1 + 1 + (i1 + 1 - 1) * (k + 2)];
                        i2 = aint[j1 + 1 + 1 + (i1 + 1) * (k + 2)];
                        j2 = aint[j1 + 1 - 1 + (i1 + 1) * (k + 2)];
                        k2 = aint[j1 + 1 + (i1 + 1 + 1) * (k + 2)];
                        if (!b(l1) && !b(i2) && !b(j2) && !b(k2)) {
                            if (this.d(l1) && this.d(i2) && this.d(j2) && this.d(k2)) {
                                aint1[j1 + i1 * k] = k1;
                            } else {
                                //aint1[j1 + i1 * k] = BiomeBase.DESERT.id;
                                aint1[j1 + i1 * k] = BiomeIDEnum.DESERT.id; // Poweruser
                            }
                        } else {
                            aint1[j1 + i1 * k] = k1;
                        }
                    }
                } else {
                    //this.a(aint, aint1, j1, i1, k, k1, BiomeBase.STONE_BEACH.id);
                    this.a(aint, aint1, j1, i1, k, k1, BiomeIDEnum.STONE_BEACH.id); // Poweruser
                }
            }
        }

        return aint1;
    }

    private void a(int[] aint, int[] aint1, int i, int j, int k, int l, int i1) {
        if (b(l)) {
            aint1[i + j * k] = l;
        } else {
            int j1 = aint[i + 1 + (j + 1 - 1) * (k + 2)];
            int k1 = aint[i + 1 + 1 + (j + 1) * (k + 2)];
            int l1 = aint[i + 1 - 1 + (j + 1) * (k + 2)];
            int i2 = aint[i + 1 + (j + 1 + 1) * (k + 2)];

            if (!b(j1) && !b(k1) && !b(l1) && !b(i2)) {
                aint1[i + j * k] = l;
            } else {
                aint1[i + j * k] = i1;
            }
        }
    }

    private boolean c(int i) {
        //return BiomeBase.getBiome(i) != null && BiomeBase.getBiome(i).l() == BiomeJungle.class ? true : i == BiomeBase.JUNGLE_EDGE.id || i == BiomeBase.JUNGLE.id || i == BiomeBase.JUNGLE_HILLS.id || i == BiomeBase.FOREST.id || i == BiomeBase.TAIGA.id || b(i);
        return this.biomeBaseObj.getBiome(i) != null && this.biomeBaseObj.getBiome(i).l() == BiomeJungle.class ? true : i == BiomeIDEnum.JUNGLE_EDGE.id || i == BiomeIDEnum.JUNGLE.id || i == BiomeIDEnum.JUNGLE_HILLS.id || i == BiomeIDEnum.FOREST.id || i == BiomeIDEnum.TAIGA.id || b(i); // Poweruser
    }

    private boolean d(int i) {
        //return BiomeBase.getBiome(i) != null && BiomeBase.getBiome(i) instanceof BiomeMesa;
        return this.biomeBaseObj.getBiome(i) != null && this.biomeBaseObj.getBiome(i) instanceof BiomeMesa; // Poweruser
    }
}
