THIS IS A FORK of what has not been updated in 2.5 years.

Author's introduction on forum: http://www.intfiction.org/forum/viewtopic.php?f=38&t=11478&start=20#p78133

Incant is an experimental interface to a Glulx interpreter and a Z-Machine interpreter for Android using Android's speech recognition and text-to-speech.

The Z-Machine interpreter is taken from Zax: https://github.com/mattkimmel/zax

Incant's original author Q P Liu has distributed under your choice of the MIT license or GPL license, depending on which Git commit you fork from (note commit a80837fe83f27bd2fb841e0159edd57223fbcdd1).
All changes in this source code fork/branch published on Github by Stephen Gutknecht are granted dual-license, MIT and GPL, so that you can freely choose.

The Android SDK (https://developer.android.com/sdk/index.html) is needed to build.


Video: http://youtu.be/D6i7c7jdV4Q demonstrating speech in and out.


Screenshot
============

![Screenshot](screenshots/HUAWEI_H1611/opening_device-2017-04-02-162832.png)


App Use
===========
1. Long press on game listing entry to get details of the game and option to delete the story download.

Once you start a game

Incant is built for speech interface, so non-speech isn't always intuitive.

1. At the very bottom right there is a dim [Keyboard] button to activate. Sometimes it disappears, but press around bottom right.

From review of the source code, there are special phrases recognized: "enter", "enter ", "open keyboard", "delete word", "space", "backspace".


The story Six shows off features
=====================================
1. Music on opening of Six (make sure your volume is up so you don't overlook this feature)
2. Color graphic images
3. Scrolling vertically if screen is too small
4. Detection of engine features
5. Status line is 3 lines, beyond the default 1 line some apps restrict

Bronze shows that the color text support works.

NOTE:
I made the input field yellow so it is easier to spot where to touch to type input. However, this needs work (remember the app was designed for keyboardless speech input).
This yellow input line (EditText) has a tendency to hide things behind it, like prompts on which key to press (space).
I am working on other projects but figured that some of the crash cleanup and other commits were worth sharing. Someone could finish off and polish this app up.
Search the code for "SettingsCurrent.getGameLayoutInputColorA()" to find where this yellow is set.
You could try a Bluetooth or USB keyboard out to see how well the app works with your game data before putting labor into improving the layout.


A suggested developer ToDo list:
==================================
Developers are welcome to hack on this.

1. Add a welcome screen, brief help/about screen. (PARTIALLY done, now has a welcome message at top of list)
2. rework the keyboard input so it's more friendly. ~~Right now it's kind of klunky because if the story wants you to press space you have to enter a [space] then press [return] to get it into the Glulx interpreter.~~  For some games, you need to be able to actually put [return] into the Glulx engine - "In Bronze I couldn't activate a help menu item because pressing enter didn't send the enter to the game."
3. A file picker so people could browse files and load games that they wish instead of the current live-download only model.
~~4. Menu working but not saved, must be set each app start. Persist.~~ DONE!
5. Extensively test it and identify what works and what does not, what Glk features are missing, etc.
6. Document how to turn it into a 'publish your interactive fiction story as an Android app' like the documentation steps of AndroidIF here: https://github.com/SimonChris/AndroidIF -- both apps are built on MIT license so you can swap code between them.  NOTE: AndroidIF's step 4 isn't needed, Incant runs off-the-shelf Glulx binaries with no special extensions.
7. The game Six in the room "The Giant Garden Bed" (go north 4 times), the layout of the 3-line status window is broken on a 5" Android phone. Even if you rotate the phone (starting from) Portrait to Landscape, it does not reflow the status window content.
8. (Menu) option to Mute the beep on speech input may also end up muting the speech output. Needs work.
9. Saves files to /sdcard/ without use of Android API's. Probably doesn't work well on Android 4.4 for this reason.
10. Sending story files to Thunderword, add content provider for secure exchange and to avoid /sdcard/ path usage. Suggest library: https://github.com/commonsguy/cwac-provider
11. Thunderword screen size ~~and layout (Activity) picking~~ (DONE) for user interface.
12. Thunderword can publish SHA-256 hash of stories it has found on storage (not just the one path that Incant populates). Add a way to import and run those listings.
13. ~~Incant does not preserve the file extensions when downloading off it's list, so it makes sharing the downloaded stories nearly impossible to find for outside apps searching the storage.~~ Copy is made in a Keep folder.
14. Add a Thunderword Command Code dialog so users can prep settings before a launch. Also serves to document the protocol for command codes for developers who wish to enhance their own apps. This code addition can be shared with the Thunderstrike app here on GitHub.
15. Twisty 2.0 is an example of a type of launcher client that Icant could act as download and launcher for - that does not support RemGlk. The current concept of an "Engine Provider" assumes that it serves both as launcher target and RemGlk provider. Add a capabilities concept and code to Twisty 2.0 to demonstrate a launcher-only target.

Discussion topic here on forums: http://www.intfiction.org/forum/viewtopic.php?f=38&t=21075
