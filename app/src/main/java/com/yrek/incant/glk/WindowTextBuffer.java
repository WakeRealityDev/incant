package com.yrek.incant.glk;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.style.IconMarginSpan;
import android.text.style.ImageSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.Serializable;
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

class WindowTextBuffer extends Window {
    private static final long serialVersionUID = 0L;
    private static final String TAG = WindowTextBuffer.class.getSimpleName();

    ArrayDeque<Update> updates = new ArrayDeque<Update>();
    private boolean lineEventRequested = false;
    private boolean charEventRequested = false;
    private transient GlkByteArray lineEventBuffer = null;
    private boolean echoLineEvent = true;
    private int writeCount = 0;
    private boolean hyperlinkEventRequested = false;
    private int hyperlinkEventVal = 0;
    private Integer backgroundColor = null;
    private boolean midparagraph = false;

    WindowTextBuffer(int rock, GlkActivity activity) {
        super(rock, activity);
        backgroundColor = activity.getStyleBackgroundColor(getType(), GlkStream.StyleNormal);
    }

    @Override
    View createView(Context context) {
        final int[] scrollPosition = new int[1];
        final GestureDetector gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                Log.d(TAG,"onFling:vx="+velocityX+",vy="+velocityY);
                if (Math.abs(3.5f*velocityY) < -velocityX) {
                    activity.input.deleteWord();
                } else if (-Math.abs(3.5f*velocityY) > -velocityX) {
                    activity.input.enter();
                }
                return false;
            }

            TextView tappedTextView = null;
            int tappedOffset = 0;
            private void locateTap(MotionEvent e) {
                LinearLayout linearLayout = (LinearLayout) ((ScrollView) view).getChildAt(0);
                tappedTextView = null;
                int y = (int) e.getY() + scrollPosition[0];
                for (int i = 0; i < linearLayout.getChildCount(); i++) {
                    TextView textView = (TextView) linearLayout.getChildAt(i);
                    if (textView.getTop() <= y && textView.getBottom() >= y) {
                        tappedTextView = textView;
                        tappedOffset = textView.getOffsetForPosition(e.getX(), y - textView.getTop());
                        return;
                    }
                }
            }

            @Override public boolean onDoubleTap(MotionEvent e) {
                Log.d(TAG,"onDoubleTap:x="+e.getX()+",y="+e.getY()+","+(e.getY()+scrollPosition[0]));
                locateTap(e);
                if (tappedTextView == null) {
                    return false;
                }
                CharSequence text = tappedTextView.getText();
                int start = tappedOffset;
                while (start-1 > 0 && Character.isLetterOrDigit(text.charAt(start-1))) {
                    start--;
                }
                int end = tappedOffset;
                while (end < text.length() && Character.isLetterOrDigit(text.charAt(end))) {
                    end++;
                }
                Log.d(TAG,"tappedOffset="+tappedOffset+","+start+"-"+end+":"+text.subSequence(start, end));
                if (end > start) {
                    activity.input.pasteInput(new StringBuilder().append(text.subSequence(start, end)).append(' '));
                }
                return false;
            }

