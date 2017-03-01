package com.yrek.incant;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.Xml;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.security.MessageDigest;
import java.util.zip.ZipFile;

import org.xmlpull.v1.XmlSerializer;

import com.yrek.ifstd.blorb.Blorb;
import com.yrek.incant.gamelistings.XMLScraper;
import com.yrek.runconfig.SettingsCurrent;

public class Story implements Serializable {
    private static final long serializableVersionID = 0L;
    private static final String TAG = Story.class.getSimpleName();

    private final String name;
    private final String author;
    private String title;
    private final String headline;
    private final String description;
    private final URL downloadURL;
    private final String zipEntry;
    private final URL imageURL;
    private transient Metadata metadata;
    private String hashA;
    private String gameid;
    // 0 = unknown
    private int storyCategory = 0;

    public Story(String name, String author, String headline, String description, URL downloadURL, String zipEntry, URL imageURL) {
        this.name = name;
        this.author = author;
        this.headline = headline;
        this.description = description;
        this.downloadURL = downloadURL;
        this.zipEntry = zipEntry;
        this.imageURL = imageURL;
        // When dealing with downloads, title may need to be read because DownloadManager does not provide
        this.title = "";
        this.hashA = "";
        this.gameid = "";

        Log.i(TAG, "trace Story create " + this.author + ":" + this.name + ":" + this.title);
    }

    public String getName(Context context) {
        if (name.startsWith("newDL"))
        {
            if (title.length() > 0)
            {
                // Log.v(TAG, "Story getName changing: " + name + " to: " + title);
                return title;
            }
            else
            {
                // Log.w(TAG, "Story getName notitle: " + name);
                Metadata m = getMetadata(context);
                if (m != null) {
                    // Log.d(TAG, "getName " + name + " m.title " + m.title);
                    return m.title;
                }
                else
                {
                    // Log.w(TAG, "Story getName no title no metadata: " + name);
                }
            }
        }
        return name;
    }

    // In case we want to fetch the actual name without substitution
    public String getNamePure(Context context) {
        return name;
    }

    private Metadata getMetadata(Context context) {
        if (metadata != null || !getMetadataFile(context).exists()) {
            return metadata;
        }

        Metadata m = new Metadata();
        try {
            new XMLScraper(m).scrape(getMetadataFile(context));
            metadata = m;
            Log.d(TAG, "new metadata author " + m.author + " title " + m.title);
        } catch (Exception e) {
            Log.e(TAG ,"Exception on scrape metadata", e);
        }
        return metadata;
    }

    public String getIFID(Context context) {
        Metadata m = getMetadata(context);
        return m != null ? m.ifid : null;
    }

    public String getAuthor(Context context) {
        Metadata m = getMetadata(context);
        if (m != null) {
            // Log.d(TAG, "getAuthor " + author + " m.author " + m.author);
            return m.author;
        }
        else
        {
            return author;
        }
    }

    public String getHeadline(Context context) {
        Metadata m = getMetadata(context);
        return m != null ? m.headline : headline;
    }

    public String getDescription(Context context) {
        Metadata m = getMetadata(context);
        return m != null ? m.description : description;
    }

    public URL getDownloadURL(Context context) {
        return downloadURL;
    }

    public String getZipEntry(Context context) {
        return zipEntry;
    }

    public static File getRootDir(Context context) {
        if (SettingsCurrent.getFileRootDirInside()) {
            return context.getDir("story", Context.MODE_PRIVATE);
        }
        else {
            return new File("/sdcard/Incant_Stories/setA");
        }
    }

    public static File getStoryDir(Context context, String name) {
        return new File(getRootDir(context), name);
    }

    public static File getZcodeFile(Context context, String name) {
        return new File(getStoryDir(context, name), "zcode");
    }

    public static File getGlulxFile(Context context, String name) {
        return new File(getStoryDir(context, name), "glulx");
    }

    public static File getSaveFile(Context context, String name) {
        return new File(getStoryDir(context, name), "save");
    }

    public static File getCoverImageFile(Context context, String name) {
        return new File(getStoryDir(context, name), "cover");
    }

    public static File getMetadataFile(Context context, String name) {
        return new File(getStoryDir(context, name), "metadata");
    }

    public static File getBlorbFile(Context context, String name) {
        return new File(getStoryDir(context, name), "blorb");
    }

    public static boolean isDownloaded(Context context, String name) {
        return getZcodeFile(context, name).exists() || getGlulxFile(context, name).exists();
    }

