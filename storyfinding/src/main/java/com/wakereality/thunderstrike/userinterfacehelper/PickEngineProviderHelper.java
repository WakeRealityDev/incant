package com.wakereality.thunderstrike.userinterfacehelper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import com.squareup.phrase.Phrase;
import com.wakereality.storyfinding.R;
import com.wakereality.thunderstrike.EchoSpot;
import com.wakereality.thunderstrike.dataexchange.EngineProvider;
import com.yrek.incant.StoryListSpot;

import java.util.Map;

/**
 * Created by Stephen A. Gutknecht on 4/14/17.
 */

public class PickEngineProviderHelper {

    protected SharedPreferences prefs;

    public PickEngineProviderHelper(Context appContext) {
        prefs = appContext.getSharedPreferences("findstories", Context.MODE_PRIVATE);
    }

    public static void animateClickedView(final View view) {
        // Poor man's animation to show visual feedback of click.
        view.setAlpha(0.2f);
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                view.setAlpha(1.0f);
            }
        }, 1200L);
    }

    public boolean redrawEngineProvider(final TextView engineProviderStatus, final CharSequence prefixText) {
        if (engineProviderStatus == null) {
            return false;
        }

        final Context context = engineProviderStatus.getContext();
        final Resources res = context.getResources();

        CharSequence extraA = "";
        if (EchoSpot.detectedEngineProviders.size() > 0) {
            // Phrase library seems to drop the leading space that is in the string resource, so add it back here.
            extraA = " " + Phrase.from(res, R.string.engine_provider_detected_extra).put("quantity", EchoSpot.detectedEngineProviders.size()).format();
            // show current index.
            if (EchoSpot.detectedEngineProviders.size() > 1) {
                extraA = extraA + "/" + EchoSpot.currentEngineProviderIndex;
            };
        }

        if (prefixText == null) {
            engineProviderStatus.setText("");
        } else {
            engineProviderStatus.setText(prefixText);
        }

        if (EchoSpot.currentEngineProvider != null) {
            engineProviderStatus.append(Phrase.from(res, R.string.engine_provider_detected_named)
                    .put("engine", EchoSpot.currentEngineProvider.providerAppPackage.replace("com.wakereality.", "wakereality."))
                    .put("extra_a", extraA)
                    .format()
            );
            Log.i("PEPH", "render EventEngineProviderChange setting " + extraA);
        } else {
            engineProviderStatus.append(res.getText(R.string.engine_none_detected_shorter));
            return false;
        }

        // Switch providers with touch if multiple available
        // ToDo: make this smarter about not picking the one that is already visible on first touch.
        if (EchoSpot.detectedEngineProviders.size() > 1) {
            Log.v("StoryDetails", "[engineProviderPick] onClick assign, on index " + EchoSpot.currentEngineProviderIndex + " [" + EchoSpot.currentEngineProvider.providerAppPackage + "] size " + EchoSpot.detectedEngineProviders.size());
            engineProviderStatus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    /*
                    Don't want to assign number ID to engines, they are strings and want to allow any value.
                     */
                    int newIndex = EchoSpot.currentEngineProviderIndex + 1;
                    if (newIndex >= EchoSpot.detectedEngineProviders.size()) {
                        // wrap back to zero
                        newIndex = 0;
                    }

                    Log.v("StoryDetails", "[engineProviderPick] click, on index " + EchoSpot.currentEngineProviderIndex + " to " + newIndex + " [" + EchoSpot.currentEngineProvider.providerAppPackage + "] size " + EchoSpot.detectedEngineProviders.size());

                    // Match index up to entry.
                    int onLoopIndex = 0;
                    for (Map.Entry<String, EngineProvider> entry : EchoSpot.detectedEngineProviders.entrySet()) {
                        if (onLoopIndex == newIndex) {
                            EchoSpot.currentEngineProvider = entry.getValue();
                            Log.w("StoryDetails", "[engineProviderPick] current changed from index " + EchoSpot.currentEngineProviderIndex + " to " + newIndex + " [" + EchoSpot.currentEngineProvider.providerAppPackage + "] size " + EchoSpot.detectedEngineProviders.size());
                            EchoSpot.currentEngineProviderIndex = newIndex;
                            break;
                        }
                        onLoopIndex++;
                    }

                    // ToDo: save to shared preferences?
                    // redraw
                    redrawEngineProvider(engineProviderStatus, prefixText);
                    animateClickedView(v);
                }
            });
        }
        return true;
    }

    public void spinnerForThunderwordActivity(Spinner spinnerView, CheckBox checkboxView) {
        Context context = spinnerView.getContext();
        final TypedArray selectedActivityValues = context.getResources().obtainTypedArray(R.array.thunderword_activity_values);

        spinnerView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                StoryListSpot.optionLaunchExternalActivityCode = selectedActivityValues.getInt(position, -1);
                prefs.edit().putInt("TWactivityPos", position).commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                StoryListSpot.optionLaunchExternalActivityCode = 0;
                prefs.edit().putInt("TWactivityPos", 0).commit();
            }
        });
        spinnerView.setSelection(prefs.getInt("TWactivityPos", 0), false /* Do not animate, avoid triggering Listner */);

        checkboxView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StoryListSpot.optionaLaunchInterruptEngine = ((CheckBox) v).isChecked();
            }
        });
    }
}
