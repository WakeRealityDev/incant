package com.wakereality.apphelpersadupe.fileutils;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

/**
* Created by Stephen A. Gutknecht on 1/7/17.
 */

public class HashFile {

    public static String hashFileSHA256(final File inFile) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            FileInputStream fis = new FileInputStream(inFile.getPath());

            byte[] dataBytes = new byte[8192];

            int nread = 0;
            while ((nread = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }
            ;
            byte[] digestBytes = md.digest();

            final String outHex = encodeHexString(digestBytes);
            if (outHex.length() != 64) {
                Log.e("HashFile", "hashFileSHA256 SHA-256 SHA256 got irregular length? " + outHex + " source " + inFile.getPath());
            }
            return outHex;
        } catch (Exception e0) {
            Log.e("HashFile", "Exception SHA-256 hash", e0);
            return null;
        }
    }

    private final static char[] hexArray = "0123456789abcdef".toCharArray();

    private static String encodeHexString(byte[] bytes) {
        final char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String hashFileMD5(final File inFile) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            FileInputStream fis = new FileInputStream(inFile.getPath());

            byte[] dataBytes = new byte[8192];

            int nread = 0;
            while ((nread = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }
            ;
            byte[] digestBytes = md.digest();

            final String outHex = encodeHexString(digestBytes);
            if (outHex.length() != 32) {
                Log.e("HashFile", "hashFileMD5 MD5 got irregular length? " + outHex + " source " + inFile.getPath());
            }
            return outHex;
        } catch (Exception e0) {
            Log.e("HashFile", "Exception MD5 hash", e0);
            return null;
        }
    }
}
