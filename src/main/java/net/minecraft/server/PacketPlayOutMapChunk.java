package net.minecraft.server;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import de.minetick.MinetickMod;
import de.minetick.packetbuilder.PacketBuilderBuffer;

public class PacketPlayOutMapChunk extends Packet {

    private int a;
    private int b;
    private int c;
    private int d;
    private byte[] e;
    private byte[] buffer;
    private boolean inflatedBuffer;
    private int size;
    //private static byte[] buildBuffer = new byte[196864];
    // Poweruser start
    private static final ThreadLocal<byte[]> localBuildBuffer = new ThreadLocal<byte[]>() {
        @Override
        protected byte[] initialValue() {
            return new byte[196864];
        }
    };

    private AtomicInteger pendingUses;
    private static int targetCompressionLevel = 7;

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

    private PacketBuilderBuffer pbb;

    public void discard() {
        if(this.pbb != null) {
            if(this.e != null) {
                this.pbb.offerSendBuffer(this.e);
                this.e = null;
            }
            this.pbb = null;
        }
    }
    // Poweruser end
    public PacketPlayOutMapChunk() {}

    //public PacketPlayOutMapChunk(Chunk chunk, boolean flag, int i) {
    public PacketPlayOutMapChunk(PacketBuilderBuffer pbb, Chunk chunk, boolean flag, int i) { // Poweruser
        this.pbb = pbb; // Poweruser
        this.a = chunk.locX;
        this.b = chunk.locZ;
        this.inflatedBuffer = flag;
        ChunkMap chunkmap = a(chunk, flag, i);
        //Deflater deflater = new Deflater(-1);
        Deflater deflater = new Deflater(targetCompressionLevel); // Poweruser

        this.d = chunkmap.c;
        this.c = chunkmap.b;

        try {
            this.buffer = chunkmap.a;
            deflater.setInput(chunkmap.a, 0, chunkmap.a.length);
            deflater.finish();
            //this.e = new byte[chunkmap.a.length];
            this.e = this.pbb.requestSendBuffer(chunkmap.a.length); // Poweruser
            this.size = deflater.deflate(this.e);
        } finally {
            deflater.end();
        }
    }

    public static int c() {
        return 196864;
    }

    public void a(PacketDataSerializer packetdataserializer) throws IOException { // Poweruser - added throws Exception
        this.a = packetdataserializer.readInt();
        this.b = packetdataserializer.readInt();
        this.inflatedBuffer = packetdataserializer.readBoolean();
        this.c = packetdataserializer.readShort();
        this.d = packetdataserializer.readShort();
        this.size = packetdataserializer.readInt();
        /*
        if (buildBuffer.length < this.size) {
            buildBuffer = new byte[this.size];
        }
        */
        if(localBuildBuffer.get().length < this.size) {
            localBuildBuffer.set(new byte[this.size]);
        }

        //packetdataserializer.readBytes(buildBuffer, 0, this.size);
        packetdataserializer.readBytes(localBuildBuffer.get(), 0, this.size); // Poweruser
        int i = 0;

        int j;

        for (j = 0; j < 16; ++j) {
            i += this.c >> j & 1;
        }

        j = 12288 * i;
        if (this.inflatedBuffer) {
            j += 256;
        }

        this.buffer = new byte[j];
        Inflater inflater = new Inflater();

        //inflater.setInput(buildBuffer, 0, this.size);
        inflater.setInput(localBuildBuffer.get(), 0, this.size); // Poweruser

        try {
            inflater.inflate(this.buffer);
        } catch (DataFormatException dataformatexception) {
            throw new IOException("Bad compressed data format");
        } finally {
            inflater.end();
        }
    }

