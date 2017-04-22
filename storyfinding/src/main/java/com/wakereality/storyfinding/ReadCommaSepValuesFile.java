package com.wakereality.storyfinding;

import android.content.Context;
import android.util.Log;


import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Stephen A Gutknecht on 4/1/17.
 * on Linux, this produces CSV file that has all CRLF intact and proper escaping
 *    mysql --login-path=wakedev -ss --database=ifdbarchive --batch --skip-column-names -e "SELECT * FROM games WHERE system LIKE '%Inform%' OR system LIKE '%Supergl%' OR system LIKE '%ZETA%' OR system = 'ZIL';" | awk -F'\t' '{ sep=""; for(i = 1; i <= NF; i++) { gsub(/\\t/,"\t",$i); gsub(/\\n/,"\n",$i); gsub(/\%/,"__PERCENT__",$i); gsub(/\\\\/,"\\",$i); gsub(/"/,"\"\"",$i); printf sep"\""$i"\""; sep=","; if(i==NF){printf"\n"}}}' > ifdb_inform_list0.csv
 * awk tip here:
 *    http://stackoverflow.com/questions/15640287/change-output-format-for-mysql-command-line-results-to-csv/17910254#17910254
 *
 * An quick way to view the table structures
 *    cat ifdb-archive.sql | less -S
 *
 * Star ratings:
 *    mysql --login-path=wakedev -ss --database=ifdbarchive --batch --skip-column-names -e "SELECT a.id, AVG(rating) AS AVGrating, COUNT(rating) AS COUNTrating FROM games AS a INNER JOIN reviews AS r ON a.id = r.gameid  GROUP BY id HAVING COUNTrating > 2 ORDER BY AVGrating DESC;" |  sed 's/\t/,/g' > ifdb_rating_stars_list0.csv
 *
 * Download links for Inform stories:
 *    mysql --login-path=wakedev -ss --database=ifdbarchive --batch --skip-column-names -e "SELECT id, url, displayorder,fmtid,osid,compression,compressedprimary FROM games AS a INNER JOIN gamelinks AS r ON a.id = r.gameid WHERE a.system LIKE '%Inform%' ORDER BY a.id, displayorder ASC;" |  sed 's/\t/,/g' > ifdb_inform_downloads_list0.csv
 * Filter that list
 *    grep -i '\.gblorb\|\.z[1-8],\|\.zblorb\|\.blb\|\.zlb\|\.glb\|\.ulx\|\.blorb' ifdb_inform_downloads_list0.csv > ifdb_inform_downloads_list0_filtered.csv
 * Filter that list a second pass, removing live website play links
 *    grep -i -v 'iplayif\.com\|parchment.full.html\|play-remote\.html' ifdb_inform_downloads_list0_filtered.csv > ifdb_inform_downloads_list0_filtered_A.csv
 *
 * Do a second pass to pick up zip files that also contain desired extensions:
 *    grep -i '\.zip' ifdb_inform_downloads_list0.csv | grep '\.gblorb\|\.z[1-8]\|\.zblorb\|\.blb\|\.zlb\|\.glb\|\.ulx\|\.blorb' > ifdb_inform_downloads_list0_filtered_in_zip.csv
 *
 * ToDo:  there needs to be a step to filter parchment links, html links, etc. Study the ones removed from various GitHub checkins to get clues of what was done incorrectly.
 * ToDo:  Right now it isn't using the index off the download to favor the newest, an example to study is curses-r12.z5
 *
 * desire to add from runtime:
 *    sha256 of file, file size, engine code, artwork filename (zip download of all artwork, filename to sha256?), language, int code to quirks (special engine requirement), adult rating
 *
 */

public class ReadCommaSepValuesFile {

    public static final String TAG = "ReadCSVFile";

    public static String rc(final String inString) {
        return inString.replace(",", "&");
    }

    public final ArrayList<StoryEntryIFDB> foundEntries = new ArrayList<>();

