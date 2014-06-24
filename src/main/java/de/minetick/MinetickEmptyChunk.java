package de.minetick;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

import net.minecraft.server.Chunk;
import net.minecraft.server.Blocks;
import net.minecraft.server.Block;
import net.minecraft.server.ChunkSection;
import net.minecraft.server.IChunkProvider;
import net.minecraft.server.World;
import net.minecraft.server.Entity;
import net.minecraft.server.TileEntity;
import net.minecraft.server.EnumSkyBlock;
import net.minecraft.server.AxisAlignedBB;
import net.minecraft.server.IEntitySelector;

public class MinetickEmptyChunk extends Chunk {

    public MinetickEmptyChunk(World world, int i, int j) {
        super(world, i, j);
        this.done = true;
        this.lit = true;
    }

    public boolean a(int i, int j) {
        return i == this.locX && j == this.locZ;
    }

    public int b(int i, int j) {
        return 1;
    }

    public void initLighting() {}

    public Block getType(int i, int j, int k) {
        return Blocks.AIR;
    }

    public int b(int i, int j, int k) {
        return 255;
    }

    public boolean a(int i, int j, int k, Block block, int l) {
        return true;
    }

    public int getData(int i, int j, int k) {
        return 0;
    }

    public boolean a(int i, int j, int k, int l) {
        return false;
    }

    public int getBrightness(EnumSkyBlock enumskyblock, int i, int j, int k) {
        return 0;
    }

    public void a(EnumSkyBlock enumskyblock, int i, int j, int k, int l) {}

    public int b(int i, int j, int k, int l) {
        return 0;
    }

    public void a(Entity entity) {}

    public void b(Entity entity) {}

    public void a(Entity entity, int i) {}

    public boolean d(int i, int j, int k) {
        return false;
    }

    public TileEntity e(int i, int j, int k) {
        return null;
    }

    public void a(TileEntity tileentity) {}

    public void a(int i, int j, int k, TileEntity tileentity) {}

    public void f(int i, int j, int k) {}

    public void addEntities() {}

    public void removeEntities() {}

    public void e() {}

    public void a(Entity entity, AxisAlignedBB axisalignedbb, List list, IEntitySelector ientityselector) {}

    public void a(Class oclass, AxisAlignedBB axisalignedbb, List list, IEntitySelector ientityselector) {}

    public boolean a(boolean flag) {
        return false;
    }

    public Random a(long i) {
        return new Random(this.world.getSeed() + (long) (this.locX * this.locX * 4987142) + (long) (this.locX * 5947611) + (long) (this.locZ * this.locZ) * 4392871L + (long) (this.locZ * 389711) ^ i);
    }

    public boolean isEmpty() {
        return true;
    }

    public boolean c(int i, int j) {
        return true;
    }

    public void a(IChunkProvider ichunkprovider, IChunkProvider ichunkprovider1, int i, int j) {}
}
