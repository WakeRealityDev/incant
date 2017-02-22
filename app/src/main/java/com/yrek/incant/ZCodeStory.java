package com.yrek.incant;

import android.content.Context;
import android.util.Log;

import com.yrek.ifstd.blorb.Blorb;
import com.yrek.ifstd.glk.GlkDispatch;
import com.yrek.ifstd.glk.GlkStream;
import com.yrek.ifstd.zcode.ZCode;
import com.yrek.incant.glk.GlkMain;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

class ZCodeStory implements GlkMain {
    private static final long serialVersionUID = 0L;
    private static final String TAG = ZCodeStory.class.getSimpleName();
    final Story story;
    final String name;
    int textForegroundColor;
    int textBackgroundColor;
    int textInputColor;
    transient Thread thread = null;
    transient ZCode zcode;
    transient GlkDispatch glk = null;

    ZCodeStory(Story story, String name) {
        this.story = story;
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void init(Context context, GlkDispatch glk, Serializable suspendState) {
        this.glk = glk;
        textForegroundColor = context.getResources().getColor(R.color.text);
        textBackgroundColor = context.getResources().getColor(R.color.background);
        textInputColor = context.getResources().getColor(R.color.input_text);
        if (suspendState != null) {
            this.zcode = (ZCode) suspendState;
            try {
                this.zcode.resume(glk);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                this.zcode = new ZCode(story.getZcodeFile(context), glk);
            } catch (IOException e) {
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
                    zcode.initGlk(textForegroundColor, textBackgroundColor);
                    glk.glk.main(zcode);
                    synchronized (ZCodeStory.this) {
                        thread = null;
                        ZCodeStory.this.notifyAll();
                    }
                    onFinished.run();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        thread.start();
    }

    @Override
    public void requestSuspend() {
        try {
            zcode.suspend(false);
        } catch (InterruptedException e) {
            Log.wtf(TAG,e);
        }
    }

    @Override
    public Serializable suspend() {
        try {
            zcode.suspend(false);
            synchronized (this) {
                if (thread != null) {
                    thread.interrupt();
                    while (thread != null) {
                        wait();
                    }
                }
            }
        } catch (InterruptedException e) {
            Log.wtf(TAG,e);
        }
        return zcode;
    }

    @Override
    public boolean suspendRequested() {
        return zcode.suspending();
    }

    @Override
    public boolean finished() {
        return !zcode.suspended() && thread == null;
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
            return R.style.zcode_italic;
        case GlkStream.StylePreformatted:
            return R.style.zcode_preformatted;
        case GlkStream.StyleHeader:
            return R.style.zcode_bold;
        case GlkStream.StyleSubheader:
            return R.style.zcode_bolditalic;
        case GlkStream.StyleAlert:
            return R.style.zcode_normal;
        case GlkStream.StyleNote:
            return R.style.zcode_preformatted;
        case GlkStream.StyleBlockQuote:
            return R.style.zcode_italic;
        case GlkStream.StyleUser1:
            return R.style.zcode_bold;
        case GlkStream.StyleUser2:
            return R.style.zcode_bolditalic;
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
            return R.style.zcode_grid_italic;
        case GlkStream.StylePreformatted:
            return R.style.zcode_grid_normal;
        case GlkStream.StyleHeader:
            return R.style.zcode_grid_bold;
        case GlkStream.StyleSubheader:
            return R.style.zcode_grid_bolditalic;
        case GlkStream.StyleAlert:
        case GlkStream.StyleNote:
            return R.style.zcode_grid_normal;
        case GlkStream.StyleBlockQuote:
            return R.style.zcode_grid_italic;
        case GlkStream.StyleUser1:
            return R.style.zcode_grid_bold;
        case GlkStream.StyleUser2:
            return R.style.zcode_grid_bolditalic;
        default:
            return R.style.glk_grid_normal;
        }
    }

    @Override
    public Integer getStyleForegroundColor(int style) {
        switch (style) {
        case GlkStream.StyleAlert:
        case GlkStream.StyleNote:
        case GlkStream.StyleBlockQuote:
        case GlkStream.StyleUser1:
        case GlkStream.StyleUser2:
            return textBackgroundColor;
        case GlkStream.StyleInput:
            return textInputColor;
        default:
            return textForegroundColor;
        }
    }

    @Override
    public Integer getStyleBackgroundColor(int style) {
        switch (style) {
        case GlkStream.StyleAlert:
        case GlkStream.StyleNote:
        case GlkStream.StyleBlockQuote:
        case GlkStream.StyleUser1:
        case GlkStream.StyleUser2:
            return textForegroundColor;
        default:
            return textBackgroundColor;
        }
    }
}
