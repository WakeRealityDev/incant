package com.yrek.incant;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;

/*
@author Stephen A. Gutknecht
 */
public class Viewer extends Activity {
    private static final String TAG = Viewer.class.getSimpleName();

    /*
    sample of good download from Chrome:
    D/Viewer: game intent=Intent { act=android.intent.action.VIEW dat=content://downloads/my_downloads/1630 typ=application/x-blorb flg=0x13400001 cmp=com.fictionpuzzle.incant/com.yrek.incant.Viewer }
    D/Viewer: intent getType application/x-blorb
    D/Viewer: gameUri content://downloads/my_downloads/1630 title newDL_1630
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        Log.d(TAG,"game intent="+intent);
        if (!intent.getAction().equals(Intent.ACTION_VIEW)) {
            Log.w(TAG, "game finish intent getAction wrong on "+intent);
            finish();
            return;
        }

        String intentType = intent.getType();
        Log.d(TAG, "intent getType " + intentType);

        switch (intentType)
        {
            // same MIME type used for all, ref https://en.wikipedia.org/wiki/Blorb
            case "application/x-blorb":
                break;
        }

        InputStream gameInputStream = null;
        String title = "newDLt_" + System.currentTimeMillis();

        // Uri gameUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        Uri gameUri = Uri.parse(getIntent().getDataString());
        if (gameUri != null) {
            // typical: content://downloads/my_downloads/1560
            //  desired is the "1560" at end
            title = "newDL_" + gameUri.getLastPathSegment();

            Log.d(TAG, "gameUri " + gameUri + " title " + title);
            // String filePath = getFilePathFromUri(this, gameUri);
            // Log.d(TAG, "gameUri path " + filePath);

            // After a bunch of trial and error, this seems the proper solution?
            try {
                gameInputStream = getContentResolver().openInputStream(gameUri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        if (gameInputStream == null)
        {
            try {
                gameInputStream = getContentResolver().openInputStream(intent.getData());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        // messAroundWithFileA(intent);

        try {
            Story story = new Story(title, null, null, null, null, null, null);
            if (story.download(this, gameInputStream)) {
                intent = new Intent(this, StoryDetails.class);
                intent.putExtra(ParamConst.SERIALIZE_KEY_STORY, story);
                Log.d(TAG, "startActivity for story " + story.getName(this) + " title " + story.getTitle(this) + " getStorageName " + story.getStorageName(this));
                startActivity(intent);
                return;
            }
            else
            {
                Log.w(TAG, "download / local copy failed " + gameUri);
            }
        } catch (Exception e) {
            Log.wtf(TAG,e);
        } finally {
            if (gameInputStream != null) {
                try {
                    gameInputStream.close();
                } catch (Exception e) {
                    Log.wtf(TAG,e);
                }
            }
        }

        Toast.makeText(this, getString(R.string.download_invalid, title), Toast.LENGTH_SHORT).show();
        finish();
    }


    public static String getFilePathFromUri(Context c, Uri uri) {
        String filePath = null;
        if ("content".equals(uri.getScheme())) {
            final String[] filePathColumn = { MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.SIZE };
            ContentResolver contentResolver = c.getContentResolver();

            Cursor cursor = contentResolver.query(uri, filePathColumn, null,
                    null, null);

            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            filePath = cursor.getString(columnIndex);
            Log.i(TAG, "cursor columNames " + Arrays.toString(cursor.getColumnNames()));
            cursor.close();
        } else if ("file".equals(uri.getScheme())) {
            filePath = new File(uri.getPath()).getAbsolutePath();
        } else {
            Log.w(TAG, "Uri " + uri + " has unmatched scheme");
        }

        return filePath;
    }


    public void messAroundWithFileA(Intent intent)
    {
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
        Log.d(TAG, "downloadId " + downloadId);

        if (downloadId >= 0) {
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            Cursor cursorA = downloadManager.query(query);

            Uri uri = Uri.parse(cursorA.getString(cursorA.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)));
            if ("content".equals(uri.getScheme())) {
                Cursor cursorB = getContentResolver().query(uri, new String[]{MediaStore.Images.ImageColumns.DATA}, null, null, null);
                cursorB.moveToFirst();
                final String filePath = cursorB.getString(0);
                cursorB.close();
                uri = Uri.fromFile(new File(filePath));
            }


            if ("file".equals(uri.getScheme())) {
                Intent installIntent = new Intent(Intent.ACTION_VIEW);
                installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Required if launching outside of an activity
                installIntent.setDataAndType(uri, downloadManager.getMimeTypeForDownloadedFile(downloadId));
                startActivity(installIntent);
            } else {
                Log.w(TAG, "uri not file? " + uri);
            }
        }

        String title = null;
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(intent.getData(), new String[] { "title" }, null, null, null);
            if (cursor.getCount() >= 1) {
                cursor.moveToFirst();
                title = cursor.getString(0);
            }
        } finally {
            cursor.close();
        }

        if (title == null) {
            Log.w(TAG,"Viewer got null title");
            finish();
            return;
        } else {
            int index = title.lastIndexOf('/');
            if (index > 0) {
                title = title.substring(index+1);
            }
            index = title.indexOf('.');
            if (index > 0) {
                title = title.substring(0, index);
            }
        }
        Log.d(TAG,"title="+title);
    }
}
