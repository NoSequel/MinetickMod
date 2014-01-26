package net.minecraft.server;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import de.minetick.MinetickMod;
import de.minetick.packetbuilder.PacketBuilderBuffer;

public class PacketPlayOutMapChunkBulk extends Packet {

    private int[] a;
    private int[] b;
    private int[] c;
    private int[] d;
    private byte[] buffer;
    private byte[][] inflatedBuffers;
    private int size;
    private boolean h;
    //private byte[] buildBuffer = new byte[0]; // CraftBukkit - remove static
    // Poweruser start
    private byte[] buildBuffer;
    private PacketBuilderBuffer pbb;
    private AtomicInteger pendingUses;
    public static int targetCompressionLevel = MinetickMod.defaultPacketCompression;

    public static void changeCompressionLevel(int level) {
        if(level < Deflater.BEST_SPEED || level > Deflater.BEST_COMPRESSION) {
            targetCompressionLevel = Deflater.DEFAULT_COMPRESSION;
        } else {
            targetCompressionLevel = level;
        }
    }

    public void setPendingUses(int uses) {
        this.pendingUses = new AtomicInteger(uses);
    }

    public void discard() {
        if(this.pbb != null) {
            if(this.buffer != null) {
                this.pbb.offerSendBuffer(this.buffer);
            }
            this.pbb = null;
        }
    }
    // Poweruser end

    /*
    // CraftBukkit start
    static final ThreadLocal<Deflater> localDeflater = new ThreadLocal<Deflater>() {
        @Override
        protected Deflater initialValue() {
            // Don't use higher compression level, slows things down too much
            /*
             * Default was 6, but as compression is run in seperate threads now
             * a higher compression can be afforded
             *//*
            return new Deflater(7);
        }
    };
    // CraftBukkit end
    */

    public PacketPlayOutMapChunkBulk() {}

    //public PacketPlayOutMapChunkBulk(List list) {
    // Poweruser start
    public PacketPlayOutMapChunkBulk(PacketBuilderBuffer pbb, List list) {
        this.pbb = pbb;
    // Poweruser end
        int i = list.size();

        this.a = new int[i];
        this.b = new int[i];
        this.c = new int[i];
        this.d = new int[i];
        this.inflatedBuffers = new byte[i][];
        this.h = !list.isEmpty() && !((Chunk) list.get(0)).world.worldProvider.g;
        int j = 0;
        for (int k = 0; k < i; ++k) {
            Chunk chunk = (Chunk) list.get(k);
            ChunkMap chunkmap = PacketPlayOutMapChunk.a(chunk, true, '\uffff');

            /* Poweruser - instead of allocating a larger buffer and copying over the processed chunks each time,
             * it is better to collect the seperate buildbuffers first, to do this only once
            if (buildBuffer.length < j + chunkmap.a.length) {
                byte[] abyte = new byte[j + chunkmap.a.length];

                System.arraycopy(buildBuffer, 0, abyte, 0, buildBuffer.length);
                buildBuffer = abyte;
            }

            System.arraycopy(chunkmap.a, 0, buildBuffer, j, chunkmap.a.length);
            */
            j += chunkmap.a.length;
            this.a[k] = chunk.locX;
            this.b[k] = chunk.locZ;
            this.c[k] = chunkmap.b;
            this.d[k] = chunkmap.c;
            this.inflatedBuffers[k] = chunkmap.a;
        }
        // Poweruser start - we know the total size now (j), lets build the buffer and copy over the builderBuffers of each chunk
        byte[] completeBuildBuffer = new byte[j];
        int startIndex = 0;
        for(int a = 0; a < i; a++) {
            System.arraycopy(this.inflatedBuffers[a], 0, completeBuildBuffer, startIndex, this.inflatedBuffers[a].length);
            startIndex += this.inflatedBuffers[a].length;
        }
        Deflater deflater = new Deflater(targetCompressionLevel);
        try {
            deflater.setInput(completeBuildBuffer);
            deflater.finish();
            this.buffer = this.pbb.requestSendBuffer(completeBuildBuffer.length + 100);
            this.size = deflater.deflate(this.buffer);
        } finally {
            deflater.end();
        }
        // Poweruser end


        /* CraftBukkit start - Moved to compress()
        Deflater deflater = new Deflater(-1);

        try {
            deflater.setInput(buildBuffer, 0, j);
            deflater.finish();
            this.buffer = new byte[j];
            this.size = deflater.deflate(this.buffer);
        } finally {
            deflater.end();
        }
        */
    }
/*
    // Add compression method
    public void compress() {
        if (this.buffer != null) {
            return;
        }

        Deflater deflater = localDeflater.get();
        deflater.reset();
        try {
            deflater.setInput(this.buildBuffer);
            deflater.finish();

            this.buffer = new byte[this.buildBuffer.length + 100];
            this.size = deflater.deflate(this.buffer);
        } finally {
            deflater.end();
        }
    }
    // CraftBukkit end

*/
    public static int c() {
        return 5;
    }

