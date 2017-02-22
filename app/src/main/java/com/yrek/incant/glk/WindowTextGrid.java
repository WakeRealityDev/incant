package com.yrek.incant.glk;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayDeque;

import com.yrek.ifstd.glk.GlkByteArray;
import com.yrek.ifstd.glk.GlkEvent;
import com.yrek.ifstd.glk.GlkIntArray;
import com.yrek.ifstd.glk.GlkStream;
import com.yrek.ifstd.glk.GlkStreamResult;
import com.yrek.ifstd.glk.GlkWindow;
import com.yrek.ifstd.glk.GlkWindowSize;
import com.yrek.ifstd.glk.GlkWindowStream;
import com.yrek.ifstd.glk.UnicodeString;

class WindowTextGrid extends Window {
    private static final long serialVersionUID = 0L;
    private static final String TAG = WindowTextGrid.class.getSimpleName();

    private transient SpannableStringBuilder gridBuffer = new SpannableStringBuilder();
    private int gridWidth = 0;
    private int gridHeight = 0;
    private int cursorX = 0;
    private int cursorY = 0;
    private int currentStyle = GlkStream.StyleNormal;
    private boolean lineEventRequested = false;
    private boolean charEventRequested = false;
    private GlkByteArray lineEventBuffer = null;
    private boolean echoLineEvent = true;
    private int writeCount = 0;
    private boolean pendingResizeEvent = false;
    private boolean mouseEventRequested = false;
    private int mouseIndex = -1;
    private int linkVal = 0;
    private boolean hyperlinkEventRequested = false;
    private int hyperlinkEventVal = 0;
    private Integer backgroundColor = null;

    WindowTextGrid(int rock, GlkActivity activity) {
        super(rock, activity);
        backgroundColor = activity.getStyleBackgroundColor(getType(), GlkStream.StyleNormal);
    }

