package com.yrek.ifstd.blorb;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class ByteArrayBlorb extends Blorb {
    private final byte[] bytes;
    private int position = 0;

    public ByteArrayBlorb(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    protected int readInt() throws IOException {
        if (position + 4 > bytes.length) {
            throw new EOFException();
        }
        position += 4;
        return ((bytes[position - 4]&255) << 24)
            | ((bytes[position - 3]&255) << 16)
            | ((bytes[position - 2]&255) << 8)
            | ((bytes[position - 1]&255) << 0);
    }

    @Override
    protected long getPosition() throws IOException {
        return (long) position;
    }

    @Override
    protected void seek(long position) throws IOException {
        this.position = (int) position;
    }

    @Override
    protected byte[] getChunkContents(long start, int length) throws IOException {
        return Arrays.copyOfRange(bytes, (int) start, length + (int) start);
    }

    @Override
    protected void writeChunk(long start, int length, OutputStream out) throws IOException {
        out.write(bytes, (int) start, length);
    }
}
