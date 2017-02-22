package com.yrek.incant.glk;

import android.content.Context;
import android.view.View;
import android.widget.Space;

import com.yrek.ifstd.glk.GlkEvent;
import com.yrek.ifstd.glk.GlkWindow;

class WindowBlank extends Window {
    private static final long serialVersionUID = 0L;

    WindowBlank(int rock, GlkActivity activity) {
        super(rock, activity);
    }

    @Override
    View createView(Context context) {
        return new Space(context);
    }

    @Override
    boolean hasPendingEvent() {
        return false;
    }

    @Override
    GlkEvent getEvent(long timeout, boolean polling) throws InterruptedException {
        return null;
    }

    @Override
    boolean updatePendingOutput(Runnable continueOutput, boolean doSpeech) {
        return false;
    }

    @Override
    public int getType() {
        return GlkWindow.TypeBlank;
    }
}
