package com.yrek.incant.glk;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.yrek.ifstd.glk.GlkByteArray;
import com.yrek.ifstd.glk.GlkFile;
import com.yrek.ifstd.glk.GlkIntArray;
import com.yrek.ifstd.glk.GlkStream;
import com.yrek.ifstd.glk.GlkStreamResult;
import com.yrek.ifstd.glk.UnicodeString;

class StreamFile extends Stream {
    private static final long serialVersionUID = 0L;
    final File file;
    final int mode;
    final boolean unicode;
    transient RandomAccessFile randomAccessFile;
    private long filePointer;
    int readCount = 0;
    int writeCount = 0;

    StreamFile(FileRef fileRef, int mode, boolean unicode, int rock) throws IOException {
        this(fileRef.file, mode, unicode, rock);
    }

    StreamFile(File file, int mode, boolean unicode, int rock) throws IOException {
        super(rock);
        this.file = file;
        this.mode = mode;
        this.unicode = unicode;
        this.randomAccessFile = new RandomAccessFile(file, mode == GlkFile.ModeRead ? "r" : "rw");
        switch (mode) {
        case GlkFile.ModeWrite:
            this.randomAccessFile.setLength(0L);
            break;
        case GlkFile.ModeRead:
        case GlkFile.ModeReadWrite:
            this.randomAccessFile.seek(0L);
            break;
        case GlkFile.ModeWriteAppend:
            this.randomAccessFile.seek(this.randomAccessFile.length());
            break;
        default:
            throw new IllegalArgumentException();
        }
        savePointer();
    }
    
    @Override
    public GlkStreamResult close() throws IOException {
        if (randomAccessFile != null) {
            randomAccessFile.close();
        }
        destroy();
        return new GlkStreamResult(readCount, writeCount);
    }

    private void resume() throws IOException {
        if (randomAccessFile == null) {
            randomAccessFile = new RandomAccessFile(file, mode == GlkFile.ModeRead ? "r" : "rw");
            randomAccessFile.seek(filePointer);
        }
    }

    private void savePointer() throws IOException {
        filePointer = randomAccessFile.getFilePointer();
    }

    @Override
    public void putChar(int ch) throws IOException {
        resume();
        writeCount++;
        if (unicode) {
            randomAccessFile.writeInt(ch & 255);
        } else {
            randomAccessFile.write(ch);
        }
        savePointer();
    }

    @Override
    public void putString(CharSequence string) throws IOException {
        resume();
        writeCount += string.length();
        if (unicode) {
            for (int i = 0; i < string.length(); i++) {
                randomAccessFile.writeInt(string.charAt(i) & 255);
            }
        } else {
            randomAccessFile.writeBytes(string.toString());
        }
        savePointer();
    }

    @Override
    public void putBuffer(GlkByteArray buffer) throws IOException {
        resume();
        writeCount += buffer.getArrayLength();
        if (unicode) {
            for (int i = 0; i < buffer.getArrayLength(); i++) {
                randomAccessFile.writeInt(buffer.getByteElementAt(i) & 255);
            }
        } else {
            for (int i = 0; i < buffer.getArrayLength(); i++) {
                randomAccessFile.write(buffer.getByteElementAt(i));
            }
        }
        savePointer();
    }

    @Override
    public void putCharUni(int ch) throws IOException {
        resume();
        writeCount++;
        if (unicode) {
            randomAccessFile.writeInt(ch);
        } else {
            randomAccessFile.write(ch);
        }
        savePointer();
    }

    @Override
    public void putStringUni(UnicodeString string) throws IOException {
        resume();
        writeCount += string.codePointCount();
        if (unicode) {
            for (int i = 0; i < string.codePointCount(); i++) {
                randomAccessFile.writeInt(string.codePointAt(i));
            }
        } else {
            for (int i = 0; i < string.codePointCount(); i++) {
                randomAccessFile.write(string.codePointAt(i));
            }
        }
        savePointer();
    }

    @Override
    public void putBufferUni(GlkIntArray buffer) throws IOException {
        resume();
        writeCount += buffer.getArrayLength();
        if (unicode) {
            for (int i = 0; i < buffer.getArrayLength(); i++) {
                randomAccessFile.writeInt(buffer.getIntElementAt(i));
            }
        } else {
            for (int i = 0; i < buffer.getArrayLength(); i++) {
                randomAccessFile.write(buffer.getIntElementAt(i));
            }
        }
        savePointer();
    }

