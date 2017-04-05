package com.yrek.ifstd.blorb;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.TreeMap;

public abstract class Blorb implements Closeable {
    public static final int FORM = 0x464f524d;
    public static final int IFRS = 0x49465253;
    public static final int RIdx = 0x52496478;
    public static final int Pict = 0x50696374;
    public static final int Snd  = 0x536e6420;
    public static final int Data = 0x44617461;
    public static final int Exec = 0x45786563;
    public static final int IFhd = 0x49466864;
    public static final int Plte = 0x506c7465;
    public static final int Fspc = 0x46737063;
    public static final int RDes = 0x52446573;
    public static final int IFmd = 0x49466d64;
    public static final int RelN = 0x52656c4e;
    public static final int Reso = 0x5265736f;
    public static final int APal = 0x4150616c;
    public static final int Loop = 0x4c6f6f70;
    public static final int AUTH = 0x41555448;
    public static final int C    = 0x28432920;
    public static final int ANNO = 0x414e4e4f;
    public static final int SNam = 0x534e616d;

    public static final int PNG  = 0x504e4720;
    public static final int JPEG = 0x4a504547;
    public static final int OGGV = 0x4f474756;
    public static final int MOD  = 0x4d4f4420;
    public static final int SONG = 0x534f4e47;
    public static final int TEXT = 0x54455854;
    public static final int BINA = 0x42494e41;
    public static final int ZCOD = 0x5a434f44;
    public static final int GLUL = 0x474c554c;
    public static final int TAD2 = 0x54414422;
    public static final int TAD3 = 0x54414423;
    public static final int HUGO = 0x4855474f;
    public static final int ALAN = 0x414c414e;
    public static final int ADRI = 0x41445249;
    public static final int LEVE = 0x4c455645;
    public static final int AGT  = 0x41475420;
    public static final int MAGS = 0x4d414753;
    public static final int ADVS = 0x41445653;
    public static final int EXEC = 0x45584543;

    public static Blorb from(File file) throws IOException {
        return new FileBlorb(file);
    }

    public static Blorb from(byte[] bytes) {
        return new ByteArrayBlorb(bytes);
    }

    private final TreeMap<Long,Chunk> chunks = new TreeMap<Long,Chunk>();
    private final LinkedList<Resource> resources = new LinkedList<Resource>();
    private boolean initialized = false;

    public abstract void close() throws IOException;

    protected abstract int readInt() throws IOException;
    protected abstract long getPosition() throws IOException;
    protected abstract void seek(long position) throws IOException;
    protected abstract byte[] getChunkContents(long start, int length) throws IOException;
    protected abstract void writeChunk(long start, int length, OutputStream out) throws IOException;

    public synchronized void init() throws IOException {
        if (initialized) {
            return;
        }
        seek(0L);
        if (readInt() != FORM) {
            throw new IOException("Invalid file");
        }
        long eof = 8 + (0xffffffffL & readInt());
        if (readInt() != IFRS) {
            throw new IOException("Invalid file");
        }
        if (eof > 12) {
            for (;;) {
                Chunk chunk = new Chunk(getPosition(), readInt(), readInt());
                chunks.put(chunk.start, chunk);
                if (chunk.start + 8 + chunk.length >= eof - 8) {
                    break;
                }
                seek(chunk.start + 8 + ((chunk.length+1)&0xfffffffe));
            }
        }
        for (Chunk chunk : chunks.values()) {
            if (chunk.id == RIdx) {
                seek(chunk.start + 8);
                int count = readInt();
                for (int i = 0; i < count; i++) {
                    resources.add(new Resource(readInt(), readInt(), readInt()));
                }
            }
        }
        initialized = true;
    }

    public Iterable<Chunk> chunks() throws IOException {
        init();
        return chunks.values();
    }

    public Iterable<Resource> resources() throws IOException {
        init();
        return resources;
    }

    public class Chunk {
        final long start;
        final int id;
        final int length;

        Chunk(long start, int id, int length) {
            this.start = start;
            this.id = id;
            this.length = length;
        }

        public int getId() {
            return id;
        }

        public int getLength() {
            return length;
        }

        public byte[] getContents() throws IOException {
            return getChunkContents(start + 8, length);
        }

        public void write(OutputStream out) throws IOException {
            writeChunk(start + 8, length, out);
        }
    }

    public class Resource {
        final int usage;
        final int number;
        final long start;

        Resource(int usage, int number, int start) {
            this.usage = usage;
            this.number = number;
            this.start = 0xffffffffL & start;
        }

        public int getUsage() {
            return usage;
        }

        public int getNumber() {
            return number;
        }

        public Chunk getChunk() {
            return chunks.get(start);
        }

        @Override
        public String toString() {
            return "number " + getNumber() + " usage " + getUsage();
        }
    }
}
