package com.tstorm.seed;

import java.util.UUID;
import java.util.regex.Pattern;

public class BaconActor extends Actor {

    static final Pattern YEAR_PATTERN = Pattern.compile("\\([0-9|?]{4}+(.[IVXLCDM]*)?\\)");

    public BaconActor(Name n) {
        super(n);
    }

    @Override
    public UUID getUUID() {
        return UUID.randomUUID();
    }

    @Override
    public Pattern getPattern() {
        return YEAR_PATTERN;
    }
}
