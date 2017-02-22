package com.yrek.incant.glk;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import com.yrek.ifstd.glk.GlkEvent;
import com.yrek.ifstd.glk.GlkSChannel;

class SChannel extends GlkSChannel implements Serializable {
    private static final String TAG = SChannel.class.getSimpleName();

    private static final long serialVersionUID = 0L;
    transient GlkActivity activity;
    transient MediaPlayer player;
    private float volume = 0.25f;
    private GlkEvent soundEvent = null;
    private GlkEvent volumeEvent = null;

    SChannel(int rock, GlkActivity activity) {
        super(rock);
        this.activity = activity;
    }

    void restoreActivity(GlkActivity activity) {
        this.activity = activity;
    }

    void onPause() {
        MediaPlayer player = this.player;
        if (player != null) {
            player.release();
            this.player = null;
        }
    }

    void onResume() {
    }

    boolean hasPendingEvent() {
        return soundEvent != null || volumeEvent != null;
    }

    GlkEvent getEvent() {
        GlkEvent result = null;
        if (soundEvent != null) {
            result = soundEvent;
            soundEvent = null;
        } else if (volumeEvent != null) {
            result = volumeEvent;
            volumeEvent = null;
        }
        return result;
    }

    @Override
    public void destroyChannel() throws IOException {
        MediaPlayer player = this.player;
        if (player != null) {
            player.release();
            this.player = null;
        }
    }

    @Override
    public boolean play(int resourceId) throws IOException {
        Log.d(TAG,"play:ch="+this+",resourceId="+resourceId);
        File file = activity.getSoundResourceFile(resourceId);
        if (file == null) {
            return false;
        }
        if (player == null) {
            player = new MediaPlayer();
        } else {
            player.reset();
        }
        player.setVolume(volume, volume);
        player.setDataSource(file.getPath());
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.prepare();
        player.start();
        return true;
    }

    @Override
    public boolean playExt(final int resourceId, final int repeats, final int notify) throws IOException {
        Log.d(TAG,"playExt:ch="+this+",resourceId="+resourceId+",repeats="+repeats+",notify="+notify);
        File file = activity.getSoundResourceFile(resourceId);
        if (file == null) {
            return false;
        }
        if (repeats == 0) {
            if (player != null) {
                player.reset();
            }
            return true;
        }
        if (player == null) {
            player = new MediaPlayer();
        } else {
            player.reset();
        }
        player.setVolume(volume, volume);
        player.setDataSource(file.getPath());
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        if (notify != 0 || repeats > 1) {
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                int count = 0;
                @Override public void onCompletion(MediaPlayer mp) {
                    count++;
                    if (count < repeats) {
                        mp.start();
                    } else if (notify != 0) {
                        soundEvent = new GlkEvent(GlkEvent.TypeSoundNotify, null, resourceId, notify);
                    }
                }
            });
        }
        if (repeats == -1) {
            player.setLooping(true);
        }
        player.prepare();
        player.start();
        return true;
    }

    @Override
    public void stop() throws IOException {
        Log.d(TAG,"stop:ch="+this);
        if (player != null) {
            player.stop();
        }
    }

    @Override
    public void pause() throws IOException {
        Log.d(TAG,"pause:ch="+this);
        if (player != null) {
            player.pause();
        }
    }

    @Override
    public void unpause() throws IOException {
        Log.d(TAG,"unpause:ch="+this);
        if (player != null) {
            player.start();
        }
    }

    @Override
    public void setVolume(int volume) throws IOException {
        Log.d(TAG,"setVolume:ch="+this+",volume="+volume);
        this.volume = translateVolume(volume);
        if (player != null) {
            player.setVolume(this.volume, this.volume);
        }
    }

    @Override
    public void setVolumeExt(int volume, int duration, int notify) throws IOException {
        Log.d(TAG,"setVolumeExt:ch="+this+",volume="+volume+",duration="+duration+",notify="+notify);
        this.volume = translateVolume(volume);
        if (player != null) {
            player.setVolume(this.volume, this.volume);
        }
        if (notify != 0) {
            volumeEvent = new GlkEvent(GlkEvent.TypeVolumeNotify, null, 0, notify);
        }
    }

    private float translateVolume(int volume) {
        return Math.max(0.0f,Math.min((float) volume / 262144.0f, 0.8f));
    }
}
