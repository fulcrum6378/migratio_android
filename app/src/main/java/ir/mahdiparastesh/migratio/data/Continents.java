package ir.mahdiparastesh.migratio.data;

import ir.mahdiparastesh.migratio.R;

@SuppressWarnings("unused")
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
