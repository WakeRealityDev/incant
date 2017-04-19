package com.wakereality.storyfinding;

import android.content.Context;
import android.util.Log;


import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Created by Stephen A Gutknecht on 4/1/17.
 * on Linux, this produces CSV file that has all CRLF intact and proper escaping
 *    mysql --login-path=wakedev -ss --database=ifdbarchive --batch --skip-column-names -e "SELECT * FROM games WHERE system LIKE '%Inform%' OR system LIKE '%Supergl%' OR system LIKE '%ZETA%';" | awk -F'\t' '{ sep=""; for(i = 1; i <= NF; i++) { gsub(/\\t/,"\t",$i); gsub(/\\n/,"\n",$i); gsub(/\%/,"__PERCENT__",$i); gsub(/\\\\/,"\\",$i); gsub(/"/,"\"\"",$i); printf sep"\""$i"\""; sep=","; if(i==NF){printf"\n"}}}' > ifdb_inform_list0.csv
 * awk tip here:
 *    http://stackoverflow.com/questions/15640287/change-output-format-for-mysql-command-line-results-to-csv/17910254#17910254
 *
 * An quick way to view the table structures
 *    cat ifdb-archive.sql | less -S
 *
 * Star ratings:
 *    mysql --login-path=wakedev -ss --database=ifdbarchive --batch --skip-column-names -e "SELECT a.id, AVG(rating) AS AVGrating, COUNT(rating) AS COUNTrating FROM games AS a INNER JOIN reviews AS r ON a.id = r.gameid  GROUP BY id HAVING COUNTrating > 2 ORDER BY AVGrating DESC;" |  sed 's/\t/,/g' > ifdb_stars_list0.csv
 *
 * Download links for Inform stories:
 *    mysql --login-path=wakedev -ss --database=ifdbarchive --batch --skip-column-names -e "SELECT id, url, displayorder,fmtid,osid,compression,compressedprimary FROM games AS a INNER JOIN gamelinks AS r ON a.id = r.gameid WHERE a.system LIKE '%Inform%';" |  sed 's/\t/,/g' > ifdb_inform_downloads_list0.csv
 * Filter that list
 *    grep '\.gblorb\|\.z[1-8],\|\.zblorb\|\.blb\|\.zlb\|\.glb\|\.ulx\|\.blorb' ifdb_inform_downloads_list0.csv
 * Do a second pass to pick up zip files that also contain desired extensions:
 *    grep '\.zip' ifdb_inform_downloads_list0.csv | grep '\.gblorb\|\.z[1-8]\|\.zblorb\|\.blb\|\.zlb\|\.glb\|\.ulx\|\.blorb' > ifdb_inform_downloads_list0_filtered_in_zip.csv
 *
 * ToDo:  there needs to be a step to filter parchment links, html links, etc. Study the ones removed from various GitHub checkins to get clues of what was done incorrectly.
 * ToDo:  Right now it isn't using the index off the download to favor the newest, an example to study is curses-r12.z5
 *
 */

public class ReadCommaSepValuesFile {

    public static String rc(final String inString) {
        return inString.replace(",", "&");
    }

    public final ArrayList<StoryEntryIFDB> foundEntries = new ArrayList<>();

    public boolean readComplexSetOfFilesCSV(Context context) {
        Log.i("ReadCSV", "[ReadCSV] start totalMemory " + Runtime.getRuntime().totalMemory());

        foundEntries.clear();

        String next[] = {};
        List<String[]> informStoriesList = new ArrayList<String[]>();
        try {
            String fileSource = "csv/ifdb_inform_list0.csv";
            CSVReader reader = new CSVReader(new InputStreamReader(context.getAssets().open(fileSource)));
            for(;;) {
                next = reader.readNext();
                if (next != null) {
                    informStoriesList.add(next);
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }


        List<String[]> ratingsStars = new ArrayList<String[]>();
        try {
            String fileSource = "csv/ifdb_rating_stars_list0.csv";
            CSVReader reader = new CSVReader(new InputStreamReader(context.getAssets().open(fileSource)));
            for(;;) {
                next = reader.readNext();
                if (next != null) {
                    ratingsStars.add(next);
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        List<String[]> downloadLinks = new ArrayList<String[]>();
        try {
            String fileSource = "csv/ifdb_inform_downloads_list0_filtered.csv";
            CSVReader reader = new CSVReader(new InputStreamReader(context.getAssets().open(fileSource)));
            for(;;) {
                next = reader.readNext();
                if (next != null) {
                    downloadLinks.add(next);
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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
                                        previousEntry = e[0];
                                        targetMatch++;
                                        final StoryEntryIFDB storyEntry = new StoryEntryIFDB();
                                        storyEntry.downloadLink = l[1];
                                        storyEntry.siteIdentity = e[0];
                                        storyEntry.rating = Float.valueOf(r[1]);
                                        storyEntry.storyTitle = e[1];
                                        storyEntry.storyAuthor = e[2];
                                        if (e[12].equals("NULL")) {
                                            storyEntry.storyDescription = "";
                                        } else {
                                            storyEntry.storyDescription = e[12];
                                        }
                                        if (e[16].equals("NULL")) {
                                            storyEntry.storyWhimsy = "";
                                        } else {
                                            storyEntry.storyWhimsy = e[16];
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
        // saveCopyAsCSV(context);

        Log.i("ReadCSV", "[ReadCSV] targetMatch " + targetMatch + " totalMemory " + Runtime.getRuntime().totalMemory());
        return true;
    }


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
    public boolean saveCopyAsCSV(Context context) {
        CSVWriter writer;
        try {
            writer = new CSVWriter(new FileWriter("/sdcard/Incant_saveList.csv", true));
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
