package com.chess.engine.gui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class UITheme {

    // --- Light Theme Colors ---
    public static final Color LIGHT_APP_BG = new Color(245, 247, 251);
    public static final Color LIGHT_SIDEBAR_BG = new Color(245, 247, 251);
    public static final Color LIGHT_CARD_BG = new Color(255, 255, 255);
    public static final Color LIGHT_CARD_BORDER = new Color(230, 235, 242);
    public static final Color LIGHT_TEXT_PRIMARY = new Color(28, 33, 44);
    public static final Color LIGHT_TEXT_SECONDARY = new Color(125, 133, 146);
    public static final Color LIGHT_TEXT_MUTED = new Color(165, 173, 186);
    public static final Color LIGHT_ACCENT_BLUE = new Color(59, 130, 246);
    public static final Color LIGHT_ACCENT_HOVER = new Color(37, 99, 235);
    public static final Color LIGHT_ACCENT_ACTIVE_BG = new Color(235, 242, 255);
    public static final Color LIGHT_ACTIVE_GREEN = new Color(34, 197, 94);
    public static final Color LIGHT_ROW_ALT = new Color(250, 252, 254);
    public static final Color LIGHT_BOARD_FRAME_BG = new Color(255, 255, 255);
    // --- Dark Theme Colors ---
    public static final Color DARK_APP_BG = new Color(13, 17, 26);
    public static final Color DARK_SIDEBAR_BG = new Color(13, 17, 26);
    public static final Color DARK_CARD_BG = new Color(20, 26, 38);
    public static final Color DARK_CARD_BORDER = new Color(32, 42, 60);
    public static final Color DARK_TEXT_PRIMARY = new Color(240, 243, 248);
    public static final Color DARK_TEXT_SECONDARY = new Color(145, 158, 176);
    public static final Color DARK_TEXT_MUTED = new Color(90, 103, 122);
    public static final Color DARK_ACCENT_BLUE = new Color(59, 130, 246);
    public static final Color DARK_ACCENT_HOVER = new Color(37, 99, 235);
    public static final Color DARK_ACCENT_ACTIVE_BG = new Color(28, 44, 76);
    public static final Color DARK_ACTIVE_GREEN = new Color(34, 197, 94);
    public static final Color DARK_ROW_ALT = new Color(24, 31, 46);
    public static final Color DARK_BOARD_FRAME_BG = new Color(20, 26, 38);
    private static final List<Runnable> listeners = new ArrayList<>();
    private static Mode currentMode = Mode.LIGHT;

    // Dynamic Getters based on currentMode
    public static boolean isDark() {
        return currentMode == Mode.DARK;
    }

    public static Mode getMode() {
        return currentMode;
    }

    public static void setMode(Mode mode) {
        currentMode = mode;
        for (Runnable r : listeners) {
            try {
                r.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void addThemeListener(Runnable listener) {
        listeners.add(listener);
    }

    public static Color getAppBg() {
        return isDark() ? DARK_APP_BG : LIGHT_APP_BG;
    }

    public static Color getSidebarBg() {
        return isDark() ? DARK_SIDEBAR_BG : LIGHT_SIDEBAR_BG;
    }

    public static Color getCardBg() {
        return isDark() ? DARK_CARD_BG : LIGHT_CARD_BG;
    }

    public static Color getCardBorder() {
        return isDark() ? DARK_CARD_BORDER : LIGHT_CARD_BORDER;
    }

    public static Color getTextPrimary() {
        return isDark() ? DARK_TEXT_PRIMARY : LIGHT_TEXT_PRIMARY;
    }

    public static Color getTextSecondary() {
        return isDark() ? DARK_TEXT_SECONDARY : LIGHT_TEXT_SECONDARY;
    }

    public static Color getTextMuted() {
        return isDark() ? DARK_TEXT_MUTED : LIGHT_TEXT_MUTED;
    }

    public static Color getAccentBlue() {
        return isDark() ? DARK_ACCENT_BLUE : LIGHT_ACCENT_BLUE;
    }

    public static Color getAccentHover() {
        return isDark() ? DARK_ACCENT_HOVER : LIGHT_ACCENT_HOVER;
    }

    public static Color getAccentActiveBg() {
        return isDark() ? DARK_ACCENT_ACTIVE_BG : LIGHT_ACCENT_ACTIVE_BG;
    }

    public static Color getActiveGreen() {
        return isDark() ? DARK_ACTIVE_GREEN : LIGHT_ACTIVE_GREEN;
    }

    public static Color getRowAlt() {
        return isDark() ? DARK_ROW_ALT : LIGHT_ROW_ALT;
    }

    public static Color getBoardFrameBg() {
        return isDark() ? DARK_BOARD_FRAME_BG : LIGHT_BOARD_FRAME_BG;
    }

    public static Color getControlBtnBg() {
        return isDark() ? new Color(27, 35, 50) : new Color(240, 243, 249);
    }

    public static Color getControlBtnBorder() {
        return isDark() ? new Color(38, 48, 68) : new Color(225, 230, 240);
    }

    public static Color getToggleTrackOff() {
        return isDark() ? new Color(42, 52, 72) : new Color(203, 213, 225);
    }

    public static Color getToggleTrackOn() {
        return new Color(59, 130, 246);
    }

    public static Color getArrowColor() {
        return new Color(245, 166, 35, 200);
    }

    public enum Mode {
        LIGHT,
        DARK
    }
}
