package com.yrek.ifstd.blorb;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class FileBlorb extends Blorb {
    private final RandomAccessFile file;

    public FileBlorb(File file) throws IOException {
        this.file = new RandomAccessFile(file, "r");
    }

    @Override
    public void close() throws IOException {
        file.close();
    }

    @Override
    protected int readInt() throws IOException {
        return file.readInt();
    }

    @Override
    protected long getPosition() throws IOException {
        return file.getFilePointer();
    }

    @Override
    protected void seek(long position) throws IOException {
        file.seek(position);
    }

    @Override
    protected byte[] getChunkContents(long start, int length) throws IOException {
        file.seek(start);
        byte[] contents = new byte[length];
        file.readFully(contents);
        return contents;
    }

    @Override
    protected void writeChunk(long start, int length, OutputStream out) throws IOException {
        file.seek(start);
        int total = 0;
        byte[] buffer = new byte[8192];
        while (total < length) {
            int count = file.read(buffer, 0, Math.min(buffer.length, length - total));
            if (count < 0) {
                break;
            }
            out.write(buffer, 0, count);
            total += count;
        }
    }
}