    public void a(PacketDataSerializer packetdataserializer) throws IOException { // CraftBukkit - throws IOException
        short short1 = packetdataserializer.readShort();

        this.size = packetdataserializer.readInt();
        this.h = packetdataserializer.readBoolean();
        this.a = new int[short1];
        this.b = new int[short1];
        this.c = new int[short1];
        this.d = new int[short1];
        this.inflatedBuffers = new byte[short1][];
        if (buildBuffer.length < this.size) {
            buildBuffer = new byte[this.size];
        }

        packetdataserializer.readBytes(buildBuffer, 0, this.size);
        byte[] abyte = new byte[PacketPlayOutMapChunk.c() * short1];
        Inflater inflater = new Inflater();

        inflater.setInput(buildBuffer, 0, this.size);

        try {
            inflater.inflate(abyte);
        } catch (DataFormatException dataformatexception) {
            throw new IOException("Bad compressed data format");
        } finally {
            inflater.end();
        }

        int i = 0;

        for (int j = 0; j < short1; ++j) {
            this.a[j] = packetdataserializer.readInt();
            this.b[j] = packetdataserializer.readInt();
            this.c[j] = packetdataserializer.readShort();
            this.d[j] = packetdataserializer.readShort();
            int k = 0;
            int l = 0;

            int i1;

            for (i1 = 0; i1 < 16; ++i1) {
                k += this.c[j] >> i1 & 1;
                l += this.d[j] >> i1 & 1;
            }

            i1 = 2048 * 4 * k + 256;
            i1 += 2048 * l;
            if (this.h) {
                i1 += 2048 * k;
            }

            this.inflatedBuffers[j] = new byte[i1];
            System.arraycopy(abyte, i, this.inflatedBuffers[j], 0, i1);
            i += i1;
        }
    }

    public void b(PacketDataSerializer packetdataserializer) throws IOException { // CraftBukkit - throws IOException
        //compress(); // CraftBukkit  // Poweruser - moved back to the constructor
        packetdataserializer.writeShort(this.a.length);
        packetdataserializer.writeInt(this.size);
        packetdataserializer.writeBoolean(this.h);
        packetdataserializer.writeBytes(this.buffer, 0, this.size);

        for (int i = 0; i < this.a.length; ++i) {
            packetdataserializer.writeInt(this.a[i]);
            packetdataserializer.writeInt(this.b[i]);
            packetdataserializer.writeShort((short) (this.c[i] & '\uffff'));
            packetdataserializer.writeShort((short) (this.d[i] & '\uffff'));
        }

        // Poweruser start
        if(this.pendingUses.decrementAndGet() == 0) {
            this.discard();
        }
        // Poweruser end
    }

    public void a(PacketPlayOutListener packetplayoutlistener) {
        packetplayoutlistener.a(this);
    }

    public String b() {
        StringBuilder stringbuilder = new StringBuilder();

        for (int i = 0; i < this.a.length; ++i) {
            if (i > 0) {
                stringbuilder.append(", ");
            }

            stringbuilder.append(String.format("{x=%d, z=%d, sections=%d, adds=%d, data=%d}", new Object[] { Integer.valueOf(this.a[i]), Integer.valueOf(this.b[i]), Integer.valueOf(this.c[i]), Integer.valueOf(this.d[i]), Integer.valueOf(this.inflatedBuffers[i].length)}));
        }

        return String.format("size=%d, chunks=%d[%s]", new Object[] { Integer.valueOf(this.size), Integer.valueOf(this.a.length), stringbuilder});
    }

    public void handle(PacketListener packetlistener) {
        this.a((PacketPlayOutListener) packetlistener);
    }
}
