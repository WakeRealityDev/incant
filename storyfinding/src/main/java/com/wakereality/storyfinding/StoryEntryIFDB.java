package com.wakereality.storyfinding;

/**
 * Created by Stephen A. Gutknecht on 4/1/17.
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
        return new String[]{
                siteIdentity,
                String.valueOf(rating),
                storyTitle,
                storyAuthor,
                storyWhimsy,
                downloadLink,
                storyDescription
        };
    }
}
