package com.yrek.incant;

import android.content.Context;
import android.util.Log;

import com.wakereality.storyfinding.EventStoryFindingAppError;
import com.wakereality.storyfinding.R;
import com.wakereality.storyfinding.ReadCommaSepValuesFile;
import com.wakereality.storyfinding.StoryEntryIFDB;
import com.yrek.incant.gamelistings.IFArchiveScraper;
import com.yrek.incant.gamelistings.IFDBScraper;
import com.yrek.incant.gamelistings.Scraper;
import com.yrek.incant.gamelistings.StoryHelper;
import com.yrek.runconfig.SettingsCurrent;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StoryLister {
    private static final String TAG = StoryLister.class.getSimpleName();

    private Context context;
    private Scraper[] scrapers;

    public StoryLister(Context context) {
        this.context = context;
        // IFArchive scraping is more primitive, as we have only filenames and some folder-context of category.
        if (SettingsCurrent.getScrapeIFArchive()) {
            this.scrapers = new Scraper[]{
                    new IFDBScraper(context),
                    new IFArchiveScraper(context),
            };
        } else {
            this.scrapers = new Scraper[]{
                    new IFDBScraper(context),
            };
        }
    }


    /*
    Convert the simpler CSV StoryEntryIFDB holding objects to more complex Story objects.
     */
    public void addStoriesCommaSepValuesFile(ArrayList<Story> stories, ReadCommaSepValuesFile readCommaSepValuesFile, Context context) {
        if (readCommaSepValuesFile != null) {
            // No Concurrency lock. If a user rotates screen in the middle of a building of this Array... crash.
            final int csize = readCommaSepValuesFile.foundEntries.size();
            for (int i = 0; i < csize; i++) {
                StoryEntryIFDB ifdbListEntry = readCommaSepValuesFile.foundEntries.get(i);

                if (ifdbListEntry.siteIdentity.equals("35yqdqy3ennlte69")) {
                    Log.e(TAG, "[CSV_matchup][WhereJim][listPopulate] Life on Mars? Index " + i + " title: " + ifdbListEntry.storyTitle);
                }

                if (ifdbListEntry.storyTitle.startsWith("Life")) {
                    Log.e(TAG, "[CSV_matchup][WhereJim][listPopulate] startsWith Life on Mars? Index " + i + " title: " + ifdbListEntry.storyTitle);
                }

                URL downloadLink = null;
                try {
                    downloadLink = new URL(ifdbListEntry.downloadLink);
                } catch (MalformedURLException e) {
                    Log.w(TAG, "Bad URL on CSV list generation ", e);
                }
                URL imageLink = null;
                try {
                    imageLink = new URL(context.getString(R.string.ifdb_cover_image_url, ifdbListEntry.siteIdentity));
                } catch (MalformedURLException e) {
                    Log.w(TAG, "Bad URL for Image on CSV list generation ", e);
                }

                // Story(String name, String author, String headline, String description, URL downloadURL, String zipEntry, URL imageURL)
                Story newStory = new Story(ifdbListEntry.storyTitle, ifdbListEntry.storyAuthor, ifdbListEntry.storyWhimsy, ifdbListEntry.storyDescription, downloadLink, null /* not zip */, imageLink);

                switch (ifdbListEntry.storyLanguage) {
                    case "":
                    case "en":
                        // do nothing, default English
                        break;
                    default:
                        newStory.setLanguageIdentifier(ifdbListEntry.storyLanguage);
                        break;
                }

                StoryHelper.addStory(context, newStory, stories, 1000);

                // Scraper.writeStory();
                // writeStory(out, name, author, extraURL, zipFile, context.getString(R.string.ifdb_cover_image_url, currentStoryID[0]));
            }
        }
    }

    public List<Story> filterAndSortStories(ArrayList<Story> stories, Comparator<Story> sort, Context context) throws IOException {
        int skipCount = 0;
        if (SettingsCurrent.getStoryListFilterOnlyNotDownloaded() > 0) {
            ArrayList<Story> freshList = new ArrayList<>();

            boolean desiredValue = false;
            if (SettingsCurrent.getStoryListFilterOnlyNotDownloaded() == 2) {
                desiredValue = true;
            }

            for (int i = 0; i < stories.size(); i++) {
                final Story singleStroy = stories.get(i);
                if (singleStroy.isDownloadedExtensiveCheck(context) == desiredValue) {
                    freshList.add(singleStroy);
                    Log.d(TAG, "[listPopulate][foundDownloaded] NOT downloaded " + singleStroy.keepFile + " isDownloadedCachedAnswer " + singleStroy.isDownloadedCachedAnswer + " trace " + singleStroy.traceDownlaodChecked);
                } else {
                    skipCount++;
                }
            }

            stories = freshList;
        }

        if (sort != null) {
            try {
                Collections.sort(stories, sort);
            } catch (Exception e0) {
                Log.e(TAG, "Exception sorting", e0);
                EventBus.getDefault().post(new EventStoryFindingAppError("SL", 1, 10, "Exception Collections.sort", ""));
            }
        }

        Log.d(TAG, "[listPopulate] getStories final stories " + stories.size() + " skipCount (downloaded) " + skipCount);
        return stories;
    }


    public void scrape() throws IOException {
        for (Scraper scraper : scrapers) {
            try {
                scraper.scrape();
            }
            catch (Exception e0)
            {
                Log.w(TAG, "Scraper ", e0);
            }
        }
    }


    public final Comparator<Story> SortByDefault = new Comparator<Story>() {

        @Override public int compare(Story story1, Story story2) {
            final String storyName1 = story1.getName(context);

            if (story1 == story2) {
                //Log.v(TAG, "[sortName] equalA " + storyName1);
                return 0;
            }

            if (story1.isDownloadedExtensiveCheck(context)) {
                if (! story2.isDownloadedExtensiveCheck(context)) {
                    // If first is downloaded and second is not push second to bottom.
                    Log.v(TAG, "[sortName] downloadA " + storyName1);
                    return -1;
                }

                // Stories with save files go highest, most recent saved first.
                // ToDo: last played needs to be trackec on it's own, for Thunderword integration. iFrotz has nice logic to resume in-progress game.
                if (story1.getSaveFile(context).exists()) {
                    if (!story2.getSaveFile(context).exists()) {
                        //Log.v(TAG, "[sortName] saveA " + storyName1);
                        return -1;
                    } else {
                        //Log.v(TAG, "[sortName] saveB " + storyName1);
                        return - (int) (story1.getSaveFile(context).lastModified() - story2.getSaveFile(context).lastModified());
                    }
                } else if (story2.getSaveFile(context).exists()) {
                    //Log.v(TAG, "[sortName] saveC " + storyName1);
                    return 1;
                }

                //Log.v(TAG, "[sortName] lastModified? " + storyName1);
                return (int) (story1.getStoryFile(context).lastModified() - story2.getStoryFile(context).lastModified());
            } else if (story2.isDownloadedExtensiveCheck(context)) {
                // Downloaded go before non-downloaded.
                //Log.v(TAG, "[sortName] story2 downloaded " + storyName1);
                return 1;
            } else {
                // If neither one is downloaded or both are downloaded, they are on the same equality of sorting by name.
                //final String storyName1 = story1.getName(context);
                final String storyName2 = story2.getName(context);
                //Log.v(TAG, "[sortName] " + storyName1 + " : " + storyName2);
                return storyName1.compareToIgnoreCase(storyName2);
            }
        }

        @Override public boolean equals(Object object) {
            return this == object;
        }
    };

    public static int addDownloadRunIndex = 2;

    /*
    Previously downloaded story files in Incant app legacy expanded structure
     */
    public void addDownloadedStories(ArrayList<Story> stories) throws IOException {
        File[] primaryDirectoryFiles = Story.getRootDir(context).listFiles();
        if (primaryDirectoryFiles == null)
        {
            Log.w(TAG, "[listPopulate] primaryDirectoryFiles is null");
            return;
        }

        addDownloadRunIndex++;
        for (File file : primaryDirectoryFiles) {
// ToDo: using the filename as a direct keyname? This likely needs rework for "Life on Mars?"
            if (! Story.isDownloaded(context, file.getName())) {
                Log.i(TAG, "[listPopulate] addDownloaded SKIP file " + file);
                continue;
            }
            Log.d(TAG, "[listPopulate] SPOT_AAA0 addDownloaded story " + file + " addDownloadRunIndex " + addDownloadRunIndex);
            StoryHelper.addStory(context, new Story(file.getName(), null, null, null, null, null, null), stories, addDownloadRunIndex);
        }
    }

    public void addInitialStories(ArrayList<Story> stories) throws IOException {
        String[] initial = context.getResources().getStringArray(R.array.initial_story_list);
        // 7 lines of array per entry.
        for (int i = 0; i + 6 < initial.length; i += 7) {
            StoryHelper.addStory(context, new Story(initial[i], initial[i+1], initial[i+2], initial[i+3], new URL(initial[i+4]), initial[i+5].length() > 0 ? initial[i+5] : null, initial[i+6].length() > 0 ? new URL(initial[i+6]) : null), stories, 1);
        }
    }


    public ArrayList<Story> generateStoriesListAllSortedArrayListA(ArrayList<Story> stories) {
        if (stories == null) {
            stories = new ArrayList<Story>();
        }
        Log.d(TAG, "[listPopulate] startingList " + stories.size());

        try {
            // Find downloaded (expanded Incant ap format) stories
            addDownloadedStories(stories);
            Log.d(TAG, "[listPopulate] getStories addDownloaded " + stories.size());

            if (!SettingsCurrent.getListingShowLocal()) {
                // Featured download links
                addInitialStories(stories);
                Log.d(TAG, "[listPopulate] getStories addInitialStories " + stories.size());
            }

            // CSV of Inform 7 files
            StoryListSpot.storyLister.addStoriesCommaSepValuesFile(stories, StoryListSpot.readCommaSepValuesFile, context);

            // XML live (and cached) scraping of IFDB and ifarchive
            if (! SettingsCurrent.getListingShowLocal()) {
                for (Scraper scraper : scrapers) {
                    addDownloadRunIndex++;
                    scraper.addStories(stories, addDownloadRunIndex);
                    Log.d(TAG, "[listPopulate] getStories scraper " + stories.size() + " addDownloadRunIndex " + addDownloadRunIndex);
                }
            }

            stories = (ArrayList<Story>) filterAndSortStories(stories, StoryListSpot.storyLister.SortByDefault, context);
            Log.d(TAG, "[listPopulate] getStories post-sort " + stories.size());
        } catch (IOException e) {
            Log.e(TAG, "[listPopulate] IOException generating stories list", e);
        }

        return stories;
    }
}
