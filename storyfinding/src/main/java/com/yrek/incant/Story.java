package com.yrek.incant;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.util.Log;
import android.util.Xml;
import android.widget.TextView;

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

import org.greenrobot.eventbus.EventBus;
import org.xmlpull.v1.XmlSerializer;

// renamed package to avoid conflict
import com.wakereality.apphelpersadupe.fileutils.FileCopy;
import com.wakereality.apphelpersadupe.fileutils.HashFile;
import com.wakereality.storyfinding.EventStoryListDownloadResult;
import com.wakereality.storyfinding.R;
import com.wakereality.thunderstrike.EchoSpot;
import com.wakereality.thunderstrike.dataexchange.EngineConst;
import com.yrek.ifstd.blorb.Blorb;
import com.yrek.incant.gamelistings.StoryHelper;
import com.yrek.incant.gamelistings.XMLScraper;
import com.yrek.runconfig.SettingsCurrent;

/*
ToDo: this might be a heavy object for using as RecyclerView listing of 5000 stories, high RAM usage
  Maybe make a class underneith this one that is lightweight, only the minimal fields for the listing
    And expand to a higher class for download/launch/single-story detail

     The CSV import already has a smaller-field object StoryEntryIFDB

  Having worked with this code for several months, some comments about complexity:
    This code uses the story title, the name of the story, to construct storage path
    and folder names / file names.  This was the legacy code and the chocie was made to stick with it.
    However, it proves tricky because it drops the file extension for the folder it creates
    and generically centralizes all variations into one (all z5, z6, z8 become just z basically -
    which doesn't allow awareness of multiple available interpreters to run tricky ones like .z6).
    This makes sharing the resulting downloads with outside apps more messy
    Further, the name can't be changed easily - as it's a key to the system in locating
    and listing it - so if you come up with a better name later - you have remember how the original
    story name (folder name) as dervied from the original circumstances.  Example: In a situation of
    pre-download vs. post-down there can be changes to the name informaton - and a better name
    may be found post-download.

    In contract, Thunderword's design is to SHA256 hash all data files and use that as an index.
    So renaming of files, duplicate files, moving files around on storage (duplicate network paths
      to the same file) will end up still having the same key to the same author, title, save games, history
      of last play, etc.  This has worked well in practice - as IF data files are stable and do not have
      any kind of lossy compression.  This design would NOT have worked well for a MP3 player where the same
      track (song) may have 5 variations due to compression and a SHA256 hash would provide no clue that they
      are duplicates.
 */
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
    private String hashSHA256A = null;
    private int engineCode = EngineConst.ENGINE_UNKNOWN;
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

        if (SettingsCurrent.debugLogStoryCreate)
            Log.v(TAG, "trace Story create " + this.author + ":" + this.name + ":" + this.title);
    }

    public Story(String name, String author, String headline, String description, String storyHashSHA256, URL imageURL, int storyEngineCode) {
        this.name = name;
        this.author = author;
        this.headline = headline;
        this.description = description;
        this.downloadURL = null;
        this.zipEntry = null;
        this.imageURL = imageURL;
        // When dealing with downloads, title may need to be read because DownloadManager does not provide
        this.title = "";
        this.hashSHA256A = storyHashSHA256;
        this.engineCode = storyEngineCode;
        this.gameid = "";

        if (SettingsCurrent.debugLogStoryCreate)
            Log.v(TAG, "trace Story create " + this.author + ":" + this.name + ":" + this.title);
    }


/*
########################################################################################################################################################
    ## The messy section, filename generation
*/

