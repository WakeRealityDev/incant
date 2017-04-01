package com.yrek.incant.gamelistings;

import android.content.Context;
import android.util.Log;

import com.yrek.incant.R;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IFDBScraper extends Scraper {
    private final String[] scrapeURLs;

    public IFDBScraper(Context context) {
        super(context, CACHE_IFDB, context.getResources().getInteger(R.integer.ifdb_cache_timeout));
        this.scrapeURLs = context.getResources().getStringArray(R.array.ifdb_scrape_urls);
    }

    private final Pattern listPattern = Pattern.compile("\\<a href=\"viewgame\\?id=([a-zA-Z0-9]+)\"\\>");

    @Override
    void scrape(final DataOutputStream out) throws IOException {
        final HashSet<String> storyIDs = new HashSet<String>();
        final String[] currentStoryID = new String[1];
        PageScraper listScraper = new PageScraper() {
            @Override public void scrape(String line) throws IOException {
                Matcher m = listPattern.matcher(line);
                while (m.find()) {
                    String freshGroup = m.group(1);
                    // regex parsing is returning duplicates
                    if (! storyIDs.contains(freshGroup)) {
                        Log.i(TAG, "freshGroup " + freshGroup);
                        storyIDs.add(freshGroup);
                    }
                }
            }
        };

        for (String scrapeURL : scrapeURLs) {
            Log.i(TAG, "StoryLister:scrapeURL " + scrapeURL);
            scrapeURL(scrapeURL, listScraper);
        }

        Log.d(TAG,"IFDBScraper:storyIDs="+storyIDs);
        try {
            XMLScraper xmlScraper = new XMLScraper(new XMLScraper.Handler() {
                String name;
                String author;
                String url;
                String extraURL;
                String zipFile;
                String format;
                String stars;

                @Override public void startDocument() {
                    name = null;
                    author = null;
                    url = null;
                    extraURL = null;
                    zipFile = null;
                    format = null;
                    stars = null;
                }

                @Override public void endDocument() {
                    Log.d(TAG,"[IFDBScraper][cacheFile] name="+name+",author="+author+",url="+url+",extraURL="+extraURL+",zipFile="+zipFile+",format="+format+",stars="+stars);
                    try {
                        if (name == null) {
                            Log.d(TAG, "[IFDBScraper][skipScrape] name=null url " + url);
                        } else if (url != null && url.matches(patternMatchA)) {
                            writeStory(out, name, author, url, null, context.getString(R.string.ifdb_cover_image_url, currentStoryID[0]));
                        } else if (zipFile != null && zipFile.matches(patternMatchA)) {
                            writeStory(out, name, author, extraURL, zipFile, context.getString(R.string.ifdb_cover_image_url, currentStoryID[0]));
                        }
                        else
                        {
                            Log.d(TAG, "[IFDBScraper][skipScrape] url " + url);
                        }
                    } catch (Exception e) {
                        Log.wtf(TAG,e);
                    }
                }

                public static final String patternMatchA = ".*\\.(z[1-8]|zblorb|zlb|ulx|blorb|blb|gblorb|glb)";

                @Override public void element(String path, String value) {
                    if (value.trim().length() == 0) {
                        // do nothing
                    } else if ("autoinstall/title".equals(path)) {
                        name = value;
                    } else if ("autoinstall/author".equals(path)) {
                        author = value;
                    } else if ("autoinstall/download/game/href".equals(path)) {
                        url = value;
                    } else if ("autoinstall/download/game/format/id".equals(path)) {
                        format = value;
                    } else if ("autoinstall/download/game/compression/primaryfile".equals(path)) {
                        if (url != null && url.matches(".*\\.(zip|ZIP)") && value.matches(patternMatchA)) {
                            extraURL = url;
                            zipFile = value;
                        }
                    } else if ("autoinstall/download/extra/href".equals(path)) {
                        if (url == null && value.matches(patternMatchA)) {
                            url = value;
                        } else if (zipFile == null) {
                            extraURL = value;
                        }
                    } else if ("autoinstall/download/extra/compression/primaryfile".equals(path)) {
                        if (extraURL != null && extraURL.matches(".*\\.(zip|ZIP)") && value.matches(patternMatchA)) {
                            zipFile = value;
                        }
                    }
                }
            });

            for (String storyID : storyIDs) {
                String a = "?";
                try {
                    currentStoryID[0] = storyID;
                    a = context.getString(R.string.ifdb_download_info_url, storyID);
                    xmlScraper.scrape(a);
                } catch (Exception e) {
                    // example of failure: http://ifdb.tads.org/dladviser?xml&os=MacOSX&id=9v92p0cv9q48wyk2
                    Log.wtf(TAG, "[IFDBparse] parse fail on URL storyID " + storyID + " " + a, e);
                }
            }
        } catch (Exception e) {
            Log.wtf(TAG,e);
        }
    }
}