    // example: "2017-03-11 15:22:16"
    SimpleDateFormat dateFormatIFDB0 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    public boolean readComplexSetOfFilesCSV(Context context) {
        Log.i("ReadCSV", "[ReadCSV] start totalMemory " + Runtime.getRuntime().totalMemory());

        long isRecentDateWhen = System.currentTimeMillis();
        try {
            isRecentDateWhen = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("2016-11-15").getTime();
        } catch (ParseException e) {
            Log.e(TAG, "[ReadCSVfile] exception recent date", e);
        }

        foundEntries.clear();

        String next[] = {};
        final List<String[]> informStoriesList = new ArrayList<String[]>();
        String fileSource = "csv/ifdb_inform_list0.csv";
        try {
            final CSVReader reader = new CSVReader(new InputStreamReader(context.getAssets().open(fileSource)));
            for(;;) {
                next = reader.readNext();
                if (next != null) {
                    informStoriesList.add(next);
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "[ReadCSVfile] exception file " + fileSource, e);
            return false;
        }


        final List<String[]> ratingsStars = new ArrayList<String[]>();
        fileSource = "csv/ifdb_rating_stars_list0.csv";
        try {
            final CSVReader reader = new CSVReader(new InputStreamReader(context.getAssets().open(fileSource)));
            for(;;) {
                next = reader.readNext();
                if (next != null) {
                    ratingsStars.add(next);
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "[ReadCSVfile] exception file " + fileSource, e);
            return false;
        }

        final List<String[]> downloadLinks = new ArrayList<String[]>();
        fileSource = "csv/ifdb_inform_downloads_list0_filtered_A.csv";
        try {
            final CSVReader reader = new CSVReader(new InputStreamReader(context.getAssets().open(fileSource)));
            for(;;) {
                next = reader.readNext();
                if (next != null) {
                    downloadLinks.add(next);
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "[ReadCSVfile] exception file " + fileSource, e);
            return false;
        }

        int targetMatch = 0;
        String previousEntry = "never_match";
        Log.i("ReadCSV", "[ReadCSV] got " + informStoriesList.size());
        for (int i = 0; i < informStoriesList.size(); i++) {
            final String[] e = informStoriesList.get(i);

            for (int s = 0; s < ratingsStars.size(); s++) {
                final String[] r = ratingsStars.get(s);
                if (r[0].equals(e[0])) {
                    if (Float.valueOf(r[1]) > 3.0f) {

                        for (int d = 0; d < downloadLinks.size(); d++) {
                            final String[] l = downloadLinks.get(d);
                            if (l[0].equals(e[0])) {
                                // For now, filter out zip, there is another csv if we want to delve deeper into embedded files.
                                // Using contains instead of endswith as sometimes there are URL parameters.
                                if (! l[1].toLowerCase(Locale.US).contains(".zip")) {
                                    // Multiple download links for the same story
                                    // ToDo: it is likely that this logic is pulling the OLDEST, the west link, and revisions of the same story may be skipped.
                                    //   Rework, reverse sorting in the SQL SELECT ORDER BY?
                                    if (! e[0].equals(previousEntry)) {

                                        /*
25 fields
"gf9agvi3a39ub3z6","A travers la forêt","Julien Frison","NULL","TRAVERS LA FORêT, A","FRISON, JULIEN","French,GPL,I7 source available","2013-11-20 01:00:00","1","GPL","Inform 7","fr","Fiction interactive (jeu) à l'inspiration oscillant entre Tolkien et D&D","NULL","NULL","NULL","NULL","NULL","NULL","https://github.com/jfrison/A-travers-la-foret.inform","Found on GitHub but not listed here","2017-03-06 20:46:02","7gfpmfp63105d53g","2017-03-11 15:22:16","2"
why no date on output of this one?
"cg4j40i7wq34ggo1","Not Just An Ordinary Ballerina","Jim Aikin","Jim Aikin {2qrzwolh24lwg44w}","NOT JUST AN ORDINARY BALLERINA","AIKIN, JIM","Xyzzy Awards 1999,adaptive hints,built-in hints,character graphics,female protagonist,guided maze,cover art,Christmas,Holiday Theme,parser,cow,milk","1999-01-01 00:00:00","1","Freeware","Inform 6","en","NULL","http://ifdb.tads.org/viewgame?coverart&id=cg4j40i7wq34ggo1","NULL","NULL","Seasonal","Nasty","490","NULL","NULL","2007-09-29 00:00:00","3wcnay8ie4ajtyrm","2015-06-19 22:45:31","6"
                                         */

                                        previousEntry = e[0];
                                        targetMatch++;
                                        final StoryEntryIFDB storyEntry = new StoryEntryIFDB();
                                        storyEntry.downloadLink = l[1];
                                        if (! l[6].equals("NULL")) {
                                            // If the zip-inside file is populated, this is probably an undesired link.
                                            Log.w("ReadCSV", "[ReadCSV] SKIP as likely a zip file l[1] " + l[1] + " l[6] " + l[6]);
                                            storyEntry.extractFilename = l[6];
                                            continue;
                                        }
                                        storyEntry.siteIdentity = e[0];
                                        storyEntry.rating = Float.valueOf(r[1]);
                                        storyEntry.storyTitle = e[1].trim();
                                        storyEntry.storyAuthor = e[2].trim();
                                        if (e[12].equals("NULL")) {
                                            storyEntry.storyDescription = "";
                                        } else {
                                            storyEntry.storyDescription = e[12].trim();
                                        }
                                        if (e[16].equals("NULL")) {
                                            storyEntry.storyWhimsy = "";
                                        } else {
                                            storyEntry.storyWhimsy = e[16].trim();
                                        }
                                        // The first posting date
                                        String dateFieldA = e[21];
                                        if (dateFieldA.isEmpty()) {
                                            // Missing create date, try recently edited date
                                            dateFieldA = e[23];
                                        }
                                        if (dateFieldA.contains("-")) {
                                            try {
                                                storyEntry.listingWhen = dateFormatIFDB0.parse(dateFieldA).getTime();
                                                if (storyEntry.listingWhen >= isRecentDateWhen) {
                                                    storyEntry.tickeBits = 1;
                                                }
                                                final long listingElapsed = System.currentTimeMillis() - storyEntry.listingWhen;
                                                if (listingElapsed < (1000L * 60L * 60L * 24L * 90L)) {
                                                    storyEntry.tickeBits += 2;
                                                    Log.w("ReadCSV", "[ReadCSV] recent date " + dateFieldA + " elapsed " + listingElapsed);
                                                } else {
                                                    // Log.w("ReadCSV", "[ReadCSV] NOT recent date " + dateFieldA + " elapsed " + listingElapsed);
                                                }
                                            } catch (ParseException e1) {
                                                Log.w("ReadCSV", "[ReadCSV] problem with date ", e1);
                                            }
                                        } else {
                                            Log.w("ReadCSV", "[ReadCSV] problem with date, no dash? " + dateFieldA);
                                        }

                                        if (storyEntry.listingWhen == 0L) {
                                            Log.w("ReadCSV", "[ReadCSV] why 0L listingWhen? " + e[0] + ", no dash? " + e[21] + ", " + e[23]);
                                        }

                                        foundEntries.add(storyEntry);
                                        Log.i("ReadCSV", "[ReadCSV] RATING (" + r[1] + "/" + r[2] + ") # " + i + ": " + storyEntry.toString());
                                    }

                                }
                            }
                        }

                    }
                }
            }

            if (i % 100 == 0) {
                Log.v("ReadCSV", "[ReadCSV] # " + i + ": " + rc(e[1]) + ", " + rc(e[2]));
            }
        }

        // Sort by title
        Collections.sort(foundEntries, new Comparator<StoryEntryIFDB>() {
            @Override
            public int compare(StoryEntryIFDB story1, StoryEntryIFDB story2)
            {
                return story1.storyTitle.toLowerCase(Locale.US).compareTo(story2.storyTitle.toLowerCase(Locale.US));
            }
        });

        // save copy on dev system once in a white.
        saveCopyAsCSV(context, "/sdcard/Incant_saveList.csv");

        Log.i("ReadCSV", "[ReadCSV] targetMatch " + targetMatch + " totalMemory " + Runtime.getRuntime().totalMemory());
        return true;
    }


   // 1. fix zork filenames from unknown
   // 2. sha256 matchup

    public boolean readSimpleFileOneObjectCSV(Context context) {
        Log.i("ReadCSV", "[ReadCSV] start totalMemory " + Runtime.getRuntime().totalMemory());

        foundEntries.clear();

        String next[] = {};
        List<String[]> informStoriesList = new ArrayList<String[]>();
        try {
            String fileSource = "csv/Incant_saveList.csv";
            CSVReader reader = new CSVReader(new InputStreamReader(context.getAssets().open(fileSource)));
            for ( ; ; ) {
                next = reader.readNext();
                if (next != null) {
                    informStoriesList.add(next);
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            Log.e("ReadCSV", "[ReadCSV] IOException", e);
            return false;
        }

        Log.i("ReadCSV", "[ReadCSV] got " + informStoriesList.size());
        for (int i = 0; i < informStoriesList.size(); i++) {
            final String[] e = informStoriesList.get(i);

            final StoryEntryIFDB storyEntry = new StoryEntryIFDB();
            storyEntry.siteIdentity = e[0];
            storyEntry.rating = Float.valueOf(e[1]);
            storyEntry.storyTitle = e[2];
            storyEntry.storyAuthor = e[3];
            storyEntry.storyWhimsy = e[4];
            storyEntry.downloadLink = e[5];
            storyEntry.storyDescription = e[6];
            foundEntries.add(storyEntry);
        }

        Log.i("ReadCSV", "[ReadCSV] foundEntries " + foundEntries.size() + " totalMemory " + Runtime.getRuntime().totalMemory());
        return true;
    }


    // ToDo:
    // The saved copy can be read in and be a way to save RAM and CPU usage. Further, SHA-256 cross-reference and image-addition can be introduced for stories that the main image URL is missing
    //   ideally users can see images before even downloading.
    public boolean saveCopyAsCSV(Context context, String filePath) {
        File deleteFile = new File(filePath);
        deleteFile.delete();

        CSVWriter writer;
        try {
            writer = new CSVWriter(new FileWriter(filePath, true));
        } catch (IOException e) {
            Log.e("ReadCSV", "[ReadCSV] Exception saving copy of data to CSV", e);
            return false;
        }

        for (int i = 0; i < foundEntries.size(); i++) {
            StoryEntryIFDB ifdbListEntry = foundEntries.get(i);
            writer.writeNext(ifdbListEntry.toStringArray());
        }
        try {
            writer.close();
            return true;
        } catch (IOException e) {
            Log.e("ReadCSV", "[ReadCSV] IOException", e);
            return false;
        }
    }
}
