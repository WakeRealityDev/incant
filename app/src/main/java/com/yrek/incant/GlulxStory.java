package com.yrek.incant;

import android.content.Context;
import android.util.Log;

import com.yrek.ifstd.blorb.Blorb;
import com.yrek.ifstd.glk.GlkDispatch;
import com.yrek.ifstd.glk.GlkStream;
import com.yrek.ifstd.glulx.Glulx;
import com.yrek.incant.glk.GlkMain;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public class GlulxStory implements GlkMain {
    private static final long serialVersionUID = 0L;
    private static final String TAG = GlulxStory.class.getSimpleName();
    final Story story;
    final String name;
    transient Thread thread = null;
    transient Glulx glulx = null;
    transient GlkDispatch glk = null;

    public GlulxStory(Story story, String name) {
        this.story = story;
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void init(Context context, GlkDispatch glk, Serializable suspendState) {
        Log.d(TAG,"init glk="+glk);
        this.glk = glk;
        if (suspendState != null) {
            Log.i(TAG,"init going suspendedState");
            this.glulx = (Glulx) suspendState;
            this.glulx.resume(glk);
        } else {
            try {
                this.glulx = new Glulx(story.getGlulxFile(context), glk);
            } catch (IOException e) {
                Log.e(TAG, "create IOException", e);
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void start(final Runnable waitForInit, final Runnable onFinished) {
        thread = new Thread("glk") {
            @Override public void run() {
                waitForInit.run();
                try {
                    Log.d(TAG,"start:start:glk="+glk);
                    glk.glk.main(glulx);
                    Log.d(TAG,"start:finish");
                    synchronized (GlulxStory.this) {
                        thread = null;
                        GlulxStory.this.notifyAll();
                    }
                    onFinished.run();
                } catch (IOException e) {
                    Log.e(TAG, "start IOException", e);
                    throw new RuntimeException(e);
                }
            }
        };
        thread.start();
    }

    @Override
    public void requestSuspend() {
        try {
            Log.d(TAG,"requestSuspend");
            glulx.suspend(false);
        } catch (InterruptedException e) {
            Log.wtf(TAG,e);
        }
    }

    @Override
    public Serializable suspend() {
        try {
            Log.d(TAG,"suspend");
            glulx.suspend(false);
            Log.d(TAG,"suspend:interrupt");
            synchronized (this) {
                if (thread != null) {
                    thread.interrupt();
                    Log.d(TAG,"suspend:wait");
                    while (thread != null) {
                        wait();
                    }
                    Log.d(TAG,"suspend:wait done");
                }
            }
            Log.d(TAG,"suspend:done");
        } catch (InterruptedException e) {
            Log.wtf(TAG,e);
        }
        return glulx;
    }

    @Override
    public boolean suspendRequested() {
        return glulx.suspending();
    }

    @Override
    public boolean finished() {
        return !glulx.suspended() && thread == null;
    }

    @Override
    public Blorb getBlorb(Context context) {
        File blorbFile = story.getBlorbFile(context);
        if (blorbFile.exists()) {
            try {
                return Blorb.from(blorbFile);
            } catch (IOException e) {
                Log.wtf(TAG,e);
            }
        }
        return null;
    }

    @Override
    public File getSaveFile(Context context) {
        return story.getSaveFile(context);
    }

    @Override
    public File getDir(Context context) {
        return story.getDir(context);
    }


    @Override
    public int getGlkLayout() {
        return R.layout.glk;
    }

    @Override
    public int getFrameLayout() {
        return R.id.frame;
    }

    @Override
    public int getNextButton() {
        return R.id.next;
    }

    @Override
    public int getKeyboardButton() {
        return R.id.keyboard;
    }

    @Override
    public int getEditText() {
        return R.id.edit;
    }

    @Override
    public int getSkipButton() {
        return R.id.skip;
    }

    @Override
    public int getOneByOneMeasurer() {
        return R.id.onexone;
    }

    @Override
    public int getHolderLayout() {
        return R.id.game_layout_holder0;
    }

    @Override
    public int getTwoByTwoMeasurer() {
        return R.id.twoxtwo;
    }

    @Override
    public int getProgressBar() {
        return R.id.progress_bar;
    }


    @Override
    public int getTextBufferStyle(int style) {
        switch (style) {
        case GlkStream.StyleEmphasized:
            return R.style.glk_emphasized;
        case GlkStream.StylePreformatted:
            return R.style.glk_preformatted;
        case GlkStream.StyleHeader:
            return R.style.glk_header;
        case GlkStream.StyleSubheader:
            return R.style.glk_subheader;
        case GlkStream.StyleAlert:
            return R.style.glk_alert;
        case GlkStream.StyleNote:
            return R.style.glk_note;
        case GlkStream.StyleBlockQuote:
            return R.style.glk_blockquote;
        case GlkStream.StyleInput:
            return R.style.glk_input;
        default:
            return R.style.glk_normal;
        }
    }

    @Override
    public int getTextGridStyle(int style) {
        switch (style) {
        case GlkStream.StyleEmphasized:
            return R.style.glk_grid_emphasized;
        case GlkStream.StylePreformatted:
            return R.style.glk_grid_preformatted;
        case GlkStream.StyleHeader:
            return R.style.glk_grid_header;
        case GlkStream.StyleSubheader:
            return R.style.glk_grid_subheader;
        case GlkStream.StyleAlert:
            return R.style.glk_grid_alert;
        case GlkStream.StyleNote:
            return R.style.glk_grid_note;
        case GlkStream.StyleBlockQuote:
            return R.style.glk_grid_blockquote;
        case GlkStream.StyleInput:
            return R.style.glk_grid_input;
        default:
            return R.style.glk_grid_normal;
        }
    }

    @Override
    public Integer getStyleForegroundColor(int style) {
        return null;
    }

    @Override
    public Integer getStyleBackgroundColor(int style) {
        return null;
    }
}
