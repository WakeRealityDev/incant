package com.wakereality.apphelpersadupe.fileutils;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Created by Stephen A. Gutknecht on 3/31/17.
 * ref: http://stackoverflow.com/questions/29867121/how-to-copy-programmatically-a-file-to-another-directory
 */

public class FileCopy {

    public static boolean copyFileOrDirectory(String srcDir, String dstDir) {

        try {
            File src = new File(srcDir);
            File dst = new File(dstDir, src.getName());

            if (src.isDirectory()) {

                String files[] = src.list();
                int filesLength = files.length;
                for (int i = 0; i < filesLength; i++) {
                    String src1 = (new File(src, files[i]).getPath());
                    String dst1 = dst.getPath();
                    copyFileOrDirectory(src1, dst1);

                }
            } else {
                copyFile(src, dst);
            }
            return true;
        } catch (Exception e) {
            Log.e("FileCopy", "Exception in file copy " + srcDir + " " + dstDir, e);
            return false;
        }
    }

    public static boolean copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.getParentFile().exists())
            destFile.getParentFile().mkdirs();

        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }

        if (destFile.exists()) {
            if (destFile.length() == sourceFile.length()) {
                return true;
            }
        }
        return false;
    }
}
