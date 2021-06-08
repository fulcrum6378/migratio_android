package com.mahdiparastesh.migratio.data;

import com.mahdiparastesh.migratio.R;

@SuppressWarnings({"unused", "RedundantSuppression"})
public enum Continents {
    AFRICA(R.string.africa),
    ASIA(R.string.asia),
    EUROPE(R.string.europe),
    OCEANIA(R.string.oceania),
    AMERICA(R.string.america);

    public final int label;

    Continents(int label) {
        this.label = label;
    }
}