package com.yrek.incant.gamelistings;

import android.content.Context;
import android.util.Log;

import com.yrek.incant.Story;
import com.yrek.runconfig.SettingsCurrent;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public abstract class Scraper {
    public static final String CACHE_DIR = "cache";
    public static final String CACHE_IFARCHIVE = "ifarchive";
    public static final String CACHE_IFDB = "ifdb";

    protected Context context;

    public static final String TAG = "Scraper";
    private final File cacheDir;
    private final File cacheFile;
    private final long cacheTimeout;

    Scraper(Context context, String name, int cacheTimeout) {
        this.context = context;
        if (SettingsCurrent.getFileCacheDirInside()) {
            this.cacheDir = context.getDir(CACHE_DIR, Context.MODE_PRIVATE);
        }
        else
        {
            this.cacheDir = new File("/sdcard/Incant_Stories/cache");
            if (! this.cacheDir.exists())
            {
                this.cacheDir.mkdirs();
            }
        }
        this.cacheFile = new File(cacheDir, name);
        this.cacheTimeout = (long) cacheTimeout;
    }

    public interface PageScraper {
        public void scrape(String line) throws IOException;
    }

    public void addStories(ArrayList<Story> stories, int addCategory) throws IOException {
        DataInputStream in = null;
        try {
            if (! cacheFile.exists()) {
                Log.d(TAG, "[listPopulate] Scraper addStories, no cacheFile " + cacheFile.getPath());
                return;
            }

            Log.d(TAG, "[listPopulate] Scraper addStories, start processing cacheFile " + cacheFile.getPath());

            in = new DataInputStream(new FileInputStream(cacheFile));
            for (;;) {
                String name = in.readUTF();
                String author = in.readUTF();
                String url = in.readUTF();
                String zipFile = in.readUTF();
                String imageURLdata = in.readUTF();

                URL zzzz = new URL(url);
                URL imageUrl = null;
                if (imageURLdata.trim().length() > 0) {
                    try {
                        imageUrl = new URL(imageURLdata);
                    } catch (MalformedURLException e) {
                        Log.w(TAG, "MalformedURLException a:" + imageURLdata, e);
                    }
                }
                String cleanedUpName = name.trim().replace("_", " ");
                Log.d(TAG, "[storyName][cacheFile][listPopulate] setting name from cacheFile '" + name + "'" + " to '" + cleanedUpName + "'");
                Story newStory = new Story(cleanedUpName, author.length() == 0 ? null : author, null, null, zzzz, zipFile.length() == 0 ? null : zipFile, imageUrl);
                StoryHelper.addStory(context, newStory, stories, addCategory);
            }
        } catch (FileNotFoundException e) {
            Log.w(TAG, "FileNotFoundException", e);
        } catch (EOFException e) {
            // Code just blindly reads until EOF?
            Log.w(TAG, "EOFException");
        } catch (MalformedURLException e) {
            // This entry seems to trigger: http://www.allthingsjacq.com/IntroComp15/
            // ToDo: this logic aborts the entire loop if one goes bad? move exception earlier?
            Log.w(TAG, "MalformedURLException", e);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public void scrape() throws IOException {
        if (cacheFile.lastModified() + cacheTimeout > System.currentTimeMillis()) {
            Log.i(TAG, "[cacheFile][listPopulate] cacheFile good " + cacheFile);
            if (SettingsCurrent.getDownloadSkipCachefile()) {
                return;
            }
        }
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("tmp", "tmp", cacheDir);
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(tmpFile);
                Log.d(TAG, "[cacheFile] scrape:tmpFile " + tmpFile);
                scrape(new DataOutputStream(out));
            } finally {
                if (out != null) {
                    out.close();
                }
            }
            Log.d(TAG, "[cacheFile] scrape:tmpFile:rename " + tmpFile + " to " + cacheFile);
            boolean goodRename = tmpFile.renameTo(cacheFile);
            Log.d(TAG, "[cacheFile] scrape:tmpFile:rename:after "  + cacheFile.length() + " " + goodRename);
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
    }

    abstract void scrape(DataOutputStream out) throws IOException;

    void scrapeURL(String url, PageScraper pageScraper) throws IOException {
        InputStream in = null;
        try {
            in = new URL(url).openStream();
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                pageScraper.scrape(line);
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public static void writeStory(DataOutputStream out, String name, String author, String url, String zipFile, String imageURL) throws IOException {
        Log.i(TAG, "[cacheFile][writeStory] name=" + name + " author=" + author);
        out.writeUTF(name);
        out.writeUTF(author == null ? "" : author);
        out.writeUTF(url);
        out.writeUTF(zipFile == null ? "" : zipFile);
        out.writeUTF(imageURL == null ? "" : imageURL);
    }
}