    @Override
    View createView(Context context) {
        final GestureDetector gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onSingleTapConfirmed(MotionEvent e) {
                if (mouseEventRequested || hyperlinkEventRequested) {
                    TextView textView = (TextView) view;
                    mouseIndex = ((TextView) view).getOffsetForPosition(e.getX(), e.getY());
                    HyperlinkSpan[] spans = gridBuffer.getSpans(mouseIndex, mouseIndex, HyperlinkSpan.class);
                    if (spans != null && spans.length > 0) {
                        hyperlinkEventVal = spans[0].linkVal;
                    }
                    activity.input.cancelInput();
                    return true;
                }
                return false;
            }
        });
        final ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(activity, onScaleGestureListener());
        TextView textView = new TextView(context) {
            @Override public boolean onTouchEvent(MotionEvent motionEvent) {
                return gestureDetector.onTouchEvent(motionEvent) || scaleGestureDetector.onTouchEvent(motionEvent) || super.onTouchEvent(motionEvent);
            }
        };
        if (backgroundColor != null) {
            textView.setBackgroundColor(0xff000000 | backgroundColor);
        }
        return textView;
    }

    @Override
    boolean hasPendingEvent() {
        return lineEventRequested || charEventRequested || pendingResizeEvent || (mouseEventRequested && mouseIndex >= 0) || (hyperlinkEventRequested && hyperlinkEventVal != 0);
    }

    @Override
    GlkEvent getEvent(long timeout, boolean polling) throws InterruptedException {
        if (pendingResizeEvent) {
            pendingResizeEvent = false;
            return new GlkEvent(GlkEvent.TypeArrange, this, 0, 0);
        } else if (polling) {
            return null;
        } else if (hyperlinkEventRequested && hyperlinkEventVal != 0) {
            int val = hyperlinkEventVal;
            hyperlinkEventRequested = false;
            hyperlinkEventVal = 0;
            return new GlkEvent(GlkEvent.TypeHyperlink, this, val, 0);
        } else if (mouseEventRequested && mouseIndex >= 0) {
            int x = mouseIndex % (gridWidth + 1);
            int y = mouseIndex / (gridWidth + 1);
            mouseEventRequested = false;
            mouseIndex = -1;
            return new GlkEvent(GlkEvent.TypeMouseInput, this, x, y);
        } else if (lineEventRequested) {
            activity.hideProgressBar();
            activity.speech.resetSkip();
            String line = activity.input.getInput(timeout);
            if (line == null) {
                return null;
            }
            lineEventRequested = false;
            int count = lineEventBuffer == null ? 0 : Math.min(lineEventBuffer.getArrayLength(), gridWidth - cursorX);
            for (int i = 0; i < count; i++) {
                lineEventBuffer.setByteElementAt(i, line.charAt(i));
            }
            if (echoLineEvent) {
                int saveStyle = currentStyle;
                try {
                    stream.setStyle(GlkStream.StyleInput);
                    stream.putString(line.substring(0, count));
                    stream.setStyle(saveStyle);
                    stream.putChar(10);
                } catch (IOException e) {
                    Log.wtf(TAG,e);
                    stream.setStyle(saveStyle);
                }
            }
            activity.showProgressBar();
            return new GlkEvent(GlkEvent.TypeLineInput, this, count, 0);
        } else if (charEventRequested) {
            activity.hideProgressBar();
            activity.speech.resetSkip();
            int ch = activity.input.getCharInput(timeout);
            charEventRequested = false;
            activity.showProgressBar();
            return new GlkEvent(GlkEvent.TypeCharInput, this, ch, 0);
        } else {
            return null;
        }
    }

    @Override
    boolean updatePendingOutput(Runnable continueOutput, boolean doSpeech) {
        if (gridBuffer == null) {
            return false;
        }
        ((TextView) getView()).setText(gridBuffer);
        return false;
    }

    @Override
    TextAppearanceSpan getSpanForStyle(int style) {
        return new TextAppearanceSpan(activity, activity.main.getTextGridStyle(style));
    }


    @Override
    int getPixelWidth(int size) {
        waitForTextMeasurer();
        return size*activity.charWidth+activity.charHMargin;
    }

    @Override
    int getPixelHeight(int size) {
        waitForTextMeasurer();
        return size*activity.charHeight+activity.charVMargin;
    }

    @Override
    void onWindowSizeChanged(int width, int height) {
        waitForTextMeasurer();
        int newGridWidth = width/activity.charWidth;
        int newGridHeight = height/activity.charHeight;
        if (gridBuffer == null) {
            gridBuffer = new SpannableStringBuilder();
            gridWidth = 0;
            gridHeight = 0;
        } else {
            for (int i = gridHeight-1; i >= 0; i--) {
                if (newGridWidth < gridWidth) {
                    gridBuffer.delete(i*(gridWidth+1)+newGridWidth, i*(gridWidth+1)+gridWidth);
                } else if (newGridWidth > gridWidth) {
                    for (int j = gridWidth; j < newGridWidth; j++) {
                        gridBuffer.insert(i*(gridWidth+1) + j, " ");
                    }
                }
            }
        }
        for (int i = gridHeight; i < newGridHeight; i++) {
            if (i > 0) {
                gridBuffer.append('\n');
            }
            for (int j = 0; j < newGridWidth; j++) {
                gridBuffer.append(' ');
            }
        }
        if (newGridHeight < gridHeight) {
            gridBuffer.delete((newGridWidth+1)*gridHeight-1, gridBuffer.length());
        }
        cleanGridBufferSpans();
        pendingResizeEvent = true;
        gridWidth = newGridWidth;
        gridHeight = newGridHeight;
        Log.d(TAG,"gridWidth="+gridWidth+",gridHeight="+gridHeight);
    }

    @Override
    void clearPendingArrangeEvent() {
        pendingResizeEvent = false;
    }

    private void cleanGridBufferSpans() {
        for (Object o : gridBuffer.getSpans(0, gridBuffer.length(), Object.class)) {
            if (gridBuffer.getSpanStart(o) >= gridBuffer.getSpanEnd(o)) {
                gridBuffer.removeSpan(o);
            }
        }
    }

    private void cleanGridBufferSpans(int start, int end) {
        for (Object o : gridBuffer.getSpans(start, end, Object.class)) {
            if (gridBuffer.getSpanStart(o) >= start && gridBuffer.getSpanEnd(o) <= end) {
                gridBuffer.removeSpan(o);
            }
        }
    }


    @Override
    public GlkWindowStream getStream() {
        return stream;
    }

    @Override
    public GlkStreamResult close() throws IOException {
        super.close();
        stream.destroy();
        return new GlkStreamResult(0, writeCount);
    }

    @Override
    public GlkWindowSize getSize() {
        return new GlkWindowSize(gridWidth, gridHeight);
    }

    @Override
    public int getType() {
        return GlkWindow.TypeTextGrid;
    }

    @Override
    public void clear() {
        if (gridBuffer == null) {
            return;
        }
        gridBuffer.clear();
        gridBuffer.clearSpans();
        cursorX = 0;
        cursorY = 0;
        for (int i = 0; i < gridWidth*gridHeight; i++) {
            if (i > 0 && i%gridWidth == 0) {
                gridBuffer.append('\n');
            }
            gridBuffer.append(' ');
        }
    }

    @Override
    public void moveCursor(int x, int y) throws IOException {
        cursorX = x;
        cursorY = y;
    }

    @Override
    public int getCursorX() {
        return cursorX;
    }

    @Override
    public int getCursorY() {
        return cursorY;
    }

    @Override
    public boolean styleDistinguish(int style1, int style2) {
        return activity.main.getTextGridStyle(style1) != activity.main.getTextGridStyle(style2);
    }

    @Override
    public Integer styleMeasure(int style, int hint) {
        if (true) { //... tmp
            return null;
        } //... tmp
        throw new RuntimeException("unimplemented");
    }

    @Override
    public void requestLineEvent(GlkByteArray buffer, int initLength) {
        if (lineEventRequested || charEventRequested) {
            throw new IllegalStateException();
        }
        lineEventRequested = true;
        lineEventBuffer = buffer;
    }

    @Override
    public void requestCharEvent() {
        if (lineEventRequested || charEventRequested) {
            throw new IllegalStateException();
        }
        charEventRequested = true;
    }

    @Override
    public void requestMouseEvent() {
        if (!mouseEventRequested) {
            mouseEventRequested = true;
            mouseIndex = -1;
        }
    }

    @Override
    public void requestHyperlinkEvent() {
        if (!hyperlinkEventRequested) {
            hyperlinkEventRequested = true;
            hyperlinkEventVal = 0;
        }
    }

    @Override
    public GlkEvent cancelLineEvent() {
        if (!lineEventRequested) {
            return new GlkEvent(GlkEvent.TypeNone, this, 0, 0);
        }
        lineEventRequested = false;
        lineEventBuffer = null;
        return new GlkEvent(GlkEvent.TypeLineInput, this, 0, 0);
    }

    @Override
    public void cancelCharEvent() {
        charEventRequested = false;
    }

    @Override
    public void cancelMouseEvent() {
        mouseEventRequested = false;
        mouseIndex = -1;
    }

    @Override
    public void cancelHyperlinkEvent() {
        hyperlinkEventRequested = false;
        hyperlinkEventVal = 0;
    }


    @Override
    public void setEchoLineEvent(boolean echoLineEvent) {
        this.echoLineEvent = echoLineEvent;
    }

    
    private void putChar(char ch, boolean setSpan) {
        if (gridBuffer == null) {
            return;
        }
        if (ch == '\n') {
            cursorX = 0;
            cursorY++;
            return;
        }
        int index = cursorY*(gridWidth+1) + cursorX;
        cursorX++;
        if (cursorX >= gridWidth) {
            cursorX = 0;
            cursorY++;
        }
        if (index >= 0 && index < gridBuffer.length()) {
            gridBuffer.replace(index, index+1, String.valueOf(ch));
            if (setSpan) {
                styleText(gridBuffer, index, index+1, currentStyle, activity.getStyleForegroundColor(getType(), currentStyle), activity.getStyleBackgroundColor(getType(), currentStyle), linkVal);
            }
        }
    }

    private final WindowStream stream = new WindowStream(this) {
        private static final long serialVersionUID = 0L;

        @Override
        public void putChar(int ch) throws IOException {
            super.putChar(ch);
            writeCount++;
            WindowTextGrid.this.putChar((char) (ch&255), true);
        }

        @Override
        public void putString(CharSequence string) throws IOException {
            super.putString(string);
            writeCount += string.length();
            if (gridBuffer == null) {
                return;
            }
            int start = cursorY*(gridWidth+1) + cursorX;
            for (int i = 0; i < string.length(); i++) {
                WindowTextGrid.this.putChar((char) (string.charAt(i) & 255), false);
            }
            int end = cursorY*(gridWidth+1) + cursorX;
            if (start < gridBuffer.length() && end > 0 && end > start) {
                start = Math.max(0, start);
                end = Math.min(end, gridBuffer.length());
                cleanGridBufferSpans(start, end);
                styleText(gridBuffer, start, end, currentStyle, activity.getStyleForegroundColor(getType(), currentStyle), activity.getStyleBackgroundColor(getType(), currentStyle), linkVal);
            }
        }

        @Override
        public void putBuffer(GlkByteArray buffer) throws IOException {
            super.putBuffer(buffer);
            writeCount += buffer.getArrayLength();
            if (gridBuffer == null) {
                return;
            }
            int start = cursorY*(gridWidth+1) + cursorX;
            for (int i = 0; i < buffer.getArrayLength(); i++) {
                WindowTextGrid.this.putChar((char) (buffer.getByteElementAt(i) & 255), false);
            }
            int end = cursorY*(gridWidth+1) + cursorX;
            if (start < gridBuffer.length() && end > 0 && end > start) {
                start = Math.max(0, start);
                end = Math.min(end, gridBuffer.length());
                cleanGridBufferSpans(start, end);
                styleText(gridBuffer, start, end, currentStyle, activity.getStyleForegroundColor(getType(), currentStyle), activity.getStyleBackgroundColor(getType(), currentStyle), linkVal);
            }
        }

        @Override
        public void putCharUni(int ch) throws IOException {
            super.putCharUni(ch);
            writeCount++;
            if (Character.charCount(ch) == 1) {
                WindowTextGrid.this.putChar((char) ch, true);
            } else {
                WindowTextGrid.this.putChar('?', true);
            }
        }

        @Override
        public void putStringUni(UnicodeString string) throws IOException {
            super.putStringUni(string);
            writeCount += string.codePointCount();
            if (gridBuffer == null) {
                return;
            }
            int start = cursorY*(gridWidth+1) + cursorX;
            for (int i = 0; i < string.codePointCount(); i++) {
                int ch = string.codePointAt(i);
                if (Character.charCount(ch) == 1) {
                    WindowTextGrid.this.putChar((char) ch, false);
                } else {
                    WindowTextGrid.this.putChar('?', false);
                }
            }
            int end = cursorY*(gridWidth+1) + cursorX;
            if (start < gridBuffer.length() && end > 0 && end > start) {
                start = Math.max(0, start);
                end = Math.min(end, gridBuffer.length());
                cleanGridBufferSpans(start, end);
                styleText(gridBuffer, start, end, currentStyle, activity.getStyleForegroundColor(getType(), currentStyle), activity.getStyleBackgroundColor(getType(), currentStyle), linkVal);
            }
        }

        @Override
        public void putBufferUni(GlkIntArray buffer) throws IOException {
            super.putBufferUni(buffer);
            writeCount += buffer.getArrayLength();
            if (gridBuffer == null) {
                return;
            }
            int start = cursorY*(gridWidth+1) + cursorX;
            for (int i = 0; i < buffer.getArrayLength(); i++) {
                int ch = buffer.getIntElementAt(i);
                if (Character.charCount(ch) == 1) {
                    WindowTextGrid.this.putChar((char) ch, false);
                } else {
                    WindowTextGrid.this.putChar('?', false);
                }
            }
            int end = cursorY*(gridWidth+1) + cursorX;
            if (start < gridBuffer.length() && end > 0 && end > start) {
                start = Math.max(0, start);
                end = Math.min(end, gridBuffer.length());
                cleanGridBufferSpans(start, end);
                styleText(gridBuffer, start, end, currentStyle, activity.getStyleForegroundColor(getType(), currentStyle), activity.getStyleBackgroundColor(getType(), currentStyle), linkVal);
            }
        }

        @Override
        public void setStyle(int style) {
            super.setStyle(style);
            currentStyle = style;
        }

        @Override
        public void setHyperlink(int linkVal) {
            super.setHyperlink(linkVal);
            WindowTextGrid.this.linkVal = linkVal;
        }
    };
}
