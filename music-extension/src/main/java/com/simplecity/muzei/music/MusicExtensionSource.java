package com.simplecity.muzei.music;


import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;
import com.simplecity.muzei.music.utils.Constants;
import com.simplecity.muzei.music.utils.MusicExtensionUtils;

import java.io.File;

public class MusicExtensionSource extends RemoteMuzeiArtSource {

    private static final String TAG = "MusicExtensionSource";

    private static final String SOURCE_NAME = "MusicExtensionSource";

    private SharedPreferences mPrefs;

    /**
     * Remember to call this constructor from an empty constructor!
     */
    public MusicExtensionSource() {
        super(SOURCE_NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }

    /**
     * Publishes the {@link com.google.android.apps.muzei.api.Artwork} to Muzei
     *
     * @param artistName the name of the artist
     * @param albumName  the name of the album
     * @param trackName  the name of the song
     * @param uri        the {@link android.net.Uri} to the album art
     */
    public void publishArtwork(String artistName, String albumName, String trackName, Uri uri) {
        publishArtwork(new Artwork.Builder()
                .title(trackName)
                .byline(artistName + " - " + albumName)
                .imageUri(uri)
                .viewIntent(new Intent("android.intent.action.MUSIC_PLAYER"))
                .build());

        mPrefs = getSharedPreferences();
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString("lastTrackName", trackName);
        editor.putString("lastArtistName", artistName);
        editor.putString("lastAlbumName", albumName);
        editor.putString("lastUri", uri.toString());
        editor.apply();
    }

    @Override
    protected void onTryUpdate(int reason) throws RetryException {
        if (reason == UPDATE_REASON_INITIAL) {

            Log.i(TAG, "onTryUpdate.. reason: initial");

            //Log.d(TAG, "Initial Update");
            mPrefs = getSharedPreferences();
            String trackName = mPrefs.getString("lastTrackName", null);
            String artistName = mPrefs.getString("lastArtistName", null);
            String albumName = mPrefs.getString("lastAlbumName", null);
            String uri = mPrefs.getString("lastUri", null);
            if (artistName != null && albumName != null && trackName != null && uri != null) {
                publishArtwork(artistName, albumName, trackName, Uri.parse(uri));
            }
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);

        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(MusicExtensionUtils.EXTENSION_UPDATE_INTENT)) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    final String artistName = extras.getString(Constants.KEY_ARTIST);
                    final String albumName = extras.getString(Constants.KEY_ALBUM);
                    final String trackName = extras.getString(Constants.KEY_TRACK);
                    if (artistName != null && albumName != null && trackName != null) {
                        MusicExtensionUtils.updateMuzei(this, artistName, albumName, trackName);
                    }
                }

            } else if (intent.getAction().equals(MusicExtensionUtils.EXTENSION_CLEAR_INTENT)) {

                mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
                if (mPrefs.getBoolean(SettingsActivity.KEY_PREF_USE_DEFAULT_ARTWORK, false)) {
                    File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "default_wallpaper.jpg");
                    if (file.exists()) {
                        publishArtwork(new Artwork.Builder()
                                .imageUri(Uri.fromFile(file))
                                .build());
                    } else {
                        publishArtwork(new Artwork.Builder()
                                .imageUri(null)
                                .build());
                    }
                }
            }
        }
    }
}
