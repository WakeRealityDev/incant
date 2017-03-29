Incant! app Future brainstorming
==================================

Intro, April 2017, State of the Apps

1. The Glk layer in Incant has not been adapted to RemGlk JSON - so it can only run with it's local engines.

2. Wake Reality has adapted Text Fiction's Glk layer to JSON, so it can work with remote engines.

3. Both Text Fiction and Incant! are pure Java apps with relatively small APK size.

4. Right now, both Text Fiction and Incant! do not search storage for stories. However, Thunderword does have the ability to search storage and build a database. Thunderword can publish SHA-256 hash list of all stories it has found. Text Fiction has already been enhanced to process this list and present them.


Future Ideas:

1. Merge Incant! and Text Fiction into one app. Keep the source code split as Android libraries - and one 'master app' of the two that can be compiled into a monolitic app. This woudl create a single APK around 2.5MB that could share some of the features.

2. Text Fiction lacks ability to download and provide any meta information about stories (descriptions, author, etc).  This could be borrowed from Incant!


Where to Start?
================
Incant! could be adapted to process the meta__* files that are being created by Text Fiction app to list Thunderword entries. Right now Incant! has two categories, the left icon is [Download] or [Play], A third category of [Thunderwrod] could be added for the meta__ files - and associated icon download, meta information from IFDB, etc.

One of the major needs is a comprehensive mapping of SHA-256 hash to IFDB icons and meta information (IFDB cross-reference).