//LEFT_WHERE: on Samsung Galxy emulator, freshly wiped
//      Why isn't "Life on Mars" showing up twice? is CSV import not creating two?
// Life on Mars. Why is the english one not getting image. How exactly is a .z5 file getting images anyway?

    // IFDB MysQL database conventions
    private String languageIdentifier = "en";

    /*
    This is the start of an attempt to split the usage of
    The story "Life on Mars?" on IFDB shows this problem of assuming two stories can't ahve the identical title
       SHA-256 hash would say they are non-equal, which is what we want. And multiple-editions of the same story, etc.
     */
    public String getStorageName(Context context) {
        if (! languageIdentifier.equals("en")) {
            // ToDo: Eventualy replace this with SHA256 and not break any "DisplayName" usage on-screen
            return getName(context).replace(" ", "_").replace("\t", "_").replace(".", "_").replace("/", "_").replace("\\", "_").replace("+", "_") + "__" + languageIdentifier;
        } else {
            // ToDo: Eventualy replace this with SHA256 and not break any "DisplayName" usage on-screen
            return getName(context).replace(" ", "_").replace("\t", "_").replace(".", "_").replace("/", "_").replace("\\", "_").replace("+", "_");
        }
    }

    public String getName(Context context) {
        if (name.startsWith("newDL"))
        {
            if (title.length() > 0)
            {
                Log.v(TAG, "[storyName] Story getName changing: " + name + " to: " + title);
                return title;
            }
            else
            {
                Log.w(TAG, "[storyName] Story getName notitle: " + name);
                Metadata m = getMetadata(context);
                if (m != null) {
                    Log.d(TAG, "[storyName] getName " + name + " m.title " + m.title);
                    return m.title;
                }
                else
                {
                    Log.w(TAG, "[storyName] Story getName no title no metadata: " + name);
                }
            }
        }
        return name;
    }

    // In case we want to fetch the actual name without substitution
    public String getNamePure(Context context) {
        return name;
    }


    public String generateDownloadFilename(Context context) {
        return generateDownloadFilename(context, downloadURL);
    }

    public String generateDownloadFilename(Context context, URL sourcePathOrUrl) {
        String fileExtension = "." + StoryHelper.getUsefulFileExtensionFromURL(sourcePathOrUrl);

        String storyNameSanitizedWithoutExtension =  "Incant__"  + getStorageName(context);

        switch (fileExtension) {
            case ".unknown":
            case ".tmp":
            case ".zip":
            case ".blorb":
            case ".blb":
                traceDownlaodChecked += "N";
                Log.w(TAG, "[StoryDL_Path] generateDownloadFilename undesired extension for URL: " + sourcePathOrUrl);
                if (foundKeepFilesPathsPileA == null) {
                    traceDownlaodChecked += "O";
                    rebuildStaticKeepFilesPathPile(context);
                }

                // First line is started with a \n to ensure pattern is total
                final int indexOfMatch = foundKeepFilesPathsPileA.indexOf("\n" + storyNameSanitizedWithoutExtension);
                if (indexOfMatch < 0) {
                    traceDownlaodChecked += "P";
                    Log.w(TAG, "[IncantHash][StoryDL_Path] foundKeepFilesPathsPileA failed to find? " + storyNameSanitizedWithoutExtension);
                } else {
                    traceDownlaodChecked += "Q";
                    final int indexOfIntEnd = foundKeepFilesPathsPileA.indexOf("\n", indexOfMatch + 1 /* length of leading 'n' */);
                    final String beyondTheMatch = foundKeepFilesPathsPileA.substring(indexOfMatch + 1 /* length of '\n' */, indexOfIntEnd);
                    // ALog.w("[IncantHash] foundKeepFilesPathsPileA FOUND? " + storyNameSanitizedWithoutExtension + " EXTENSION: " + beyondTheMatch);
                    return beyondTheMatch;
                }

                /*
                if (fileExtension.equals(".unknown")) {
                    if (getName(context).contains("Zork")) {
                        Log.d(TAG, "Forcing '_Zork' file extention to 'z5' zip file");
                        fileExtension = ".z5";
                    }
                }
                */
                break;
        }

        traceDownlaodChecked += "R";
        return storyNameSanitizedWithoutExtension + fileExtension;
    }


    public void setLanguageIdentifier(String languageIdentifierString) {
        this.languageIdentifier = languageIdentifierString;
    }

    public String getLanguageIdentifier() {
        return this.languageIdentifier;
    }

    public File getDir(Context context) {
        return getStoryDir(context, getStorageName(context));
    }


