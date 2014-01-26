package de.minetick.packetbuilder;

import java.util.Iterator;
import java.util.LinkedList;

public class PacketBuilderBuffer {

    private LinkedList<byte[]> sendBufferCache = new LinkedList<byte[]>();
    private int notUsedSendBuffers = Integer.MAX_VALUE;

    public PacketBuilderBuffer() {}

    public void offerSendBuffer(byte[] array) {
        synchronized(this.sendBufferCache) {
            this.sendBufferCache.add(array);
        }
    }

    public byte[] requestSendBuffer(int length) {
        synchronized(this.sendBufferCache) {
            return this.checkInList(this.sendBufferCache, length);
        }
    }

    public void clear() {
        synchronized(this.sendBufferCache) {
            this.sendBufferCache.clear();
        }
    }

    private void checkSendBufferUsage(int newSize) {
        if(newSize < this.notUsedSendBuffers) {
            this.notUsedSendBuffers = newSize;
        }
    }

    public void releaseUnusedBuffers() {
        if(this.notUsedSendBuffers <= this.sendBufferCache.size()) {
            synchronized(this.sendBufferCache) {
                for(int i = 0; i < this.notUsedSendBuffers; i++) {
                    this.sendBufferCache.removeFirst();
                }
            }
            this.notUsedSendBuffers = Integer.MAX_VALUE;
        }
    }

    private byte[] checkInList(LinkedList<byte[]> list, int length) {
        Iterator<byte[]> iter = list.descendingIterator();
        byte[] tmp;
        int size = list.size();
        while(iter.hasNext()) {
            tmp = iter.next();
            if(tmp.length >= length) {
                iter.remove();
                if(list == this.sendBufferCache) {
                    this.checkSendBufferUsage(size - 1);
                }
                return tmp;
            }
        }
        tmp = null;
        return new byte[length];
    }
}
