THIS IS A FORK of what has not been updated in 2.5 years.

Author's introduction on forum: http://www.intfiction.org/forum/viewtopic.php?f=38&t=11478&start=20#p78133

Incant is an experimental interface to a Glulx interpreter and a Z-Machine interpreter for Android using Android's speech recognition and text-to-speech.

The Z-Machine interpreter is taken from Zax: https://github.com/mattkimmel/zax

Incant's original author Q P Liu has distributed under your choice of the MIT license or GPL license, depending on which Git commit you fork from (note commit a80837fe83f27bd2fb841e0159edd57223fbcdd1).
All changes in this source code fork/branch published on Github by Stephen Gutknecht are granted dual-license, MIT and GPL, so that you can freely choose.

The Android SDK (https://developer.android.com/sdk/index.html) is needed to build.


Video: http://youtu.be/D6i7c7jdV4Q demonstrating speech in and out.



App Use
===========
1. Long press on game listing entry to get details of the game and option to delete.


Once you start a game

Incant is built for speech interface, so non-speech isn't always intuitive.

1. At the very bottom right there is a dim [Keyboard] button to activate. Sometimes it disappears, but press around bottom right.
2. Prompts to [press space] in Six and other games require you to do [space] then [enter]. Just an artifact of it being created for speech recognition and some work could be done to make this easier on players.


The Game Six shows off features
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
1. Add a welcome screen, brief help/about screen,
2. rework the keyboard input so it's more friendly. Right now it's kind of klunky because if the story wants you to press space you have to enter a [space] then press [return] to get it into the Glulx interpreter.  For some games, you need to be able to actually put [return] into the Glulx engine - "In Bronze I couldn't activate a help menu item because pressing enter didn't send the enter to the game."
3. A file picker so people could browse files and load games that they wish instead of the current live-download only model.
4. Menu working but not saved, must be set each app start. Persist.
5. Extensively test it and identify what works and what does not, what Glk features are missing, etc.
6. Document how to turn it into a 'publish your interactive fiction story as an Android app' like the documentation steps of AndroidIF here: https://github.com/SimonChris/AndroidIF -- both apps are built on MIT license so you can swap code between them.  NOTE: AndroidIF's step 4 isn't needed, Incant runs off-the-shelf Glulx binaries with no special extensions.
7. The game Six in the room "The Giant Garden Bed" (go north 4 times), the layout of the 3-line status window is broken on a 5" Android phone. Even if you rotate the phone (starting from) Portrait to Landscape, it does not reflow the status window content.
8. (Menu) option to Mute the beep on speech input may also end up muting the speech output. Needs work.

Discussion topic here on forums: http://www.intfiction.org/forum/viewtopic.php?f=38&t=21075