    public File getDir(Context context) {
        return getStoryDir(context, name);
    }

    public File getFile(Context context, String file) {
        return new File(getDir(context), file);
    }

    public File getStoryFile(Context context) {
        return isZcode(context) ? getZcodeFile(context) : getGlulxFile(context);
    }

    public File getZcodeFile(Context context) {
        return getZcodeFile(context, name);
    }

    public File getGlulxFile(Context context) {
        return getGlulxFile(context, name);
    }

    public File getSaveFile(Context context) {
        return getSaveFile(context, name);
    }

    public File getCoverImageFile(Context context) {
        return getCoverImageFile(context, name);
    }

    public Bitmap getCoverImageBitmap(Context context) {
        File file = getCoverImageFile(context);
        return file.exists() ? BitmapFactory.decodeFile(file.getPath()) : null;
    }

    public File getMetadataFile(Context context) {
        return getMetadataFile(context, name);
    }

    public File getBlorbFile(Context context) {
        return getBlorbFile(context, name);
    }

    public boolean isDownloaded(Context context) {
        return getZcodeFile(context).exists() || getGlulxFile(context).exists();
    }

    public boolean isZcode(Context context) {
        return getZcodeFile(context).exists();
    }

    public boolean isGlulx(Context context) {
        return getGlulxFile(context).exists();
    }

    public boolean download(Context context) throws IOException {
        return download(context, null);
    }


    public static String getDigestMd5OfFile(String filePath)
    {
        try
        {
            InputStream   input   = new FileInputStream(filePath);
            return getDigestMd5OfInputStream(input);
        }
        catch(Throwable t) {t.printStackTrace();}
        return null;
    }

    public static String getDigestMd5OfInputStream(InputStream input)
    {
        String returnVal = "";
        try
        {
            byte[]        buffer  = new byte[1024];
            MessageDigest md5Hash = MessageDigest.getInstance("MD5");
            int           numRead = 0;
            while (numRead != -1)
            {
                numRead = input.read(buffer);
                if (numRead > 0)
                {
                    md5Hash.update(buffer, 0, numRead);
                }
            }
            input.close();

            byte [] md5Bytes = md5Hash.digest();
            for (int i=0; i < md5Bytes.length; i++)
            {
                returnVal += Integer.toString( ( md5Bytes[i] & 0xff ) + 0x100, 16).substring( 1 );
            }
        }
        catch (Throwable t)
        {t.printStackTrace();}
        return returnVal.toUpperCase();
    }


    public static int MAGIC_FILE_ZIP0 = 0x504b0304;
    public static int MAGIC_FILE_GLULX0 = 0x476c756c;
    public static int MAGIC_FILE_BLORB = 0x464f524d;
    public static int MAGIC_FILE_TADS = 1412640105;

