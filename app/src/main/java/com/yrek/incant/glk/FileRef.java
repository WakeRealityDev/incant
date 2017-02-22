package com.yrek.incant.glk;

import java.io.File;
import java.io.Serializable;

import com.yrek.ifstd.glk.GlkFile;

class FileRef extends GlkFile implements Serializable {
    private static final long serialVersionUID = 0L;
    final File file;
    final int usage;
    final int mode;

    FileRef(File file, int usage, int mode, int rock) {
        super(rock);
        this.file = file;
        this.usage = usage;
        this.mode = mode;
    }

    @Override
    public void delete() {
        file.delete();
    }

    @Override
    public boolean exists() {
        return file.exists();
    }
}
