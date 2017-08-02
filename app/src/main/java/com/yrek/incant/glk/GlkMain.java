package com.yrek.incant.glk;

import android.content.Context;

import com.yrek.ifstd.blorb.Blorb;
import com.yrek.ifstd.glk.GlkDispatch;

import java.io.File;
import java.io.Serializable;

public interface GlkMain extends Serializable {
    public String name();
    public void init(Context context, GlkDispatch glk, Serializable suspendState);
    public void start(Runnable waitForInit, Runnable onFinished);
    public void requestSuspend();
    public Serializable suspend();
    public boolean suspendRequested();
    public boolean finished();
    public Blorb getBlorb(Context context);
    public File getSaveFile(Context context);
    public File getRestoreFile(Context context);
    public String getRestoreFileMessage();
    public File getDir(Context context);

    public int getGlkLayout();
    public int getFrameLayout();
    public int getNextButton();
    public int getKeyboardButton();
    public int getEditText();
    public int getSkipButton();
    public int getOneByOneMeasurer();
    public int getTwoByTwoMeasurer();
    public int getProgressBar();

    public int getTextBufferStyle(int style);
    public int getTextGridStyle(int style);
    public Integer getStyleForegroundColor(int style);
    public Integer getStyleBackgroundColor(int style);

    public int getHolderLayout();

}
