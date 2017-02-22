package com.yrek.runconfig;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by adminsag on 9/21/16.
 */
public class BundleHelper {
    private String serializeBundle(final Bundle bundle) {
        String base64 = null;
        final Parcel parcel = Parcel.obtain();
        try {
            parcel.writeBundle(bundle);
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final GZIPOutputStream zos = new GZIPOutputStream(new BufferedOutputStream(bos));
            zos.write(parcel.marshall());
            zos.close();
            base64 = Base64.encodeToString(bos.toByteArray(), 0);
        } catch(IOException e) {
            e.printStackTrace();
            base64 = null;
        } finally {
            parcel.recycle();
        }
        return base64;
    }

    private Bundle deserializeBundle(final String base64) {
        Bundle bundle = null;
        final Parcel parcel = Parcel.obtain();
        try {
            final ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            final byte[] buffer = new byte[1024];
            final GZIPInputStream zis = new GZIPInputStream(new ByteArrayInputStream(Base64.decode(base64, 0)));
            int len = 0;
            while ((len = zis.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            zis.close();
            parcel.unmarshall(byteBuffer.toByteArray(), 0, byteBuffer.size());
            parcel.setDataPosition(0);
            bundle = parcel.readBundle();
        } catch (IOException e) {
            e.printStackTrace();
            bundle = null;
        }  finally {
            parcel.recycle();
        }

        return bundle;
    }



    // http://stackoverflow.com/questions/2598248/how-to-serialize-a-bundle

    /*
    private void saveToPreferences(Bundle in, Context context) {
        Parcel parcel = Parcel.obtain();
        String serialized = null;
        try {
            in.writeToParcel(parcel, 0);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            IOUtils.write(parcel.marshall(), bos);

            serialized = Base64.encodeToString(bos.toByteArray(), 0);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), e.toString(), e);
        } finally {
            parcel.recycle();
        }
        if (serialized != null) {
            SharedPreferences settings = context.getSharedPreferences(PREFS, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("parcel", serialized);
            editor.commit();
        }
    }

    private Bundle restoreFromPreferences(Context context) {
        Bundle bundle = null;
        SharedPreferences settings = context.getSharedPreferences(PREFS, 0);
        String serialized = settings.getString("parcel", null);

        if (serialized != null) {
            Parcel parcel = Parcel.obtain();
            try {
                byte[] data = Base64.decode(serialized, 0);
                parcel.unmarshall(data, 0, data.length);
                parcel.setDataPosition(0);
                bundle = parcel.readBundle();
            } finally {
                parcel.recycle();
            }
        }
        return bundle;
    }
    */


    // http://stackoverflow.com/questions/13660889/save-bundle-to-sharedpreferences

    private static final String SAVED_PREFS_BUNDLE_KEY_SEPARATOR = "§§";

    /**
     * Save a Bundle object to SharedPreferences.
     *
     * NOTE: The editor must be writable, and this function does not commit.
     *
     * @param editor SharedPreferences Editor
     * @param key SharedPreferences key under which to store the bundle data. Note this key must
     *            not contain '§§' as it's used as a delimiter
     * @param preferences Bundled preferences
     */
    public static void savePreferencesBundle(SharedPreferences.Editor editor, String key, Bundle preferences) {
        Set<String> keySet = preferences.keySet();
        Iterator<String> it = keySet.iterator();
        String prefKeyPrefix = key + SAVED_PREFS_BUNDLE_KEY_SEPARATOR;

        while (it.hasNext()){
            String bundleKey = it.next();
            Object o = preferences.get(bundleKey);
            if (o == null){
                editor.remove(prefKeyPrefix + bundleKey);
            } else if (o instanceof Integer){
                editor.putInt(prefKeyPrefix + bundleKey, (Integer) o);
            } else if (o instanceof Long){
                editor.putLong(prefKeyPrefix + bundleKey, (Long) o);
            } else if (o instanceof Boolean){
                editor.putBoolean(prefKeyPrefix + bundleKey, (Boolean) o);
            } else if (o instanceof CharSequence){
                editor.putString(prefKeyPrefix + bundleKey, ((CharSequence) o).toString());
            } else if (o instanceof Bundle){
                savePreferencesBundle(editor, prefKeyPrefix + bundleKey, ((Bundle) o));
            } else if (o instanceof Serializable) {
                String tempConvert = objectToString((Serializable) o);
                editor.putString(prefKeyPrefix + bundleKey, ((CharSequence) tempConvert).toString());
            }
            else
            {
                Log.w("BundleHelper", "unknown type " + bundleKey);
            }
        }
    }

    /**
     * Load a Bundle object from SharedPreferences.
     * (that was previously stored using savePreferencesBundle())
     *
     * NOTE: The editor must be writable, and this function does not commit.
     *
     * @param sharedPreferences SharedPreferences
     * @param key SharedPreferences key under which to store the bundle data. Note this key must
     *            not contain '§§' as it's used as a delimiter
     *
     * @return bundle loaded from SharedPreferences
     */
    public static Bundle loadPreferencesBundle(SharedPreferences sharedPreferences, String key) {
        Bundle bundle = new Bundle();
        Map<String, ?> all = sharedPreferences.getAll();
        Iterator<String> it = all.keySet().iterator();
        String prefKeyPrefix = key + SAVED_PREFS_BUNDLE_KEY_SEPARATOR;
        Set<String> subBundleKeys = new HashSet<String>();

        while (it.hasNext()) {

            String prefKey = it.next();

            if (prefKey.startsWith(prefKeyPrefix)) {
                String bundleKey = prefKey.substring(prefKeyPrefix.length());
                Log.w("BundleHelper", "bundleKey " + bundleKey + " from " + prefKey);

                if (!bundleKey.contains(SAVED_PREFS_BUNDLE_KEY_SEPARATOR)) {

                    Object o = all.get(prefKey);
                    if (o == null) {
                        // Ignore null keys
                    } else if (o instanceof Integer) {
                        bundle.putInt(bundleKey, (Integer) o);
                    } else if (o instanceof Long) {
                        bundle.putLong(bundleKey, (Long) o);
                    } else if (o instanceof Boolean) {
                        bundle.putBoolean(bundleKey, (Boolean) o);
                    } else if (o instanceof CharSequence) {
                        switch (bundleKey)
                        {
                            case "SUSPEND_STATE":
                            case "SUSPEND_ACTIVITY_STATE":
                                bundle.putSerializable(bundleKey, stringToObject(((CharSequence) o).toString()));
                                break;
                            default:
                                bundle.putString(bundleKey, ((CharSequence) o).toString());
                                break;
                        }
                    } else if (o instanceof Serializable) {
                        bundle.putSerializable(bundleKey, stringToObject(o.toString()));
                    }
                }
                else {
                    // Key is for a sub bundle
                    String subBundleKey = bundleKey.substring(0, bundleKey.length() - SAVED_PREFS_BUNDLE_KEY_SEPARATOR.length());
                    Log.w("BundleHelper", "bundleKey " + bundleKey + " subBundleKey" + subBundleKey);
                    subBundleKeys.add(subBundleKey);
                }
            }
            else {
                // Key is not related to this bundle.
            }
        }

        // Recursively process the sub-bundles
        for (String subBundleKey : subBundleKeys) {
            Bundle subBundle = loadPreferencesBundle(sharedPreferences, prefKeyPrefix + subBundleKey);
            bundle.putBundle(subBundleKey, subBundle);
        }

        return bundle;
    }


    // http://stackoverflow.com/questions/5816695/android-sharedpreferences-with-serializable-object

    static public String objectToString(Serializable object) {
        String encoded = null;
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(object);
            objectOutputStream.close();
            encoded = new String(Base64.encodeToString(byteArrayOutputStream.toByteArray(),0));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return encoded;
    }

    @SuppressWarnings("unchecked")
    static public Serializable stringToObject(String string){
        byte[] bytes = Base64.decode(string,0);
        Serializable object = null;
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream( new ByteArrayInputStream(bytes) );
            object = (Serializable)objectInputStream.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        return object;
    }
}
