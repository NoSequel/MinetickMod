package org.bukkit.craftbukkit.chunkio;

import net.minecraft.server.Chunk;
import net.minecraft.server.ChunkProviderServer;
import net.minecraft.server.ChunkRegionLoader;
import net.minecraft.server.World;
import org.bukkit.craftbukkit.util.AsynchronousExecutor;

public class ChunkIOExecutor {
    //static final int BASE_THREADS = 1;
    static final int BASE_THREADS = 2; // Poweruser
    static final int PLAYERS_PER_THREAD = 50;
    static final Object lockObject = new Object(); // Poweruser

    private static final AsynchronousExecutor<QueuedChunk, Chunk, Runnable, RuntimeException> instance = new AsynchronousExecutor<QueuedChunk, Chunk, Runnable, RuntimeException>(new ChunkIOProvider(), BASE_THREADS);

    public static Chunk syncChunkLoad(World world, ChunkRegionLoader loader, ChunkProviderServer provider, int x, int z) {
        synchronized(lockObject) { // Poweruser - MinetickMod got several world threads that call these methods
            return instance.getSkipQueue(new QueuedChunk(x, z, loader, world, provider));
        }
    }

    public static void queueChunkLoad(World world, ChunkRegionLoader loader, ChunkProviderServer provider, int x, int z, Runnable runnable) {
        synchronized(lockObject) { // Poweruser - MinetickMod got several world threads that call these methods
            instance.add(new QueuedChunk(x, z, loader, world, provider), runnable);
        }
    }

    public static void adjustPoolSize(int players) {
        int size = Math.max(BASE_THREADS, (int) Math.ceil(players / PLAYERS_PER_THREAD));
        instance.setActiveThreads(size);
    }

    public static void tick() {
        instance.finishActive();
    }
}
