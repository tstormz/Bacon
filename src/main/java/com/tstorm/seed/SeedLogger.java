package com.tstorm.seed;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

class SeedLogger extends BufferedWriter {
    private int logInterval, i;

    public SeedLogger(String filename, int logInterval) throws IOException {
        super(new FileWriter(new File(filename + ".log")));
        this.logInterval = logInterval;
        i = 1;
    }

    public void log(String s) throws IOException {
        if (i++ == logInterval) {
            write(s);
            newLine();
            flush();
            i = 1;
        }
    }

    public void log(Actor a) throws IOException {
        if (i++ == logInterval) {
            write(a.getDisplayName() + " -> name: " + a.getFormattedName() + ";");
            if (a.getNickName().isPresent()) {
                write("nickname: " + a.getNickName().get() + ";");
            }
            if (a.getSuffix().isPresent()) {
                write("suffix: " + a.getSuffix().get());
            }
            newLine();
            flush();
            i = 1;
        }
    }

    public void error(String error) throws IOException {
        write("ERROR: " + error);
        newLine();
        flush();
    }
}
