package com.yrek.incant;

import android.content.Context;
import android.util.Log;

import com.wakereality.storyfinding.R;
import com.wakereality.storyfinding.ReadCommaSepValuesFile;
import com.wakereality.storyfinding.StoryEntryIFDB;
import com.yrek.incant.gamelistings.IFArchiveScraper;
import com.yrek.incant.gamelistings.IFDBScraper;
import com.yrek.incant.gamelistings.Scraper;
import com.yrek.incant.gamelistings.StoryHelper;
import com.yrek.runconfig.SettingsCurrent;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class StoryLister {
    private static final String TAG = StoryLister.class.getSimpleName();

    private Context context;
    private Scraper[] scrapers;

    StoryLister(Context context) {
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

    public List<Story> getStories(Comparator<Story> sort, ReadCommaSepValuesFile readCommaSepValuesFile, Context context) throws IOException {
        ArrayList<Story> stories = new ArrayList<Story>();
        addDownloaded(stories);
        Log.d(TAG, "[listPopulate] getStories addDownloaded " + stories.size());

        if (! SettingsCurrent.getListingShowLocal()) {
            // Featured download links
            addInitial(stories);
            Log.d(TAG, "[listPopulate] getStories addInitial " + stories.size());

            for (Scraper scraper : scrapers) {
                addDownloadRunIndex++;
                scraper.addStories(stories, addDownloadRunIndex);
                Log.d(TAG, "[listPopulate] getStories scraper " + stories.size());
            }
        }

        if (readCommaSepValuesFile != null) {
            // No Concurrency lock. If a user rotates screen in the middle of a building of this Array... crash.
            for (int i = 0; i < readCommaSepValuesFile.foundEntries.size(); i++) {
                StoryEntryIFDB ifdbListEntry = readCommaSepValuesFile.foundEntries.get(i);

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
                StoryHelper.addStory(context, newStory, stories, 1000);

                // Scraper.writeStory();
                // writeStory(out, name, author, extraURL, zipFile, context.getString(R.string.ifdb_cover_image_url, currentStoryID[0]));
            }
        }

        if (SettingsCurrent.getStoryListFilterOnlyNotDownloaded()) {
            ArrayList<Story> freshList = new ArrayList<>();
            for (int i = 0; i < stories.size(); i++) {
                if (! stories.get(i).isDownloaded(context)) {
                    freshList.add(stories.get(i));
                }
            }
            stories = freshList;
        }

        if (sort != null) {
            Collections.sort(stories, sort);
        }

        Log.d(TAG, "[listPopulate] getStories final stories " + stories.size());
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
            if (story1 == story2) {
                return 0;
            }
            if (story1.isDownloaded(context)) {
                if (!story2.isDownloaded(context)) {
                    return -1;
                }
                if (story1.getSaveFile(context).exists()) {
                    if (!story2.getSaveFile(context).exists()) {
                        return -1;
                    } else {
                        return - (int) (story1.getSaveFile(context).lastModified() - story2.getSaveFile(context).lastModified());
                    }
                } else if (story2.getSaveFile(context).exists()) {
                    return 1;
                }
                return (int) (story1.getStoryFile(context).lastModified() - story2.getStoryFile(context).lastModified());
            } else if (story2.isDownloaded(context)) {
                return 1;
            } else {
                return story1.getName(context).compareTo(story2.getName(context));
            }
        }
        @Override public boolean equals(Object object) {
            return this == object;
        }
    };

    public static int addDownloadRunIndex = 2;

    /*
    Previously downloaded story files
     */
    private void addDownloaded(ArrayList<Story> stories) throws IOException {
        File[] primaryDirectoryFiles = Story.getRootDir(context).listFiles();
        if (primaryDirectoryFiles == null)
        {
            Log.w(TAG, "primaryDirectoryFiles is null");
            return;
        }

        addDownloadRunIndex++;
        for (File file : primaryDirectoryFiles) {
            if (! Story.isDownloaded(context, file.getName())) {
                Log.i(TAG, "addDownloaded SKIP file " + file);
                continue;
            }
            Log.d(TAG, "SPOT_AAA0 addDownloaded story " + file + " addDownloadRunIndex " + addDownloadRunIndex);
            StoryHelper.addStory(context, new Story(file.getName(), null, null, null, null, null, null), stories, addDownloadRunIndex);
        }
    }

    private void addInitial(ArrayList<Story> stories) throws IOException {
        String[] initial = context.getResources().getStringArray(R.array.initial_story_list);
        for (int i = 0; i + 6 < initial.length; i += 7) {
            StoryHelper.addStory(context, new Story(initial[i], initial[i+1], initial[i+2], initial[i+3], new URL(initial[i+4]), initial[i+5].length() > 0 ? initial[i+5] : null, initial[i+6].length() > 0 ? new URL(initial[i+6]) : null), stories, 1);
        }
    }
}
