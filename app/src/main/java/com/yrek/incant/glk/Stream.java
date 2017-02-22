package com.yrek.incant.glk;

import java.io.Serializable;

import com.yrek.ifstd.glk.GlkStream;

abstract class Stream extends GlkStream implements Serializable {
    Stream(int rock) {
        super(rock);
    }
}