/*
########################################################################################################################################################
    ## END The messy section, filename generation. On to routine messy code.
*/

    private Metadata getMetadata(Context context) {
        if (metadata != null || !getMetadataFile(context).exists()) {
            return metadata;
        }

        Metadata m = new Metadata();
        try {
            new XMLScraper(m).scrape(getMetadataFile(context));
            metadata = m;
            Log.d(TAG, "[xmlScrape] just got new metadata author " + m.author + " title " + m.title);
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
            Log.d(TAG, "[xmlScrape][metaData] got null metadata, getAuthor " + author + " m.author " + m.author);
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
            // ToDo: yes, these are hard-coded paths, but in a lot of ways this is the most consistent thing in Android - as this app relies on the ability for other apps to find the files.
            // ToDo: become a content-provider, do own sha256-hash, and publish stories over to Thunderword via key=sha256 and content-provider is means to share data.
            //   A common library with Thunderword for this.
            return new File("/sdcard/Incant_Stories/setA");
        }
    }

    public static File getStoryDir(Context context, String name) {
        return new File(getRootDir(context), name);
    }

    public static File getDownloadKeepDir(Context context) {
        // ToDo: yes, these are hard-coded paths, but in a lot of ways this is the most consistent thing in Android - as this app relies on the ability for other apps to find the files.
        // ToDo: become a content-provider, do own sha256-hash, and publish stories over to Thunderword via key=sha256 and content-provider is means to share data.
        //   A common library with Thunderword for this.
        return new File("/sdcard/Incant_Stories/DownloadKeepA");
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

    /*
    Do not call this for rename targets
     */
    public static File getBlorbFile(Context context, String name) {
        return new File(getStoryDir(context, name), "blorb");
    }

    public static File getBlorbFile(Context context, String name, boolean isGlulxBlorb) {
        if (isGlulxBlorb) {
            return new File(getStoryDir(context, name), "gblorb");
        } else {
            return new File(getStoryDir(context, name), "zblorb");
        }
    }

    protected boolean isDownloadedCachedAnswer = false;
    private boolean isDownloadedCachedCheck = false;
    private String downloadKeepFilePath = null;
    public File keepFile;
    public String traceDownlaodChecked = "";

    public boolean isDownloadedExtensiveCheck(Context context) {
        if (isDownloadedCachedCheck) {
            return isDownloadedCachedAnswer;
        } else {
            // First check this run of the app, slower code path, non-cached
            isDownloadedCachedCheck = true;
            if (hashSHA256A != null) {
                traceDownlaodChecked += "A";
                isDownloadedCachedAnswer = true;
                return true;
            }
            if (getZcodeFile(context).exists()) {
                traceDownlaodChecked += "B";
                isDownloadedCachedAnswer = true;
                return true;
            }
            if (getGlulxFile(context).exists()) {
                traceDownlaodChecked += "C";
                isDownloadedCachedAnswer = true;
                return true;
            }

            // NOW check file system for non-expanded

            // Same logic of download copy filepath
            String keepFilePath = downloadKeepFilePath;
            if (keepFilePath == null) {
                traceDownlaodChecked += "D";
                keepFilePath = generateDownloadFilename(context);
                // keepFile = getDownloadKeepFile(context);
                keepFile = new File(getDownloadKeepDir(context), keepFilePath);
            } else {
                traceDownlaodChecked += "E";
                keepFile = new File(getDownloadKeepDir(context), keepFilePath);
            }

            if (keepFile.exists()) {
                traceDownlaodChecked += "F";
                if (hashSHA256A == null) {
                    traceDownlaodChecked += "G";
                    hashSHA256A = HashFile.hashFileSHA256(keepFile);
                }
                isDownloadedCachedAnswer = true;
                return true;
            }

            traceDownlaodChecked += "H";
            return false;
        }
    }

    public File getDownloadKeepFile(Context context) {
        String tracePathA = "";
        String keepFilePath = downloadKeepFilePath;
        File keepFile;
        if (keepFilePath == null) {
            tracePathA += "A";
            keepFilePath = generateDownloadFilename(context);
            keepFile = new File(getDownloadKeepDir(context), keepFilePath);
        } else {
            tracePathA += "B";
            keepFile = new File(keepFilePath);
        }

        if (! keepFile.exists()) {
            tracePathA += "C";
            keepFile = new File(getDownloadKeepDir(context), keepFilePath.replace(".blorb", ".gblorb"));
            if (! keepFile.exists()) {
                tracePathA += "D";
                keepFile = new File(getDownloadKeepDir(context), keepFilePath.replace(".blorb", ".zblorb"));
            }
        }

        Log.i(TAG, "[StoryDL_Path] " + tracePathA + " " + keepFilePath);
        return keepFile;
    }

    /*
    Incant legacy code which doesn't assume object is created.
     */
    public static boolean isDownloaded(Context context, String name) {
        return getZcodeFile(context, name).exists() || getGlulxFile(context, name).exists();
    }

    @Deprecated
    public boolean isDownloaded(Context context) {
        if (hashSHA256A != null) {
            return true;
        }
        return getZcodeFile(context).exists() || getGlulxFile(context).exists();
    }


    public File getFile(Context context, String file) {
        return new File(getDir(context), file);
    }

    public File getStoryFile(Context context) {
        return isZcode(context) ? getZcodeFile(context) : getGlulxFile(context);
    }

    public File getZcodeFile(Context context) {
        return getZcodeFile(context, getStorageName(context));
    }

    public File getGlulxFile(Context context) {
        return getGlulxFile(context, getStorageName(context));
    }

    public File getSaveFile(Context context) {
        return getSaveFile(context, getStorageName(context));
    }

    public File getCoverImageFile(Context context) {
        return getCoverImageFile(context, getStorageName(context));
    }

    public Bitmap getCoverImageBitmap(Context context) {
        File file = getCoverImageFile(context);
        return file.exists() ? BitmapFactory.decodeFile(file.getPath()) : null;
    }

    public File getMetadataFile(Context context) {
        return getMetadataFile(context, getStorageName(context));
    }

    public File getBlorbFile(Context context) {
        return getBlorbFile(context, getStorageName(context));
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

    private boolean downloadError = false;
    private String downloadErrorDetail = "";
    private boolean downloadingThreadRunning = false;

    public String getDownloadErrorDetail() {
        return downloadErrorDetail;
    }

    public boolean getDownloadError() {
        return downloadError;
    }

    /*
    downloadingNow should be set by caller before calling this method, it will be cleared at end.
    check downloadError after downloadingNow is false.
     */
    public boolean startDownloadThread(final Context context) {
        if (!downloadingThreadRunning) {
            downloadingThreadRunning = true;
            downloadError = false;
            // invalidate cache
            isDownloadedCachedCheck = false;
            final String storyName = Story.this.getName(context);
            Thread downloadThreadA = new Thread() {
                @Override
                public void run() {
                    Log.d(TAG, "[storyDownload] run() " + Thread.currentThread());
                    String error = null;
                    try {
                        if (! download(context)) {
                            Log.w(TAG, "[storyDownload] download_invalid " + storyName);
                            error = context.getString(R.string.download_invalid, storyName);
                        }
                    } catch (Exception e) {
                        Log.wtf(TAG, e);
                        error = context.getString(R.string.download_failed, storyName);
                    }

                    if (error != null) {
                        Log.w(TAG, "[storyDownload] download error " + error);
                        // EventBus to fragment holding story list
                        downloadError = true;
                        downloadErrorDetail = error;
                    }
                    downloadingThreadRunning = false;
                    downloadingNow = false;

                    // invalidate cache
                    isDownloadedCachedCheck = false;

                    // ToDo: another event, as this is LIST download, not Non-List
                    EventBus.getDefault().post(new EventStoryListDownloadResult((error != null), Story.this));
                }
            };
            downloadThreadA.setName("downloadStoryA");
            downloadThreadA.start();
            return true;
        }
        return false;
    }




    public static int MAGIC_FILE_ZIP0 = 0x504b0304;
    public static int MAGIC_FILE_GLULX0 = 0x476c756c;
    public static int MAGIC_FILE_BLORB = 0x464f524d;
    public static int MAGIC_FILE_TADS = 1412640105;

    // ToDo: make setting / preference for user.
    public static boolean keepDownloadFiles = true;

    protected boolean download(Context context, InputStream inputStream) throws IOException {
        boolean downloaded = false;
        // invalidate cache
        isDownloadedCachedCheck = false;

        getDir(context).mkdirs();
        getDownloadKeepDir(context).mkdirs();

        // NOTE: Keep this code in sync with method: generateDownloadFilename
        String fileExtension = "." + StoryHelper.getUsefulFileExtensionFromURL(downloadURL);
        String storyNameSanitized = getStorageName(context);
        String storyNameTotal = "Incant__" + storyNameSanitized + fileExtension;
        File endingRenamedTargetFile = null;
        String keepFilenameTempWithoutExtension = "Incant__" + storyNameSanitized + "__";
        File downloadTargetFile;
        if (!keepDownloadFiles) {
            downloadTargetFile = File.createTempFile(keepFilenameTempWithoutExtension, fileExtension, getDir(context));
        } else {
            downloadTargetFile = new File(getDir(context), storyNameTotal);
        }
        String freshHashA = "";
        try {
            int magic;

            if (inputStream == null) {
                magic = downloadTo(context, downloadURL, downloadTargetFile);
                if (downloadTargetFile.exists()) {
                    // Invalidate the cache
                    isDownloadedCachedCheck = false;
                    freshHashA = getDigestMd5OfFile(downloadTargetFile.toString());
                    Log.d(TAG, "download file freshHashA " + freshHashA);
                }
            } else {
                magic = downloadTo(context, inputStream, downloadTargetFile);
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
                    if (downloadTargetFile.exists()) {
                        freshHashA = getDigestMd5OfFile(downloadTargetFile.toString());
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

            if (magic == MAGIC_FILE_ZIP0) {
                if (zipEntry == null) {
                    Log.w(TAG, "magic says .zip file, but no target provided. unZip failed on file " + downloadTargetFile);
                } else {
                    File tmpEntry = File.createTempFile("tmp", ".zipentry", getDir(context));
                    try {
                        // Replace magic with the internal file magic.
                        magic = unzipTo(context, downloadTargetFile, tmpEntry, zipEntry);
                        // tmpEntry.renameTo(downloadTargetFile);
                        fileExtension = "." + StoryHelper.getUsefulFileExtensionFromURL(zipEntry);
                        switch (fileExtension) {
                            case ".unknown":
                                // We have a case where the files pre-date naming conventions, named '.dat' inside '.zip'
                                if (storyNameSanitized.contains("Zork")) {
                                    Log.d(TAG, "Forcing 'Zork' file extension to 'z5' zip file extract target '" + downloadTargetFile + "' got extension: " + fileExtension + " from zipEntry " + zipEntry + " totalname: " + storyNameTotal);
                                    fileExtension = ".z5";
                                } else if (storyNameSanitized.contains("Plundered")) {
                                    Log.d(TAG, "Forcing 'Plundered' file extension to 'z5' zip file extract target '" + downloadTargetFile + "' got extension: " + fileExtension + " from zipEntry " + zipEntry + " totalname: " + storyNameTotal);
                                    fileExtension = ".z5";
                                } else if (storyNameSanitized.contains("Bureaucracy")) {
                                    Log.d(TAG, "Forcing 'Bureaucracy' file extension to 'z4' zip file extract target '" + downloadTargetFile + "' got extension: " + fileExtension + " from zipEntry " + zipEntry + " totalname: " + storyNameTotal);
                                    fileExtension = ".z4";
                                }

                                break;
                        }
                        storyNameTotal = "Incant__" + storyNameSanitized + fileExtension;
                        Log.d(TAG, "zip file extract target '" + downloadTargetFile + "' got extension: " + fileExtension + " from zipEntry " + zipEntry + " totalname: " + storyNameTotal);

                        // We no longer want the shell, the zip, but the nut inside the shell.
                        downloadTargetFile = tmpEntry;
                    } catch (Exception e) {
                        Log.w(TAG, "unZip failed on file " + downloadTargetFile, e);
                    } finally {
                        if (tmpEntry.exists()) {
                            if (!keepDownloadFiles) {
                                tmpEntry.delete();
                            }
                        }
                    }
                }
            }

            if (magic == MAGIC_FILE_GLULX0) {
                downloadTargetFile.renameTo(getGlulxFile(context));
                downloaded = true;
                endingRenamedTargetFile = getGlulxFile(context);
            } else if ((magic >> 24) >= 3 && (magic >> 24) <= 8) {
                downloadTargetFile.renameTo(getZcodeFile(context));
                downloaded = true;
                endingRenamedTargetFile = getZcodeFile(context);
            } else if (magic == MAGIC_FILE_BLORB) {
                downloadTargetFile.renameTo(getBlorbFile(context));
                endingRenamedTargetFile = getBlorbFile(context);
                Blorb blorb = null;
                try {
                    blorb = Blorb.from(endingRenamedTargetFile);
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

                    int isZCodeGlulxStory = 0;
                    for (Blorb.Resource res : blorb.resources()) {
                        Blorb.Chunk chunk = res.getChunk();
                        if (chunk == null) {
                            continue;
                        }
                        switch (res.getUsage()) {
                        case Blorb.Exec:
                            if (chunk.getId() == Blorb.ZCOD) {
                                isZCodeGlulxStory = 1;
                                writeBlorbChunk(context, chunk, getZcodeFile(context));
                                downloaded = true;
                            } else if (chunk.getId() == Blorb.GLUL) {
                                isZCodeGlulxStory = 2;
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
                    if (keepDownloadFiles) {
                        if (fileExtension.equals(".blorb")) {
                            // Salvage precise extension out of generic blorb?
                            switch (isZCodeGlulxStory) {
                                case 0:
                                    // do nothing, we only know it is blorb
                                    break;
                                case 1:
                                    fileExtension = ".zblorb";
                                    storyNameTotal = "Incant__" + storyNameSanitized + fileExtension;
                                    break;
                                case 2:
                                    fileExtension = ".gblorb";
                                    storyNameTotal = "Incant__" + storyNameSanitized + fileExtension;
                                    break;
                            }
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
                Log.w(TAG, "[storyFileShare] download unmatched magic " + magic);
            }
        } finally {
            if (downloadTargetFile.exists()) {
                if (!keepDownloadFiles) {
                    downloadTargetFile.delete();
                }
            }

            if (downloaded) {
                File keepFile = new File(getDownloadKeepDir(context), storyNameTotal);
                boolean goodFileCopy = FileCopy.copyFile(endingRenamedTargetFile, keepFile);
                if (!goodFileCopy) {
                    Log.e(TAG, "[storyFileShare][mediaStoreScan] file copy failed for duplicate " + downloadTargetFile);
                } else {
                    Log.d(TAG, "[storyFileShare][mediaStoreScan] file copy for duplicate to share " + downloadTargetFile + " to " + keepFile.getPath());
                    // Notify all interested apps that there is a new file to add to their records.
                    Intent shareDownloadIntent = new Intent();
                    // Tell Android to start Thunderword app if not already running.
                    shareDownloadIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                    shareDownloadIntent.setAction("interactivefiction.enginemeta.storydownloaded");
                    shareDownloadIntent.putExtra("sentwhen", System.currentTimeMillis());
                    shareDownloadIntent.putExtra("sender", EchoSpot.sending_APPLICATION_ID);
                    shareDownloadIntent.putExtra("file", keepFile.getPath());
                    context.sendBroadcast(shareDownloadIntent);

                    // Testing on Android 7.1.1 emulator shows that if no providers are installed, they will not see
                    //  files downloaded by Incant when they are later installed.
                    //  Solution is to tell MediaStore to index them
                    MediaScannerConnection.scanFile(
                            context,
                            new String[]{ keepFile.getPath() },
                            null,
                            new MediaScannerConnection.MediaScannerConnectionClient()
                            {
                                @Override
                                public void onMediaScannerConnected()
                                {
                                }
                                @Override
                                public void onScanCompleted(String path, Uri uri)
                                {
                                    Log.d(TAG, "[storyFileShare][mediaStoreScan] onScanCompleted " + path + " uri " + uri.toString());
                                }
                            });

                    // rebuild cache of downloaded filenames
                    rebuildStaticKeepFilesPathPile(context);
                }
            } else {
                Log.w(TAG, "[storyFileShare][mediaStoreScan] downloaded false");
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

    protected int unzipTo(Context context, File zipFile, File file, String zipPayloadDesiredFilename) throws IOException {
        InputStream in = null;
        try {
            ZipFile zf = new ZipFile(zipFile);
            in = zf.getInputStream(zf.getEntry(zipPayloadDesiredFilename));
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
        Log.d(TAG, "downloadTo [downloadURL] " + file + " " + url);
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
        File tmpFile = File.createTempFile("tmp", "tmp" ,getDir(context));
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

    /*
    Deletes a tree one-directory deep?
     */
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

    /*
    MD5 hash
     */
    public String getHash() {
        return hashA;
    }


    private boolean downloadingNow = false;

    public boolean isDownloadingNow() {
        return downloadingNow;
    }

    public void setDownloadingNow(boolean value) {
        downloadingNow = value;
    }

    public boolean isMetaHashPointer() {
        return (hashSHA256A != null);
    }

    public String getStoryHashSHA256() {
        return hashSHA256A;
    }

    public int getEngineCode() {
        return engineCode;
    }

    public void setLikelyDownloadFilename(String keepFilePath) {
        downloadKeepFilePath = keepFilePath;
        Log.i(TAG, "[StoryDL_Path] set to " + keepFilePath);
    }

    public static String foundKeepFilesPathsPileA = null;


    private synchronized void rebuildStaticKeepFilesPathPile(Context context) {
        long startedWhen = System.currentTimeMillis();
        final File downloadKeepDir = Story.getDownloadKeepDir(context);
        int foundCount = 0;

        // Ok, what about doing the same! Build a pile string
        // First line is started with a \n to ensure pattern is total
        StringBuilder foundKeepFilesPathsPile = new StringBuilder("\n");
        File[] downloadKeepFiles = downloadKeepDir.listFiles();
        if (downloadKeepFiles != null) {
            for (int i = 0; i < downloadKeepFiles.length; i++) {
                foundKeepFilesPathsPile.append(downloadKeepFiles[i].getName());
                foundKeepFilesPathsPile.append("\n");
                foundCount++;
            }
        }

        foundKeepFilesPathsPileA = foundKeepFilesPathsPile.toString();
        Log.i(TAG, "[StoryDL_Path] rebuildStaticKeepFilesPathPile took " + (System.currentTimeMillis() - startedWhen) + " found " + foundCount);
    }


    public boolean isExtractedForIncantEngine(Context context) {
        File storyExtractedDir = getDir(context);
        if (storyExtractedDir.isDirectory()) {
            // is extracted, good
            return true;
        }
        return false;
    }

    public boolean prepForIncantEngineLaunch(Context context) {
        File storyExtractedDir = getDir(context);
        if (storyExtractedDir.isDirectory()) {
            // is extracted, good
            return true;
        } else {
            if (keepFile != null) {
                if (keepFile.exists()) {
                    // perform a disk to disk download
                    try {
                        InputStream storyFileInputStream = new FileInputStream(keepFile);
                        download(context, storyFileInputStream);
                    } catch (IOException e) {
                        Log.e(TAG, "keepFile exception", e);
                        return false;
                    }
                    return true;
                }
            }
        }
        return false;
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
            } else {
                // Log.w(TAG, "[xmlScrape] unmatched path " + path);
            }

            if (path.equals("ifindex")) {
                Log.v(TAG, "[xmlScrape] so-far " + toCollected());
            }
        }

        public String toCollected() {
            return "a=" + author + " h=" + headline + " d=" + description + " i=" + ifid + " t=" + title;
        }
    }


    protected void writeMetadata(Context context, File file) throws IOException {
        getDir(context).mkdir();
        File tmpFile = File.createTempFile("tmp", "xmp", getDir(context));
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

    public static String getTimeString(Context context, int recentStringId, int stringId, long time) {
        if (time + 86400000L > System.currentTimeMillis()) {
            return context.getString(recentStringId, time);
        } else {
            return context.getString(stringId, time);
        }
    }
}