    protected boolean download(Context context, InputStream inputStream) throws IOException {
        boolean downloaded = false;
        getDir(context).mkdirs();
        File tmpFile = File.createTempFile("tmp", ".tmp", getDir(context));
        String freshHashA = "";
        try {
            int magic;

            if (inputStream == null) {
                magic = downloadTo(context, downloadURL, tmpFile);
                if (tmpFile.exists()) {
                    freshHashA = getDigestMd5OfFile(tmpFile.toString());
                    Log.d(TAG, "download file freshHashA " + freshHashA);
                }
            } else {
                magic = downloadTo(context, inputStream, tmpFile);
                boolean failedFirst = false;
                try {
                    inputStream.reset();
                    freshHashA = getDigestMd5OfInputStream(inputStream);
                }
                catch (IOException e)
                {
                    Log.w(TAG, "download InputStream reset failed, doing file");
                    failedFirst = true;
                }
                if (failedFirst)
                {
                    if (tmpFile.exists()) {
                        freshHashA = getDigestMd5OfFile(tmpFile.toString());
                        Log.d(TAG, "download file_2 freshHashA " + freshHashA);
                    }
                }
                Log.d(TAG, "download inputStream freshHashA " + freshHashA);
            }

            // ToDo: track of the hash already exists in our collection, not a duplicate?
            if (freshHashA.length() > 0)
            {
                hashA = freshHashA;
            }

            if (magic == MAGIC_FILE_ZIP0 && zipEntry != null) {
                File tmpEntry = File.createTempFile("tmp", ".zip", getDir(context));
                try {
                    magic = unzipTo(context, tmpFile, tmpEntry);
                    tmpEntry.renameTo(tmpFile);
                } finally {
                    if (tmpEntry.exists()) {
                        tmpEntry.delete();
                    }
                }
            }

            if (magic == MAGIC_FILE_GLULX0) {
                tmpFile.renameTo(getGlulxFile(context));
                downloaded = true;
            } else if ((magic >> 24) >= 3 && (magic >> 24) <= 8) {
                tmpFile.renameTo(getZcodeFile(context));
                downloaded = true;
            } else if (magic == MAGIC_FILE_BLORB) {
                tmpFile.renameTo(getBlorbFile(context));
                Blorb blorb = null;
                try {
                    blorb = Blorb.from(getBlorbFile(context));
                    int coverImage = -1;
                    for (Blorb.Chunk chunk : blorb.chunks()) {
                        switch (chunk.getId()) {
                        case Blorb.IFmd:
                            writeBlorbChunk(context, chunk, getMetadataFile(context));
                            break;
                        case Blorb.Fspc:
                            coverImage = new DataInputStream(new ByteArrayInputStream(chunk.getContents())).readInt();
                            break;
                        default:
                        }
                    }
                    Metadata md = getMetadata(context);
                    if (md != null) {
                        if (md.coverpicture != null) {
                            coverImage = Integer.parseInt(md.coverpicture);
                        }
                        if (md.ifid != null)
                        {
                            Log.i(TAG, "story metadata ifid " + md.ifid);
                        }
                        if (md.title != null)
                        {
                            Log.i(TAG, "story metadata title " + md.title + " known title " + title);
                            if (title.length() == 0)
                            {
                                title = md.title;
                            }
                        }
                    }

                    for (Blorb.Resource res : blorb.resources()) {
                        Blorb.Chunk chunk = res.getChunk();
                        if (chunk == null) {
                            continue;
                        }
                        switch (res.getUsage()) {
                        case Blorb.Exec:
                            if (chunk.getId() == Blorb.ZCOD) {
                                writeBlorbChunk(context, chunk, getZcodeFile(context));
                                downloaded = true;
                            } else if (chunk.getId() == Blorb.GLUL) {
                                writeBlorbChunk(context, chunk, getGlulxFile(context));
                                downloaded = true;
                            }
                            break;
                        case Blorb.Pict:
                            if (res.getNumber() == coverImage && (chunk.getId() == Blorb.PNG || chunk.getId() == Blorb.JPEG)) {
                                writeBlorbChunk(context, chunk, getCoverImageFile(context));
                            }
                            break;
                        default:
                        }
                    }
                } finally {
                    if (blorb != null) {
                        blorb.close();
                    }
                }
            } else if (magic == MAGIC_FILE_TADS) {
                Log.w(TAG, "download TADS-3 magic " + magic);
            }
            else
            {
                Log.w(TAG, "download unmatched magic " + magic);
            }
        } finally {
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
            if (!downloaded) {
                delete(context);
            }
        }

        if (downloaded) {
            try {
                if (imageURL != null && !getCoverImageFile(context).exists()) {
                    downloadTo(context, imageURL, getCoverImageFile(context));
                }
            } catch (Exception e) {
                Log.wtf(TAG,e);
            }
            try {
                if (! getMetadataFile(context).exists()) {
                    Log.d(TAG, "no metadatafile, writing " + getMetadataFile(context));
                    writeMetadata(context, getMetadataFile(context));
                }
            } catch (Exception e) {
                Log.wtf(TAG,e);
            }
        }
        return downloaded;
    }

