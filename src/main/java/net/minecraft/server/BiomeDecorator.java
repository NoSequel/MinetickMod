package net.minecraft.server;

import java.util.Random;

// Poweruser start
import de.minetick.MinetickMod;
// Poweruser end

public class BiomeDecorator {

    protected World a;
    protected Random b;
    protected int c;
    protected int d;
    protected WorldGenerator e = new WorldGenClay(4);
    protected WorldGenerator f;
    protected WorldGenerator g;
    protected WorldGenerator h;
    protected WorldGenerator i;
    protected WorldGenerator j;
    protected WorldGenerator k;
    protected WorldGenerator l;
    protected WorldGenerator m;
    protected WorldGenerator n;
    protected WorldGenerator o;
    protected WorldGenFlowers p;
    protected WorldGenerator q;
    protected WorldGenerator r;
    protected WorldGenerator s;
    protected WorldGenerator t;
    protected WorldGenerator u;
    protected WorldGenerator v;
    protected int w;
    protected int x;
    protected int y;
    protected int z;
    protected int A;
    protected int B;
    protected int C;
    protected int D;
    protected int E;
    protected int F;
    protected int G;
    protected int H;
    public boolean I;

    public BiomeDecorator() {
        this.f = new WorldGenSand(Blocks.SAND, 7);
        this.g = new WorldGenSand(Blocks.GRAVEL, 6);
        this.h = new WorldGenMinable(Blocks.DIRT, 32);
        this.i = new WorldGenMinable(Blocks.GRAVEL, 32);
        /*
        this.j = new WorldGenMinable(Blocks.COAL_ORE, 16);
        this.k = new WorldGenMinable(Blocks.IRON_ORE, 8);
        this.l = new WorldGenMinable(Blocks.GOLD_ORE, 8);
        this.m = new WorldGenMinable(Blocks.REDSTONE_ORE, 7);
        this.n = new WorldGenMinable(Blocks.DIAMOND_ORE, 7);
        this.o = new WorldGenMinable(Blocks.LAPIS_ORE, 6);
        */
        // Poweruser start
        this.j = new WorldGenMinable(Blocks.COAL_ORE, MinetickMod.getCustomOreRates(Blocks.COAL_ORE, 16));
        this.k = new WorldGenMinable(Blocks.IRON_ORE, MinetickMod.getCustomOreRates(Blocks.IRON_ORE, 8));
        this.l = new WorldGenMinable(Blocks.GOLD_ORE, MinetickMod.getCustomOreRates(Blocks.GOLD_ORE, 8));
        this.m = new WorldGenMinable(Blocks.REDSTONE_ORE, MinetickMod.getCustomOreRates(Blocks.REDSTONE_ORE, 7));
        this.n = new WorldGenMinable(Blocks.DIAMOND_ORE, MinetickMod.getCustomOreRates(Blocks.DIAMOND_ORE, 7));
        this.o = new WorldGenMinable(Blocks.LAPIS_ORE, MinetickMod.getCustomOreRates(Blocks.LAPIS_ORE, 6));
        // Poweruser end
        this.p = new WorldGenFlowers(Blocks.YELLOW_FLOWER);
        this.q = new WorldGenFlowers(Blocks.BROWN_MUSHROOM);
        this.r = new WorldGenFlowers(Blocks.RED_MUSHROOM);
        this.s = new WorldGenHugeMushroom();
        this.t = new WorldGenReed();
        this.u = new WorldGenCactus();
        this.v = new WorldGenWaterLily();
        this.y = 2;
        this.z = 1;
        this.E = 1;
        this.F = 3;
        this.G = 1;
        this.I = true;
    }

