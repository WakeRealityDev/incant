package com.wakereality.incant;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.ImageView;
import android.widget.TextView;

import com.yrek.incant.BuildConfig;
import com.yrek.incant.R;

/**
 * Created by Stephen A. Gutknecht on 2/23/17.
 */

public class AboutAppActivity extends Activity {

    protected TextView about_version1;
    protected TextView about_linkbacks1;
    protected TextView about_linkbacks2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);

        // ToDo: since this is a library, we can't access the parent app's icon
        about_version1 = (TextView) findViewById(R.id.about_version1);
        about_version1.setText("" + BuildConfig.APPLICATION_ID + " version " + BuildConfig.VERSION_NAME);

        ImageView appIcon = (ImageView) findViewById(R.id.about_icon1);

        // Make about link clickable
        about_linkbacks1 = (TextView) findViewById(R.id.about_linkbacks1);
        about_linkbacks1.setText(R.string.app_website_link0);
        about_linkbacks1.setMovementMethod(LinkMovementMethod.getInstance());

        // Make about link clickable
        about_linkbacks2 = (TextView) findViewById(R.id.about_linkbacks2);
        about_linkbacks2.setText(R.string.app_website_link1);
        about_linkbacks2.setMovementMethod(LinkMovementMethod.getInstance());
    }

}