    @Override
    public void setStyle(int style) {
    }

    @Override
    public void setHyperlink(int linkVal) {
    }

    @Override
    public int getChar() throws IOException {
        resume();
        int ch = -1;
        try {
            if (unicode) {
                ch = randomAccessFile.readInt() & 255;
            } else {
                ch = randomAccessFile.readUnsignedByte();
            }
            readCount++;
        } catch (EOFException e) {
        }
        savePointer();
        return ch;
    }

    @Override
    public int getLine(GlkByteArray buffer) throws IOException {
        resume();
        int index = 0;
        while (index < buffer.getArrayLength() - 1) {
            int ch;
            try {
                if (unicode) {
                    ch = randomAccessFile.readInt() & 255;
                } else {
                    ch = randomAccessFile.readUnsignedByte();
                }
            } catch (EOFException e) {
                break;
            }
            readCount++;
            buffer.setByteElementAt(index, ch);
            index++;
            if (ch == 10) {
                break;
            }
        }
        buffer.setByteElementAt(index, 0);
        savePointer();
        return index;
    }

    @Override
    public int getBuffer(GlkByteArray buffer) throws IOException {
        resume();
        int index = 0;
        while (index < buffer.getArrayLength() - 1) {
            int ch;
            try {
                if (unicode) {
                    ch = randomAccessFile.readInt() & 255;
                } else {
                    ch = randomAccessFile.readUnsignedByte();
                }
            } catch (EOFException e) {
                break;
            }
            readCount++;
            buffer.setByteElementAt(index, ch);
            index++;
        }
        buffer.setByteElementAt(index, 0);
        savePointer();
        return index;
    }

    @Override
    public int getCharUni() throws IOException {
        resume();
        int ch = -1;
        try {
            if (unicode) {
                ch = randomAccessFile.readInt();
            } else {
                ch = randomAccessFile.readUnsignedByte();
            }
            readCount++;
        } catch (EOFException e) {
        }
        savePointer();
        return ch;
    }

    @Override
    public int getLineUni(GlkIntArray buffer) throws IOException {
        resume();
        int index = 0;
        while (index < buffer.getArrayLength() - 1) {
            int ch;
            try {
                if (unicode) {
                    ch = randomAccessFile.readInt();
                } else {
                    ch = randomAccessFile.readUnsignedByte();
                }
            } catch (EOFException e) {
                break;
            }
            readCount++;
            buffer.setIntElementAt(index, ch);
            index++;
            if (ch == 10) {
                break;
            }
        }
        buffer.setIntElementAt(index, 0);
        savePointer();
        return index;
    }

    @Override
    public int getBufferUni(GlkIntArray buffer) throws IOException {
        resume();
        int index = 0;
        while (index < buffer.getArrayLength() - 1) {
            int ch;
            try {
                if (unicode) {
                    ch = randomAccessFile.readInt();
                } else {
                    ch = randomAccessFile.readUnsignedByte();
                }
            } catch (EOFException e) {
                break;
            }
            readCount++;
            buffer.setIntElementAt(index, ch);
            index++;
        }
        buffer.setIntElementAt(index, 0);
        savePointer();
        return index;
    }

    @Override
    public void setPosition(int position, int seekMode) throws IOException {
        resume();
        switch (seekMode) {
        case GlkStream.SeekModeStart:
            randomAccessFile.seek((long) position);
            break;
        case GlkStream.SeekModeCurrent:
            randomAccessFile.seek(randomAccessFile.getFilePointer() + position);
            break;
        case GlkStream.SeekModeEnd:
            randomAccessFile.seek(randomAccessFile.length() + position);
            break;
        default:
            throw new IllegalArgumentException();
        }
        savePointer();
    }

    @Override
    public int getPosition() throws IOException {
        return (int) filePointer;
    }

    @Override
    public DataOutput getDataOutput() {
        return randomAccessFile;
    }

    @Override
    public DataInput getDataInput() {
        return randomAccessFile;
    }
}