    // Poweruser start
    private BiomeDecorator(BiomeDecorator decorator) {
        this.f = decorator.f;
        this.g = decorator.g;
        this.h = decorator.h;
        this.i = decorator.i;
        this.j = decorator.j;
        this.k = decorator.k;
        this.l = decorator.l;
        this.m = decorator.m;
        this.n = decorator.n;
        this.o = decorator.o;
        this.p = decorator.p;
        this.q = decorator.q;
        this.r = decorator.r;
        this.s = decorator.s;
        this.t = decorator.t;
        this.u = decorator.u;
        this.v = decorator.v;
        this.w = decorator.w;
        this.x = decorator.x;
        this.y = decorator.y;
        this.z = decorator.z;
        this.A = decorator.A;
        this.B = decorator.B;
        this.C = decorator.C;
        this.D = decorator.D;
        this.E = decorator.E;
        this.F = decorator.F;
        this.G = decorator.G;
        this.H = decorator.H;
        this.I = decorator.I;
    }
    // Poweruser end

    public void a(World world, Random random, BiomeBase biomebase, int i, int j) {
        if (this.a != null) {
            //throw new RuntimeException("Already decorating!!");
            // Poweruser start
            BiomeDecorator decorator = new BiomeDecorator(this);
            decorator.a = world;
            decorator.b = random;
            decorator.c = i;
            decorator.d = j;
            decorator.a(biomebase);
            decorator.a = null;
            decorator.b = null;
            // Poweruser end
        } else {
            this.a = world;
            this.b = random;
            this.c = i;
            this.d = j;
            this.a(biomebase);
            this.a = null;
            this.b = null;
        }
    }

