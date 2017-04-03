package com.wakereality.storyfinding;

/**
 * Created by adminsag on 4/1/17.
 */

public class StoryEntryIFDB {
    public String siteIdentity = "";
    public float rating;
    public String downloadLink = "";
    public String storyTitle = "";
    public String storyAuthor = "";
    public String storyDescription = "";
    public String storyWhimsy = "";

    @Override
    public String toString() {
        if (storyDescription.length() > 60) {
            return siteIdentity + ", " + rating + ", " + storyTitle + ", " + storyAuthor + ", " + downloadLink + ", " + storyDescription.substring(0, 60) + ", " + storyWhimsy;
        } else {
            return siteIdentity + ", " + rating + ", " + storyTitle + ", " + storyAuthor + ", " + downloadLink + ", " + storyDescription + ", " + storyWhimsy;
        }
    }

    public String[] toStringArray() {
        String allTogether = downloadLink + storyAuthor + storyDescription + storyWhimsy;
        /*
        if (allTogether.contains("\"") || allTogether.contains(",")) {
            return new String[]{
                    siteIdentity,
                    String.valueOf(rating),
                    "\"" + storyTitle.replaceAll("\"", "\"\"") + "\"",
                    "\"" + storyAuthor.replaceAll("\"", "\"\"") + "\"",
                    "\"" + storyWhimsy.replaceAll("\"", "\"\"") + "\"",
                    "\"" + downloadLink.replaceAll("\"", "\"\"") + "\"",
                    "\"" + storyDescription.replaceAll("\"", "\"\"") + "\"",
            };
        } else {
        */
            return new String[]{
                    siteIdentity,
                    String.valueOf(rating),
                    storyTitle,
                    storyAuthor,
                    storyWhimsy,
                    downloadLink,
                    storyDescription
            };
        //}
    }
}