    public void b(PacketDataSerializer packetdataserializer) {
        packetdataserializer.writeInt(this.a);
        packetdataserializer.writeInt(this.b);
        packetdataserializer.writeBoolean(this.inflatedBuffer);
        packetdataserializer.writeShort((short) (this.c & '\uffff'));
        packetdataserializer.writeShort((short) (this.d & '\uffff'));
        packetdataserializer.writeInt(this.size);
        packetdataserializer.writeBytes(this.e, 0, this.size);

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
        return String.format("x=%d, z=%d, full=%b, sects=%d, add=%d, size=%d", new Object[] { Integer.valueOf(this.a), Integer.valueOf(this.b), Boolean.valueOf(this.inflatedBuffer), Integer.valueOf(this.c), Integer.valueOf(this.d), Integer.valueOf(this.size)});
    }

    public static ChunkMap a(Chunk chunk, boolean flag, int i) {
        int j = 0;
        ChunkSection[] achunksection = chunk.i();
        int k = 0;
        ChunkMap chunkmap = new ChunkMap();
        //byte[] abyte = buildBuffer;
        byte[] abyte = localBuildBuffer.get(); // Poweruser

        if (flag) {
            chunk.q = true;
        }

        int l;

        for (l = 0; l < achunksection.length; ++l) {
            if (achunksection[l] != null && (!flag || !achunksection[l].isEmpty()) && (i & 1 << l) != 0) {
                chunkmap.b |= 1 << l;
                if (achunksection[l].getExtendedIdArray() != null) {
                    chunkmap.c |= 1 << l;
                    ++k;
                }
            }
        }

        for (l = 0; l < achunksection.length; ++l) {
            if (achunksection[l] != null && (!flag || !achunksection[l].isEmpty()) && (i & 1 << l) != 0) {
                byte[] abyte1 = achunksection[l].getIdArray();

                System.arraycopy(abyte1, 0, abyte, j, abyte1.length);
                j += abyte1.length;
            }
        }

        NibbleArray nibblearray;

        for (l = 0; l < achunksection.length; ++l) {
            if (achunksection[l] != null && (!flag || !achunksection[l].isEmpty()) && (i & 1 << l) != 0) {
                nibblearray = achunksection[l].getDataArray();
                System.arraycopy(nibblearray.a, 0, abyte, j, nibblearray.a.length);
                j += nibblearray.a.length;
            }
        }

        for (l = 0; l < achunksection.length; ++l) {
            if (achunksection[l] != null && (!flag || !achunksection[l].isEmpty()) && (i & 1 << l) != 0) {
                nibblearray = achunksection[l].getEmittedLightArray();
                System.arraycopy(nibblearray.a, 0, abyte, j, nibblearray.a.length);
                j += nibblearray.a.length;
            }
        }

        if (!chunk.world.worldProvider.g) {
            for (l = 0; l < achunksection.length; ++l) {
                if (achunksection[l] != null && (!flag || !achunksection[l].isEmpty()) && (i & 1 << l) != 0) {
                    nibblearray = achunksection[l].getSkyLightArray();
                    System.arraycopy(nibblearray.a, 0, abyte, j, nibblearray.a.length);
                    j += nibblearray.a.length;
                }
            }
        }

        if (k > 0) {
            for (l = 0; l < achunksection.length; ++l) {
                if (achunksection[l] != null && (!flag || !achunksection[l].isEmpty()) && achunksection[l].getExtendedIdArray() != null && (i & 1 << l) != 0) {
                    nibblearray = achunksection[l].getExtendedIdArray();
                    System.arraycopy(nibblearray.a, 0, abyte, j, nibblearray.a.length);
                    j += nibblearray.a.length;
                }
            }
        }

        if (flag) {
            byte[] abyte2 = chunk.m();

            System.arraycopy(abyte2, 0, abyte, j, abyte2.length);
            j += abyte2.length;
        }

        chunkmap.a = new byte[j];
        System.arraycopy(abyte, 0, chunkmap.a, 0, j);
        chunk.world.antiXRay.orebfuscate(chunkmap.a, chunkmap.a.length, chunk, chunkmap.b); // Poweruser
        return chunkmap;
    }

    // Poweruser start
    @Override
    public void handle(PacketListener packetlistener) {
        this.a((PacketPlayOutListener) packetlistener);
    }
    // Poweruser end
}