    protected void a(BiomeBase biomebase) {
        this.a();

        int i;
        int j;
        int k;

        for (i = 0; i < this.F; ++i) {
            j = this.c + this.b.nextInt(16) + 8;
            k = this.d + this.b.nextInt(16) + 8;
            this.f.generate(this.a, this.b, j, this.a.i(j, k), k);
        }

        for (i = 0; i < this.G; ++i) {
            j = this.c + this.b.nextInt(16) + 8;
            k = this.d + this.b.nextInt(16) + 8;
            this.e.generate(this.a, this.b, j, this.a.i(j, k), k);
        }

        for (i = 0; i < this.E; ++i) {
            j = this.c + this.b.nextInt(16) + 8;
            k = this.d + this.b.nextInt(16) + 8;
            this.g.generate(this.a, this.b, j, this.a.i(j, k), k);
        }

        i = this.x;
        if (this.b.nextInt(10) == 0) {
            ++i;
        }

        int l;
        int i1;
        int height; // Poweruser

        for (j = 0; j < i; ++j) {
            k = this.c + this.b.nextInt(16) + 8;
            l = this.d + this.b.nextInt(16) + 8;
            i1 = this.a.getHighestBlockYAt(k, l);
            WorldGenTreeAbstract worldgentreeabstract = biomebase.a(this.b);

            worldgentreeabstract.a(1.0D, 1.0D, 1.0D);
            if (worldgentreeabstract.generate(this.a, this.b, k, i1, l)) {
                worldgentreeabstract.b(this.a, this.b, k, i1, l);
            }
        }

        for (j = 0; j < this.H; ++j) {
            k = this.c + this.b.nextInt(16) + 8;
            l = this.d + this.b.nextInt(16) + 8;
            this.s.generate(this.a, this.b, k, this.a.getHighestBlockYAt(k, l), l);
        }

        for (j = 0; j < this.y; ++j) {
            k = this.c + this.b.nextInt(16) + 8;
            l = this.d + this.b.nextInt(16) + 8;
            //i1 = this.b.nextInt(this.a.getHighestBlockYAt(k, l) + 32);
            // Poweruser start
            height = this.a.getHighestBlockYAt(k, l) + 32;
            if(height > 0) {
                i1 = this.b.nextInt(height);
                String s = biomebase.a(this.b, k, i1, l);
                BlockFlowers blockflowers = BlockFlowers.e(s);

                if (blockflowers.getMaterial() != Material.AIR) {
                    this.p.a(blockflowers, BlockFlowers.f(s));
                    this.p.generate(this.a, this.b, k, i1, l);
                }
            }
            // Poweruser end
        }

        for (j = 0; j < this.z; ++j) {
            k = this.c + this.b.nextInt(16) + 8;
            l = this.d + this.b.nextInt(16) + 8;
            //i1 = this.b.nextInt(this.a.getHighestBlockYAt(k, l) * 2);
            // Poweruser start
            height = this.a.getHighestBlockYAt(k, l) * 2;
            if(height > 0) {
                i1 = this.b.nextInt(height);
                WorldGenerator worldgenerator = biomebase.b(this.b);

                worldgenerator.generate(this.a, this.b, k, i1, l);
            }
            // Poweruser end
        }

        for (j = 0; j < this.A; ++j) {
            k = this.c + this.b.nextInt(16) + 8;
            l = this.d + this.b.nextInt(16) + 8;
            //i1 = this.b.nextInt(this.a.getHighestBlockYAt(k, l) * 2);
            // Poweruser start
            height = this.a.getHighestBlockYAt(k, l) * 2;
            if(height > 0) {
                i1 = this.b.nextInt(height);
                (new WorldGenDeadBush(Blocks.DEAD_BUSH)).generate(this.a, this.b, k, i1, l);
            }
            // Poweruser end
        }

        for (j = 0; j < this.w; ++j) {
            k = this.c + this.b.nextInt(16) + 8;
            l = this.d + this.b.nextInt(16) + 8;

            /*
            for (i1 = this.b.nextInt(this.a.getHighestBlockYAt(k, l) * 2); i1 > 0 && this.a.isEmpty(k, i1 - 1, l); --i1) {
                ;
            }
            */
            // Poweruser start
            height = this.a.getHighestBlockYAt(k, l) * 2;
            if(height > 0) {
                i1 = this.b.nextInt(height);
                while(i1 > 0 && this.a.isEmpty(k, i1 - 1, l)) {
                    --i1;
                }
                this.v.generate(this.a, this.b, k, i1, l);
            }
            // Poweruser end
        }

        for (j = 0; j < this.B; ++j) {
            if (this.b.nextInt(4) == 0) {
                k = this.c + this.b.nextInt(16) + 8;
                l = this.d + this.b.nextInt(16) + 8;
                i1 = this.a.getHighestBlockYAt(k, l);
                this.q.generate(this.a, this.b, k, i1, l);
            }

            if (this.b.nextInt(8) == 0) {
                k = this.c + this.b.nextInt(16) + 8;
                l = this.d + this.b.nextInt(16) + 8;
                //i1 = this.b.nextInt(this.a.getHighestBlockYAt(k, l) * 2);
                // Poweruser start
                height = this.a.getHighestBlockYAt(k, l) * 2;
                if(height > 0) {
                    i1 = this.b.nextInt(height);
                    this.r.generate(this.a, this.b, k, i1, l);
                }
                // Poweruser end
            }
        }

        if (this.b.nextInt(4) == 0) {
            j = this.c + this.b.nextInt(16) + 8;
            k = this.d + this.b.nextInt(16) + 8;
            //l = this.b.nextInt(this.a.getHighestBlockYAt(j, k) * 2);
            // Poweruser start
            height = this.a.getHighestBlockYAt(j, k) * 2;
            if(height > 0) {
                l = this.b.nextInt(height);
                this.q.generate(this.a, this.b, j, l, k);
            }
            // Poweruser end
        }

        if (this.b.nextInt(8) == 0) {
            j = this.c + this.b.nextInt(16) + 8;
            k = this.d + this.b.nextInt(16) + 8;
            //l = this.b.nextInt(this.a.getHighestBlockYAt(j, k) * 2);
            // Poweruser start
            height = this.a.getHighestBlockYAt(j, k) * 2;
            if(height > 0) {
                l = this.b.nextInt(height);
                this.r.generate(this.a, this.b, j, l, k);
            }
            // Poweruser end
        }

        for (j = 0; j < this.C; ++j) {
            k = this.c + this.b.nextInt(16) + 8;
            l = this.d + this.b.nextInt(16) + 8;
            //i1 = this.b.nextInt(this.a.getHighestBlockYAt(k, l) * 2);
            // Poweruser start
            height = this.a.getHighestBlockYAt(k, l) * 2;
            if(height > 0) {
                i1 = this.b.nextInt(height);
                this.t.generate(this.a, this.b, k, i1, l);
            }
            // Poweruser end
        }

        for (j = 0; j < 10; ++j) {
            k = this.c + this.b.nextInt(16) + 8;
            l = this.d + this.b.nextInt(16) + 8;
            //i1 = this.b.nextInt(this.a.getHighestBlockYAt(k, l) * 2);
            // Poweruser start
            height = this.a.getHighestBlockYAt(k, l) * 2;
            if(height > 0) {
                i1 = this.b.nextInt(height);
                this.t.generate(this.a, this.b, k, i1, l);
            }
            // Poweruser end
        }

        if (this.b.nextInt(32) == 0) {
            j = this.c + this.b.nextInt(16) + 8;
            k = this.d + this.b.nextInt(16) + 8;
            //l = this.b.nextInt(this.a.getHighestBlockYAt(j, k) * 2);
            // Poweruser start
            height = this.a.getHighestBlockYAt(j, k) * 2;
            if(height > 0) {
                l = this.b.nextInt(height);
                (new WorldGenPumpkin()).generate(this.a, this.b, j, l, k);
            }
            // Poweruser end
        }

        for (j = 0; j < this.D; ++j) {
            k = this.c + this.b.nextInt(16) + 8;
            l = this.d + this.b.nextInt(16) + 8;
            //i1 = this.b.nextInt(this.a.getHighestBlockYAt(k, l) * 2);
            // Poweruser start
            height = this.a.getHighestBlockYAt(k, l) * 2;
            if(height > 0) {
                i1 = this.b.nextInt(height);
                this.u.generate(this.a, this.b, k, i1, l);
            }
            // Poweruser end
        }

        if (this.I) {
            for (j = 0; j < 50; ++j) {
                k = this.c + this.b.nextInt(16) + 8;
                l = this.b.nextInt(this.b.nextInt(248) + 8);
                i1 = this.d + this.b.nextInt(16) + 8;
                (new WorldGenLiquids(Blocks.WATER)).generate(this.a, this.b, k, l, i1);
            }

            for (j = 0; j < 20; ++j) {
                k = this.c + this.b.nextInt(16) + 8;
                l = this.b.nextInt(this.b.nextInt(this.b.nextInt(240) + 8) + 8);
                i1 = this.d + this.b.nextInt(16) + 8;
                (new WorldGenLiquids(Blocks.LAVA)).generate(this.a, this.b, k, l, i1);
            }
        }
    }

    protected void a(int i, WorldGenerator worldgenerator, int j, int k) {
        for (int l = 0; l < i; ++l) {
            int i1 = this.c + this.b.nextInt(16);
            int j1 = this.b.nextInt(k - j) + j;
            int k1 = this.d + this.b.nextInt(16);

            worldgenerator.generate(this.a, this.b, i1, j1, k1);
        }
    }

    protected void b(int i, WorldGenerator worldgenerator, int j, int k) {
        for (int l = 0; l < i; ++l) {
            int i1 = this.c + this.b.nextInt(16);
            int j1 = this.b.nextInt(k) + this.b.nextInt(k) + (j - k);
            int k1 = this.d + this.b.nextInt(16);

            worldgenerator.generate(this.a, this.b, i1, j1, k1);
        }
    }

    protected void a() {
        this.a(20, this.h, 0, 256);
        this.a(10, this.i, 0, 256);
        this.a(20, this.j, 0, 128);
        this.a(20, this.k, 0, 64);
        this.a(2, this.l, 0, 32);
        this.a(8, this.m, 0, 16);
        this.a(1, this.n, 0, 16);
        this.b(1, this.o, 16, 16);
    }
}
