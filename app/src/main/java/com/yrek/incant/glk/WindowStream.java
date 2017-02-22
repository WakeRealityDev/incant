package com.yrek.incant.glk;

import java.io.Serializable;

import com.yrek.ifstd.glk.GlkWindowStream;

abstract class WindowStream extends GlkWindowStream implements Serializable {
    WindowStream(Window window) {
        super(window);
    }
}
