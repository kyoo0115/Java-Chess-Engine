package com.chess.engine.gui;

import java.awt.Color;

public enum BoardTheme {
    CLASSIC("Classic", "#F0D9B5", "#B58863", "#F6F669", "#CDD16E", "#AABA44"),
    GREEN("Green", "#FFFFDD", "#86A666", "#F6F669", "#B8CF67", "#8FAE36"),
    BLUE("Blue", "#DEE3E6", "#788A9B", "#EAF04E", "#A2B870", "#7D9F3A");

    final String label;
    final Color light, dark, selected, lastLight, lastDark;

    BoardTheme(String label, String light, String dark,
               String selected, String lastLight, String lastDark) {
        this.label = label;
        this.light = Color.decode(light);
        this.dark = Color.decode(dark);
        this.selected = Color.decode(selected);
        this.lastLight = Color.decode(lastLight);
        this.lastDark = Color.decode(lastDark);
    }

    public static BoardTheme fromName(final String name) {
        for (final BoardTheme t : values()) {
            if (t.name().equals(name)) return t;
        }
        return CLASSIC;
    }
}
