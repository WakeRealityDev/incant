package com.wakereality.storyfinding;

/**
 * Created by Stephen A. Gutknecht on 4/1/17.
 */

public class StoryEntryIFDB {
    // IFDB site identity, key to the 'story', which may include multiple files (multiple SHA256 to same story)
    public String siteIdentity = "";
    public float rating;
    public String downloadLink = "";
    public String storyTitle = "";
    public String storyAuthor = "";
    public String storyDescription = "";
    public String storyWhimsy = "";
    public long listingWhen = 0L;
    // Hint if newly added or other special feature, have 31 bits to work wtih
    public int tickeBits = 0;
    // could be a list? "folder1/folder1a/file.a*folder2/file.b"
    public String extractFilename = "";
    // For playloads, not for zip containers - for the zblorb/gblorb/etc inside.
    public String fileHashSHA256  = "";
    // linuxFileSystemAllowableFilename Windows may not allow these filenames and a set of repalcement patterns may be required. For example, ":" in the filename for Zork, Windows would not allow.
    public String descriptiveFilename = "";
    public int maturitySet = 0;
    public int engineCode = 0;
    // can you share this to peer devices?
    public int copyRights = 0;

    @Override
    public String toString() {
        if (storyDescription.length() > 60) {
            return siteIdentity + ", " + rating + ", " + storyTitle + ", " + storyAuthor + ", " + downloadLink + ", " + extractFilename + ", " + storyDescription.substring(0, 60) + ", " + storyWhimsy + ", " + listingWhen;
        } else {
            return siteIdentity + ", " + rating + ", " + storyTitle + ", " + storyAuthor + ", " + downloadLink + ", " + extractFilename + ", " + storyDescription + ", " + storyWhimsy + ", " + listingWhen;
        }
    }

    public String[] toStringArray() {
        return new String[] {
                siteIdentity,
                downloadLink,
                extractFilename,
                fileHashSHA256,
                descriptiveFilename,
                String.valueOf(rating),
                storyTitle,
                storyAuthor,
                storyWhimsy,
                String.valueOf(listingWhen),
                String.valueOf(tickeBits),
                String.valueOf(maturitySet),
                String.valueOf(engineCode),
                String.valueOf(copyRights),
                storyDescription,
        };
    }
}