    protected int unzipTo(Context context, File zipFile, File file) throws IOException {
        InputStream in = null;
        try {
            ZipFile zf = new ZipFile(zipFile);
            in = zf.getInputStream(zf.getEntry(zipEntry));
            FileOutputStream out = null;
            int magic = 0;
            try {
                out = new FileOutputStream(file);
                for (int i = 0; i < 4; i++) {
                    int b = in.read();
                    if (b < 0) {
                        break;
                    } else {
                        out.write(b);
                        magic |= (b&255) << (24 - 8*i);
                    }
                }
                byte[] buffer = new byte[8192];
                for (int n = in.read(buffer); n >= 0; n = in.read(buffer)) {
                    out.write(buffer, 0, n);
                }
            } finally {
                if (out != null) {
                    out.close();
                }
            }
            return magic;
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    protected int downloadTo(Context context, URL url, File file) throws IOException {
        Log.d(TAG, "downloadTo " + file + " " + url);
        InputStream in = null;
        try {
            in = url.openStream();
            return downloadTo(context, in, file);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    protected int downloadTo(Context context, InputStream in, File file) throws IOException {
        File tmpFile = File.createTempFile("tmp","tmp",getDir(context));
        try {
            int magic = 0;
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(tmpFile);
                for (int i = 0; i < 4; i++) {
                    int b = in.read();
                    if (b < 0) {
                        break;
                    } else {
                        out.write(b);
                        magic |= (b&255) << (24 - 8*i);
                    }
                }
                byte[] buffer = new byte[8192];
                for (int n = in.read(buffer); n >= 0; n = in.read(buffer)) {
                    out.write(buffer, 0, n);
                }
            } finally {
                if (out != null) {
                    out.close();
                }
            }
            tmpFile.renameTo(file);
            Log.i(TAG, "downloadTo " + file + " done " + " length " + file.length());
            return magic;
        } finally {
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
        }
    }

    protected void writeBlorbChunk(Context context, Blorb.Chunk chunk, File file) throws IOException {
        getDir(context).mkdir();
        File tmpFile = File.createTempFile("tmp","tmp",getDir(context));
        try {
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(tmpFile);
                chunk.write(out);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
            tmpFile.renameTo(file);
            Log.d(TAG, "writeBlobChunk " + file + " length " + file.length());
        } finally {
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
        }
    }

    public void delete(Context context) {
        File dir = getDir(context);
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        dir.delete();
    }

    public void setListingExtras(int category0) {
        storyCategory = category0;
    }

    public int getStoryCategory()
    {
        return storyCategory;
    }

    /*
    title may differ from name on DownloadManager case, or to allow user to rename 'name' for listing sake
     */
    public String getTitle(Context context) {
        return title;
    }

    private class Metadata implements XMLScraper.Handler {
        String ifid;
        String author;
        String headline;
        String description;
        String coverpicture;
        String title;
        String hashA;

        @Override
        public void startDocument() {
        }

        @Override
        public void endDocument() {
        }

        @Override
        public void element(String path, String value) {
            if ("ifindex/story/bibliographic/author".equals(path)) {
                author = value;
            } else if ("ifindex/story/bibliographic/headline".equals(path)) {
                headline = value;
            } else if ("ifindex/story/bibliographic/description".equals(path)) {
                description = value;
            } else if ("ifindex/story/zcode/coverpicture".equals(path)) {
                coverpicture = value;
            } else if ("ifindex/story/glulx/coverpicture".equals(path)) {
                coverpicture = value;
            } else if ("ifindex/story/identification/ifid".equals(path)) {
                ifid = value;
            } else if ("ifindex/story/bibliographic/title".equals(path)) {
                title = value;
            }
        }
    }

    protected void writeMetadata(Context context, File file) throws IOException {
        getDir(context).mkdir();
        File tmpFile = File.createTempFile("tmp","tmp",getDir(context));
        try {
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(tmpFile);
                XmlSerializer xmlSerializer = Xml.newSerializer();
                xmlSerializer.setOutput(out, "UTF-8");
                xmlSerializer.startDocument("UTF-8", null);
                xmlSerializer.startTag("http://babel.ifarchive.org/protocol/iFiction/", "ifindex");
                xmlSerializer.startTag(null, "story");
                xmlSerializer.startTag(null, "bibliographic");
                if (title != null) {
                    xmlSerializer.startTag(null, "title");
                    xmlSerializer.text(title);
                    xmlSerializer.endTag(null, "title");
                }
                if (author != null) {
                    xmlSerializer.startTag(null, "author");
                    xmlSerializer.text(author);
                    xmlSerializer.endTag(null, "author");
                }
                if (headline != null) {
                    xmlSerializer.startTag(null, "headline");
                    xmlSerializer.text(headline);
                    xmlSerializer.endTag(null, "headline");
                }
                if (description != null) {
                    xmlSerializer.startTag(null, "description");
                    xmlSerializer.text(description);
                    xmlSerializer.endTag(null, "description");
                }
                xmlSerializer.endTag(null, "bibliographic");
                xmlSerializer.endTag(null, "story");
                xmlSerializer.endTag("http://babel.ifarchive.org/protocol/iFiction/", "ifindex");
                xmlSerializer.endDocument();
            } finally {
                if (out != null) {
                    out.close();
                }
            }
            tmpFile.renameTo(file);
        } finally {
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
        }
    }
}
