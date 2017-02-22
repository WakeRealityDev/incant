package com.yrek.incant.gamelistings;

import android.content.Context;
import android.util.Log;

import com.yrek.incant.R;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IFArchiveScraper extends Scraper {
    private final String scrapeURL;
    private final String downloadURL;

    public IFArchiveScraper(Context context) {
        super(context, CACHE_IFARCHIVE, context.getResources().getInteger(R.integer.ifarchive_cache_timeout));
        this.scrapeURL = context.getString(R.string.ifarchive_scrape_year_url);
        this.downloadURL = context.getString(R.string.ifarchive_download_url);
    }


    /*
    <li class="ParEven"><span class="Date">[15-Sep-2016]</span> <a href="../if-archive/unprocessed/entropy.z8">if-archive/unprocessed/entropy.z8</a>
     */
    private final Pattern ifarchivePattern  = Pattern.compile("\\<li.*class=\"Date\"\\>\\[([^]]+)\\].*\\.\\.(/if-archive/games/(zcode|glulx)/[^\"]+)\"\\>if-archive/games/(zcode|glulx)/([^/]+)\\.(z[1-8]|zblorb|ulx|blb|gblorb)\\</a\\>");
    /*
     <span class="Date">[05-Jul-2016]</span> <a href="../if-archive/games/glulx/Molly_and_the_Butter_Thieves.gblorb">if-archive/games/glulx/Molly_and_the_Butter_Thieves.gblorb</a>
     <span class="Date">[15-Sep-2016]</span> <a href="../if-archive/unprocessed/entropy.z8">if-archive/unprocessed/entropy.z8</a>
     */
    private final Pattern ifarchivePatternA = Pattern.compile("\\<li.*class=\"Date\"\\>\\[([^]]+)\\].*\\.\\.(/if-archive/unprocessed/[^\"]+)\"\\>if-archive/unprocessed/([^/]+)\\.(z[1-8]|zblorb|ulx|blb|gblorb)\\</a\\>");

    @Override
    void scrape(final DataOutputStream out) throws IOException {
        Log.i(TAG, "IFArchive scrapeURL " + scrapeURL);
        scrapeURL(scrapeURL, new PageScraper() {
            @Override public void scrape(String line) throws IOException {
                Log.d(TAG, "IFArchive scrapeURL " + scrapeURL + " length " + line.length() + " " + line);
                Matcher m = ifarchivePattern.matcher(line);
                while (m.find()) {
                    Log.i(TAG, "IFArchive MATCH scrapeURL " + scrapeURL + " length " + line.length() + " " + line + " 5:" + m.group(5) + " 2:" + m.group(2));
                    writeStory(out, m.group(5), "", downloadURL + m.group(2), "", null);
                }

                Matcher mA = ifarchivePatternA.matcher(line);
                while (mA.find()) {
                    Log.i(TAG, "IFArchive MATCHA scrapeURL " + scrapeURL + " length " + line.length() + " " + line + " 2:" +  mA.group(2) + " 3: " + mA.group(3));
                    writeStory(out, mA.group(3), "", downloadURL + mA.group(2), "", null);
                }
            }
        });
    }
}