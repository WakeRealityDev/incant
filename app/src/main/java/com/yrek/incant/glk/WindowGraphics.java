package com.yrek.incant.glk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.io.IOException;

import com.yrek.ifstd.glk.GlkEvent;
import com.yrek.ifstd.glk.GlkWindow;
import com.yrek.ifstd.glk.GlkWindowSize;

class WindowGraphics extends Window {
    private static final long serialVersionUID = 0L;
    private static final String TAG = WindowGraphics.class.getSimpleName();
    private static final Paint PAINT = new Paint();

    private transient Bitmap graphicsBuffer = null;
    private boolean dirty = false;
    private boolean hasPendingArrangeEvent = false;
    private boolean hasPendingRedrawEvent = false;
    private int backgroundColor = Color.WHITE;
    private boolean mouseEventRequested = false;
    private int mouseX = -1;
    private int mouseY = -1;

    WindowGraphics(int rock, GlkActivity activity) {
        super(rock, activity);
    }

    @Override
    View createView(Context context) {
        final GestureDetector gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onSingleTapConfirmed(MotionEvent e) {
                if (mouseEventRequested) {
                    mouseX = (int) e.getX();
                    mouseY = (int) e.getY();
                    activity.input.cancelInput();
                    return true;
                }
                return false;
            }
        });
        final ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(activity, onScaleGestureListener());
        return new View(context) {
            @Override protected void onDraw(Canvas canvas) {
                Bitmap buffer = graphicsBuffer;
                if (buffer != null) {
                    canvas.drawBitmap(buffer, 0f, 0f, PAINT);
                }
            }
            @Override public boolean onTouchEvent(MotionEvent motionEvent) {
                return gestureDetector.onTouchEvent(motionEvent) || scaleGestureDetector.onTouchEvent(motionEvent) || super.onTouchEvent(motionEvent);
            }
        };
    }

    @Override
    boolean hasPendingEvent() {
        return hasPendingArrangeEvent || hasPendingRedrawEvent || (mouseEventRequested && mouseX >= 0);
    }

    @Override
    GlkEvent getEvent(long timeout, boolean polling) throws InterruptedException {
        Log.d(TAG,"getEvent:arrangeEvent="+hasPendingArrangeEvent+",redrawEvent="+hasPendingRedrawEvent);
        if (hasPendingArrangeEvent) {
            hasPendingArrangeEvent = false;
            return new GlkEvent(GlkEvent.TypeArrange, this, 0, 0);
        } else if (hasPendingRedrawEvent) {
            hasPendingRedrawEvent = false;
            return new GlkEvent(GlkEvent.TypeRedraw, this, 0, 0);
        } else if (polling) {
            return null;
        } else if (mouseEventRequested && mouseX >= 0) {
            int x = mouseX;
            int y = mouseY;
            mouseEventRequested = false;
            mouseX = -1;
            mouseY = -1;
            return new GlkEvent(GlkEvent.TypeMouseInput, this, x, y);
        }
        return null;
    }

    @Override
    boolean updatePendingOutput(Runnable continueOutput, boolean doSpeech) {
        Log.d(TAG,"updatePendingOutput:dirty="+dirty+",doSpeech="+doSpeech);
        if (dirty) {
            dirty = false;
            view.invalidate();
        }
        return false;
    }

    @Override
    void onWindowSizeChanged(int width, int height) {
        Log.d(TAG,"onWindowSizeChanged:"+width+"x"+height);
        if (width <= 0 || height <= 0) {
            graphicsBuffer = null;
            hasPendingArrangeEvent = true;
            return;
        }
        Bitmap buffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        buffer.eraseColor(backgroundColor);
        graphicsBuffer = buffer;
        hasPendingArrangeEvent = true;
        hasPendingRedrawEvent = true;
    }

    @Override
    void clearPendingArrangeEvent() {
        hasPendingArrangeEvent = false;
    }

    @Override
    void clearPendingRedrawEvent() {
        hasPendingRedrawEvent = false;
    }

    @Override
    public GlkWindowSize getSize() {
        if (graphicsBuffer == null) {
            return new GlkWindowSize(0, 0);
        } else {
            return new GlkWindowSize(graphicsBuffer.getWidth(), graphicsBuffer.getHeight());
        }
    }

    @Override
    public int getType() {
        return GlkWindow.TypeGraphics;
    }

    @Override
    public void clear() throws IOException {
        if (graphicsBuffer == null) {
            return;
        }
        graphicsBuffer.eraseColor(backgroundColor);
        dirty = true;
    }

    @Override
    public void requestMouseEvent() {
        if (!mouseEventRequested) {
            mouseEventRequested = true;
            mouseX = -1;
            mouseY = -1;
        }
    }

    @Override
    public void cancelMouseEvent() {
        mouseEventRequested = false;
        mouseX = -1;
        mouseY = -1;
    }

    @Override
    public boolean drawImage(int resourceId, int val1, int val2) throws IOException {
        if (graphicsBuffer == null) {
            return true;
        }
        Bitmap image = activity.getImageResource(resourceId);
        if (image == null) {
            return false;
        }
        drawImage(image, val1, val2);
        dirty = true;
        return true;
    }

    @Override
    public boolean drawScaledImage(int resourceId, int val1, int val2, int width, int height) throws IOException {
        if (graphicsBuffer == null) {
            return true;
        }
        Bitmap image = activity.getImageResource(resourceId);
        if (image == null) {
            return false;
        }
        drawImage(Bitmap.createScaledBitmap(image, width, height, true), val1, val2);
        dirty = true;
        return true;
    }

    private void drawImage(Bitmap bitmap, int left, int top) {
        for (int y = Math.max(0, -top); y < Math.min(bitmap.getHeight(), graphicsBuffer.getHeight()-top); y++) {
            for (int x = Math.max(0, -left); x < Math.min(bitmap.getWidth(), graphicsBuffer.getWidth()-left); x++) {
                graphicsBuffer.setPixel(left+x, top+y, bitmap.getPixel(x, y));
            }
        }
    }

    @Override
    public void eraseRect(int left, int top, int width, int height) {
        fillRect(backgroundColor, left, top, width, height);
    }

    @Override
    public void fillRect(int color, int left, int top, int width, int height) {
        if (graphicsBuffer == null) {
            return;
        }
        color |= 0xff000000;
        for (int y = Math.max(0,top); y < Math.min(top+height,graphicsBuffer.getHeight()); y++) {
            for (int x = Math.max(0,left); x < Math.min(left+width,graphicsBuffer.getWidth()); x++) {
                graphicsBuffer.setPixel(x, y, color);
            }
        }
        dirty = true;
    }

    @Override
    public void setBackgroundColor(int color) {
        backgroundColor = 0xff000000 | color;
    }
}