            @Override public boolean onSingleTapConfirmed(MotionEvent e) {
                Log.d(TAG,"onSingleTapConfirmed");
                if (hyperlinkEventRequested) {
                    locateTap(e);
                    if (tappedTextView == null) {
                        return false;
                    }
                    Editable editable = tappedTextView.getEditableText();
                    if (editable != null) {
                        HyperlinkSpan[] spans = editable.getSpans(tappedOffset, tappedOffset, HyperlinkSpan.class);
                        Log.d(TAG,"onSingleTapConfirmed:offset="+tappedOffset+",spans="+spans);
                        if (spans != null && spans.length > 0) {
                            hyperlinkEventVal = spans[0].linkVal;
                            Log.d(TAG,"onSingleTapConfirmed:offset="+tappedOffset+",spans="+spans+",hyperlinkEventVal="+hyperlinkEventVal);
                        }
                        activity.input.cancelInput();
                        return true;
                    }
                }
                return false;
            }
        });
        final ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(activity, onScaleGestureListener());
        ScrollView scrollView = new ScrollView(context) {
            @Override public boolean onTouchEvent(MotionEvent motionEvent) {
                boolean touchResult = gestureDetector.onTouchEvent(motionEvent) || scaleGestureDetector.onTouchEvent(motionEvent);
                return super.onTouchEvent(motionEvent);
            }
            @Override protected void onScrollChanged(int left, int top, int oldLeft, int oldTop) {
                super.onScrollChanged(left, top, oldLeft, oldTop);
                scrollPosition[0] = top;
            }
        };
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(linearLayout);
        if (backgroundColor != null) {
            scrollView.setBackgroundColor(0xff000000 | backgroundColor);
        }
        return scrollView;
    }

    @Override
    boolean hasPendingEvent() {
        return lineEventRequested || charEventRequested || (hyperlinkEventRequested && hyperlinkEventVal != 0);
    }

    @Override
    GlkEvent getEvent(long timeout, boolean polling) throws InterruptedException {
        if (polling) {
            return null;
        } else if (hyperlinkEventRequested && hyperlinkEventVal != 0) {
            int val = hyperlinkEventVal;
            hyperlinkEventRequested = false;
            hyperlinkEventVal = 0;
            return new GlkEvent(GlkEvent.TypeHyperlink, this, val, 0);
        } else if (lineEventRequested) {
            activity.hideProgressBar();
            activity.speech.resetSkip();
            String line = activity.input.getInput(timeout);
            if (line == null) {
                return null;
            }
            lineEventRequested = false;
            int count = lineEventBuffer == null ? 0 : Math.min(lineEventBuffer.getArrayLength(), line.length());
            for (int i = 0; i < count; i++) {
                lineEventBuffer.setByteElementAt(i, line.charAt(i));
            }
            if (echoLineEvent) {
                int saveStyle = updateQueueLast(true).style;
                try {
                    stream.setStyle(GlkStream.StyleInput);
                    stream.putString(line);
                    updates.peekLast().mute = true;
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
            Log.i(TAG, "[storyInput][singleChar] calling getCharInput with timeout " + timeout);
            int ch = activity.input.getCharInput(timeout);
            Log.i(TAG, "[storyInput][singleChar] after getCharInput got " + ch);
            if (ch == 0) {
                return null;
            }
            charEventRequested = false;
            activity.showProgressBar();
            return new GlkEvent(GlkEvent.TypeCharInput, this, ch, 0);
        } else {
            return null;
        }
    }

    // Run in IO thread.  Recursively descend window tree.
    // Return whether there is pending asynchronous output.
    // If there is asynchronous output pending, post continueOutput
    // to the IO thread once the asynchronous output has completed.
    // Examples of asynchronous output:
    // - text to speech
    // - waiting for the user to hit the next button (when output
    //   will scroll out of view (if text to speech is being skipped))
    @Override
    boolean updatePendingOutput(Runnable continueOutput, boolean doSpeech) {
        if (!doSpeech) {
            return false;
        }
        if (updates.size() == 0 || (updates.size() == 1 && updates.peekFirst().string.length() == 0)) {
            return false;
        }
        activity.hideProgressBar();
        LinearLayout linearLayout = (LinearLayout) ((ScrollView) view).getChildAt(0);
        TextView textView;
        if (midparagraph) {
            textView = (TextView) linearLayout.getChildAt(linearLayout.getChildCount()-1);
        } else {
            textView = new TextView(view.getContext());
            linearLayout.addView(textView);
            if (backgroundColor != null) {
                textView.setBackgroundColor(0xff000000 | backgroundColor);
            }
            if (linearLayout.getChildCount() > 50) {
                linearLayout.removeViewAt(0);
            }
        }
        int currentStyle = GlkStream.StyleNormal;
        int currentLinkVal = 0;
        SpannableStringBuilder screenOutput = new SpannableStringBuilder();
        StringBuilder speechOutput = new StringBuilder();
        int endParagraphState = 0;
        ImageLeftMarginSpan pendingSpan = null;
        loop:
        for (;;) {
            Update update = updates.peekFirst();
            if (update == null) {
                break;
            }
            if (update.clear) {
                if (speechOutput.length() > 0 || screenOutput.length() > 0) {
                    break;
                }
                textView.setText("");
                linearLayout.removeAllViews();
                linearLayout.addView(textView);
                update.clear = false;
            }
            if (update.flowBreak) {
                if (speechOutput.length() > 0 || screenOutput.length() > 0) {
                    break;
                }
                update.flowBreak = false;
                //...
            }
            currentStyle = update.style;
            currentLinkVal = update.linkVal;
            int start = screenOutput.length();
            // Break at paragraph breaks.
            for (int i = 0; i < update.string.length(); i++) {
                if (update.string.charAt(i) != '\n') {
                    endParagraphState = 1;
                } else {
                    switch (endParagraphState) {
                    case 0: break;
                    case 1: endParagraphState = 2; break;
                    case 2:
                        styleText(screenOutput, start, screenOutput.length(), currentStyle, update.foregroundColor, update.backgroundColor, currentLinkVal);
                        if (!update.mute) {
                            speechOutput.append(update.string, 0, i);
                        }
                        update.string.delete(0, i);
                        break loop;
                    }
                }
                screenOutput.append(update.string.charAt(i));
                if (pendingSpan != null) {
                    screenOutput.setSpan(pendingSpan, start, start + 1, 0);
                    pendingSpan = null;
                }
            }
            if (!update.mute) {
                speechOutput.append(update.string);
            }
            styleText(screenOutput, start, screenOutput.length(), currentStyle, update.foregroundColor, update.backgroundColor, currentLinkVal);
            if (update.image != null) {
                switch (update.imageAlign) {
                case GlkWindow.ImageAlignMarginLeft:
                    updates.removeFirst();
                    Update next = updates.peekFirst();
                    if (next == null || next.flowBreak || next.string.length() == 0 || next.string.charAt(0) == '\n' || next.mute) {
                        screenOutput.setSpan(new ImageSpan(activity, update.image, ImageSpan.ALIGN_BASELINE), start, screenOutput.length(), 0);
                    } else {
                        screenOutput.delete(start, screenOutput.length());
                        pendingSpan = new ImageLeftMarginSpan(update.image);
                    }
                    updates.addFirst(update);
                    break;
                default:
                    screenOutput.setSpan(new ImageSpan(activity, update.image, ImageSpan.ALIGN_BASELINE), start, screenOutput.length(), 0);
                    break;
                }
            }
            updates.removeFirst();
        }
        midparagraph = screenOutput.length() == 0 || endParagraphState < 2;
        if (screenOutput.length() > 0 && screenOutput.charAt(screenOutput.length()-1) == '\n') {
            screenOutput.delete(screenOutput.length()-1,screenOutput.length());
        }
        if (screenOutput.length() > 0) {
            textView.append(screenOutput);
            post(new Runnable() {
                @Override public void run() {
                    ((ScrollView) view).fullScroll(View.FOCUS_DOWN);
                }
            });
        }
        if ((currentStyle != GlkStream.StyleNormal || currentLinkVal != 0) && updates.size() == 0) {
            stream.setStyle(currentStyle);
            stream.setHyperlink(currentLinkVal);
        }
        if (speechOutput.length() == 0) {
            return false;
        } else {
            activity.speech.speak(SpeechMunger.fixOutput(speechOutput).toString(), continueOutput);
            return true;
        }
    }

    @Override
    TextAppearanceSpan getSpanForStyle(int style) {
        return new TextAppearanceSpan(activity, activity.main.getTextBufferStyle(style));
    }

    static class Update implements Serializable {
        private static final long serialVersionUID = 0L;

        boolean clear = false;
        boolean flowBreak = false;
        boolean mute = false;
        transient Bitmap image = null;
        int imageAlign = 0;
        int style = GlkStream.StyleNormal;
        int linkVal = 0;
        Integer foregroundColor = null;
        Integer backgroundColor = null;
        StringBuilder string = new StringBuilder();
    }

    Update updateQueueLast(boolean continueString) {
        Update latest = updates.peekLast();
        if (latest != null && (continueString || latest.string.length() == 0) && !latest.mute) {
            if (!colorHintChanged(latest.style, latest.foregroundColor, latest.backgroundColor)) {
                return latest;
            }
            if (latest.string.length() == 0) {
                latest.foregroundColor = activity.getStyleForegroundColor(getType(), latest.style);
                latest.backgroundColor = activity.getStyleBackgroundColor(getType(), latest.style);
                return latest;
            }
        }
        Update newLatest = new Update();
        if (latest != null) {
            newLatest.style = latest.style;
            newLatest.linkVal = latest.linkVal;
        }
        newLatest.foregroundColor = activity.getStyleForegroundColor(getType(), newLatest.style);
        newLatest.backgroundColor = activity.getStyleBackgroundColor(getType(), newLatest.style);
        updates.addLast(newLatest);
        return newLatest;
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
        Log.d(TAG,"screenSize="+((width-activity.charHMargin)/activity.charWidth)+"x"+((height-activity.charVMargin)/activity.charHeight));
        if (width > 0) {
            for (Update update : updates) {
                if (update.image != null && update.image.getWidth() > 0 && update.image.getWidth() > width) {
                    update.image = Bitmap.createScaledBitmap(update.image, width, update.image.getHeight()*width/update.image.getWidth(), true);
                }
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
        waitForTextMeasurer();
        // Do not waitForWindowMeasurer() because Glk.select() might not
        // have been called yet.
        if (activity.charWidth > 0 && activity.charHeight > 0 && windowWidth > 0 && windowHeight > 0) {
            return new GlkWindowSize((windowWidth - activity.charHMargin)/activity.charWidth, (windowHeight - activity.charVMargin)/activity.charHeight);
        } else {
            return new GlkWindowSize(40, 25);
        }
    }

    @Override
    public int getType() {
        return GlkWindow.TypeTextBuffer;
    }

    @Override
    public void clear() throws IOException {
        updateQueueLast(false).clear = true;
    }

    @Override
    public boolean styleDistinguish(int style1, int style2) {
        return activity.main.getTextBufferStyle(style1) != activity.main.getTextBufferStyle(style2);
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
    public void cancelHyperlinkEvent() {
        hyperlinkEventRequested = false;
        hyperlinkEventVal = 0;
    }

    @Override
    public boolean drawImage(int resourceId, int val1, int val2) throws IOException {
        Bitmap image = activity.getImageResource(resourceId);
        if (image == null) {
            return false;
        }
        if (windowWidth > 0 && image.getWidth() > 0 && windowWidth < image.getWidth()) {
            image = Bitmap.createScaledBitmap(image, windowWidth, image.getHeight()*windowWidth/image.getWidth(), true);
        }
        Update update = updateQueueLast(false);
        update.image = image;
        update.imageAlign = val1;
        update.mute = true;
        update.string.append("[image " + resourceId + "]");
        return true;
    }

    @Override
    public boolean drawScaledImage(int resourceId, int val1, int val2, int width, int height) throws IOException {
        if (width <= 0 || height <= 0) {
            return false;
        }
        Bitmap image = activity.getImageResource(resourceId);
        if (image == null) {
            return false;
        }
        if (windowWidth > 0 && width > windowWidth) {
            height = height*windowWidth/width;
            width = windowWidth;
        }
        Update update = updateQueueLast(false);
        update.image = Bitmap.createScaledBitmap(image, width, height, true);
        update.imageAlign = val1;
        update.mute = true;
        update.string.append("[image " + resourceId + "]");
        return true;
    }

    @Override
    public void flowBreak() {
        updateQueueLast(false).flowBreak = true;
    }


    @Override
    public void setEchoLineEvent(boolean echoLineEvent) {
        this.echoLineEvent = echoLineEvent;
    }

    
    private final WindowStream stream = new WindowStream(this) {
        private static final long serialVersionUID = 0L;

        @Override
        public void putChar(int ch) throws IOException {
            super.putChar(ch);
            writeCount++;
            updateQueueLast(true).string.append((char) (ch & 255));
        }

        @Override
        public void putString(CharSequence string) throws IOException {
            super.putString(string);
            writeCount += string.length();
            updateQueueLast(true).string.append(string);
        }

        @Override
        public void putBuffer(GlkByteArray buffer) throws IOException {
            super.putBuffer(buffer);
            writeCount += buffer.getArrayLength();
            Update update = updateQueueLast(true);
            for (int i = 0; i < buffer.getArrayLength(); i++) {
                update.string.append((char) (buffer.getByteElementAt(i) & 255));
            }
        }

        @Override
        public void putCharUni(int ch) throws IOException {
            super.putCharUni(ch);
            writeCount++;
            updateQueueLast(true).string.append(Character.toChars(ch));
        }

        @Override
        public void putStringUni(UnicodeString string) throws IOException {
            super.putStringUni(string);
            writeCount += string.codePointCount();
            updateQueueLast(true).string.append(string);
        }

        @Override
        public void putBufferUni(GlkIntArray buffer) throws IOException {
            super.putBufferUni(buffer);
            writeCount += buffer.getArrayLength();
            Update update = updateQueueLast(true);
            for (int i = 0; i < buffer.getArrayLength(); i++) {
                update.string.append(Character.toChars(buffer.getIntElementAt(i)));
            }
        }

        @Override
        public void setStyle(int style) {
            super.setStyle(style);
            if (updateQueueLast(true).style != style) {
                Update update = updateQueueLast(false);
                update.style = style;
                update.foregroundColor = activity.getStyleForegroundColor(getType(), style);
                update.backgroundColor = activity.getStyleBackgroundColor(getType(), style);
            }
        }

        @Override
        public void setHyperlink(int linkVal) {
            super.setHyperlink(linkVal);
            if (updateQueueLast(true).linkVal != linkVal) {
                Update update = updateQueueLast(false);
                update.linkVal = linkVal;
            }
        }
    };


    private class ImageLeftMarginSpan extends IconMarginSpan implements LeadingMarginSpan.LeadingMarginSpan2 {
        final int width;
        final int height;

        ImageLeftMarginSpan(Bitmap b) {
            super(b);
            width = b.getWidth();
            height = b.getHeight();
        }

        @Override
        public int getLeadingMargin(boolean first) {
            return first ? width : 0;
        }

        @Override
        public int getLeadingMarginLineCount() {
            if (activity.charHeight <= 0) {
                return 1;
            }
            return (height + activity.charHeight - 1) / activity.charHeight;
        }
    }
}
