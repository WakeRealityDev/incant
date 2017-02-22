package com.yrek.incant.glk;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class AudioConversion {
    public static final int FORM = 0x464f524d;
    public static final int AIFF = 0x41494646;
    public static final int COMM = 0x434f4d4d;
    public static final int SSND = 0x53534e44;

    public static final int RIFF = 0x52494646;
    public static final int WAVE = 0x57415645;
    public static final int fmt  = 0x666d7420;
    public static final int data = 0x64617461;

    public static boolean aiffToWav(File aiff, OutputStream wav) throws IOException {
        RandomAccessFile in = null;
        try {
            in = new RandomAccessFile(aiff, "r");
            in.seek(0L);
            if (in.readInt() != AIFF) {
                return false;
            }

            long commPosition = 0;
            long ssndPosition = 0;
            int ssndSize = 0;
            for (;;) {
                long start = in.getFilePointer();
                if (start >= in.length()) {
                    return false;
                }
                int id = in.readInt();
                int size = in.readInt();
                if (id == COMM) {
                    commPosition = start;
                    if (ssndPosition != 0) {
                        break;
                    }
                } else if (id == SSND) {
                    ssndPosition = start;
                    ssndSize = size;
                    if (commPosition != 0) {
                        break;
                    }
                }
                in.seek(start + 8 + (size+1)/2*2);
            }

            in.seek(commPosition + 8);
            int numChannels = in.readUnsignedShort();
            in.seek(commPosition + 14);
            int bitsPerSample = in.readUnsignedShort();
            switch (bitsPerSample/numChannels) {
            case 8:
            case 16:
                break;
            default:
                return false;
            }
            in.seek(commPosition + 16);
            int sampleRate;
            int e = in.readUnsignedShort();
            long m = in.readLong();
            sampleRate = (int) (m >>> (16383 - e + 63));

            DataOutputStream out = new DataOutputStream(wav);
            out.writeInt(RIFF);
            write32LE(out, 36 + (ssndSize - 8));
            out.writeInt(WAVE);

            out.writeInt(fmt);
            write32LE(out, 16); // fmt size
            write16LE(out, 1); // PCM = 1
            write16LE(out, numChannels);
            write32LE(out, sampleRate);
            write32LE(out, sampleRate*numChannels*bitsPerSample/8);
            write16LE(out, (numChannels*bitsPerSample+7)/8);
            write16LE(out, bitsPerSample);

            out.writeInt(data);
            write32LE(out, ssndSize - 8);

            in.seek(ssndPosition + 16);
            if (bitsPerSample/numChannels <= 8) {
                for (int i = 0; i < ssndSize - 8; i++) {
                    out.write(in.read());
                }
            } else {
                for (int i = 0; i < ssndSize - 8; i += 2) {
                    int msb = in.read();
                    out.write(in.read());
                    out.write(msb);
                }
            }
            return true;
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public static boolean modToWav(File mod, OutputStream wav) throws IOException {
        return false;
    }

    public static boolean songToWav(File mod, OutputStream wav) throws IOException {
        return false;
    }

    private static void write32LE(OutputStream out, int value) throws IOException {
        out.write(value);
        out.write(value>>8);
        out.write(value>>16);
        out.write(value>>24);
    }

    private static void write16LE(OutputStream out, int value) throws IOException {
        out.write(value);
        out.write(value>>8);
    }
}
