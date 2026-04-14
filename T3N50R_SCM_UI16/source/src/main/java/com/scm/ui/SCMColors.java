package com.scm.ui;

import java.awt.*;

/**
 * SCM Application – Luxury Dark UI Colour Palette & Design Tokens
 * Colour philosophy: Deep navy/charcoal background, gold/amber accents,
 * soft white text, contextual status colours.
 */
public final class SCMColors {

    private SCMColors() {}

    // ── Core Backgrounds ──────────────────────────────────────────────────────
    public static final Color BG_PRIMARY       = new Color(0x0D, 0x11, 0x1A); // near-black navy
    public static final Color BG_SECONDARY     = new Color(0x13, 0x1A, 0x2B); // deep navy
    public static final Color BG_CARD          = new Color(0x1A, 0x24, 0x3A); // card navy
    public static final Color BG_PANEL         = new Color(0x1E, 0x29, 0x44); // panel navy
    public static final Color BG_SIDEBAR       = new Color(0x0B, 0x0F, 0x1A); // darkest sidebar
    public static final Color BG_HOVER         = new Color(0x24, 0x34, 0x55); // hover state
    public static final Color BG_SELECTED      = new Color(0x1F, 0x3A, 0x6E); // selected state
    public static final Color BG_INPUT         = new Color(0x10, 0x19, 0x30); // input fields
    public static final Color BG_TABLE_ROW_ALT = new Color(0x16, 0x20, 0x38); // alternating rows
    public static final Color BG_TABLE_HEADER  = new Color(0x0E, 0x18, 0x2E); // table header

    // ── Gold / Amber Accents (Luxury) ─────────────────────────────────────────
    public static final Color ACCENT_GOLD        = new Color(0xD4, 0xAF, 0x37); // classic gold
    public static final Color ACCENT_GOLD_LIGHT  = new Color(0xF0, 0xCC, 0x5A); // bright gold
    public static final Color ACCENT_AMBER       = new Color(0xFF, 0xB3, 0x00); // amber
    public static final Color ACCENT_COPPER      = new Color(0xB8, 0x73, 0x33); // copper/bronze

    // ── Text ──────────────────────────────────────────────────────────────────
    public static final Color TEXT_PRIMARY     = new Color(0xF0, 0xF4, 0xFF); // bright white-blue
    public static final Color TEXT_SECONDARY   = new Color(0x9A, 0xA8, 0xC5); // muted blue-grey
    public static final Color TEXT_MUTED       = new Color(0x5A, 0x6A, 0x90); // very muted
    public static final Color TEXT_GOLD        = ACCENT_GOLD;
    public static final Color TEXT_ON_GOLD     = new Color(0x0D, 0x11, 0x1A); // dark text on gold btn

    // ── Status / Semantic ─────────────────────────────────────────────────────
    public static final Color STATUS_SUCCESS   = new Color(0x00, 0xC8, 0x7A); // emerald green
    public static final Color STATUS_WARNING   = new Color(0xFF, 0xA5, 0x00); // orange
    public static final Color STATUS_ERROR     = new Color(0xFF, 0x3E, 0x55); // red
    public static final Color STATUS_INFO      = new Color(0x3A, 0x9B, 0xFF); // blue
    public static final Color STATUS_NEUTRAL   = new Color(0x6A, 0x7A, 0xA0); // grey

    // ── Borders & Dividers ────────────────────────────────────────────────────
    public static final Color BORDER_DEFAULT   = new Color(0x25, 0x35, 0x55);
    public static final Color BORDER_ACCENT    = new Color(0xD4, 0xAF, 0x37, 180);
    public static final Color BORDER_FOCUS     = ACCENT_GOLD;

    // ── Charts ────────────────────────────────────────────────────────────────
    public static final Color[] CHART_PALETTE = {
        new Color(0xD4, 0xAF, 0x37), // gold
        new Color(0x3A, 0x9B, 0xFF), // blue
        new Color(0x00, 0xC8, 0x7A), // green
        new Color(0xFF, 0x6B, 0x6B), // red
        new Color(0xBB, 0x86, 0xFC), // purple
        new Color(0xFF, 0xA5, 0x00), // orange
        new Color(0x00, 0xE5, 0xFF), // cyan
        new Color(0xFF, 0x72, 0xC0), // pink
    };

    // ── Gradients (for header/splash) ─────────────────────────────────────────
    public static final GradientPaint GRADIENT_HEADER = new GradientPaint(
        0, 0,  new Color(0x0D, 0x11, 0x1A),
        1, 0,  new Color(0x1A, 0x24, 0x3A)
    );

    // ── Typography Sizes ──────────────────────────────────────────────────────
    public static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD,  22);
    public static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD,  16);
    public static final Font FONT_SUBHEAD = new Font("Segoe UI", Font.BOLD,  13);
    public static final Font FONT_BODY    = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_MONO    = new Font("JetBrains Mono", Font.PLAIN, 12);
    public static final Font FONT_BADGE   = new Font("Segoe UI", Font.BOLD,  10);

    // ── Icon-emoji shortcuts ──────────────────────────────────────────────────
    public static final String ICON_OK      = "✅";
    public static final String ICON_WARN    = "⚠";
    public static final String ICON_ERROR   = "❌";
    public static final String ICON_INFO    = "ℹ";
    public static final String ICON_LOCK    = "🔒";
    public static final String ICON_USER    = "👤";
    public static final String ICON_DASH    = "📊";
    public static final String ICON_INV     = "📦";
    public static final String ICON_ORDER   = "🛒";
    public static final String ICON_LOGIS   = "🚚";
    public static final String ICON_PRICE   = "💰";
    public static final String ICON_FORECAST= "📈";
    public static final String ICON_NOTIF   = "🔔";
    public static final String ICON_SETTINGS= "⚙";
    public static final String ICON_LOGOUT  = "🚪";
}
