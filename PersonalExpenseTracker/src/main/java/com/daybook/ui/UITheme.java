package com.daybook.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Design system and color constants for DAYBOOK.
 * Strictly adheres to the warm neutral editorial aesthetic:
 * - Square corners and sharp edges
 * - Thin 1px subtle borders
 * - Zero blue highlights or accents
 * - Standard SansSerif typography
 */
public final class UITheme {

    // Color Palette
    public static final Color COLOR_MAIN_BG     = new Color(0xF4, 0xF0, 0xE8); // #F4F0E8
    public static final Color COLOR_CONTENT_BG  = new Color(0xFB, 0xF9, 0xF4); // #FBF9F4
    public static final Color COLOR_SIDEBAR_BG  = new Color(0xE8, 0xE0, 0xD3); // #E8E0D3
    public static final Color COLOR_PRIMARY_TEXT= new Color(0x29, 0x28, 0x20); // #292820
    public static final Color COLOR_MUTED_TEXT  = new Color(0x77, 0x72, 0x67); // #777267
    public static final Color COLOR_BORDER      = new Color(0xD8, 0xD0, 0xC4); // #D8D0C4
    public static final Color COLOR_OLIVE       = new Color(0x77, 0x7D, 0x5A); // #777D5A (Income)
    public static final Color COLOR_TERRACOTTA  = new Color(0xB9, 0x78, 0x5B); // #B9785B (Expense)
    public static final Color COLOR_LIGHT_BEIGE = new Color(0xED, 0xE5, 0xD9); // #EDE5D9
    public static final Color COLOR_SIDEBAR_HOVER = new Color(0xDF, 0xD7, 0xCA);
    public static final Color COLOR_SIDEBAR_ACTIVE = new Color(0xD4, 0xCB, 0xBD);

    // Typography (Standard SansSerif / Arial)
    public static final Font FONT_TITLE         = new Font("SansSerif", Font.BOLD, 18);
    public static final Font FONT_SUBTITLE      = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font FONT_HEADER        = new Font("SansSerif", Font.BOLD, 15);
    public static final Font FONT_BODY          = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_BODY_MEDIUM   = new Font("SansSerif", Font.BOLD, 13);
    public static final Font FONT_LABEL         = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font FONT_SMALL         = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font FONT_BIG_NUMBER    = new Font("SansSerif", Font.BOLD, 22);

    // Number format for Indian Rupees (₹)
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("₹#,##,##0.00");
    private static final DecimalFormat COMPACT_CURRENCY = new DecimalFormat("₹#,##,##0");

    private UITheme() {}

    public static String formatCurrency(double amount) {
        return CURRENCY_FORMAT.format(amount);
    }

    public static String formatCompactCurrency(double amount) {
        return COMPACT_CURRENCY.format(amount);
    }

    // Standard square 1px line border
    public static Border createBorder() {
        return new LineBorder(COLOR_BORDER, 1);
    }

    public static Border createPaddedBorder(int top, int left, int bottom, int right) {
        return new CompoundBorder(
                new LineBorder(COLOR_BORDER, 1),
                new EmptyBorder(top, left, bottom, right)
        );
    }

    public static JButton createButton(String text, boolean isPrimary) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY_MEDIUM);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(createPaddedBorder(7, 14, 7, 14));

        if (isPrimary) {
            btn.setBackground(COLOR_PRIMARY_TEXT);
            btn.setForeground(COLOR_CONTENT_BG);
        } else {
            btn.setBackground(COLOR_CONTENT_BG);
            btn.setForeground(COLOR_PRIMARY_TEXT);
        }

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (isPrimary) {
                    btn.setBackground(new Color(0x40, 0x3E, 0x34));
                } else {
                    btn.setBackground(COLOR_LIGHT_BEIGE);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (isPrimary) {
                    btn.setBackground(COLOR_PRIMARY_TEXT);
                } else {
                    btn.setBackground(COLOR_CONTENT_BG);
                }
            }
        });

        return btn;
    }

    public static JTextField createTextField() {
        JTextField field = new JTextField();
        field.setFont(FONT_BODY);
        field.setBackground(COLOR_CONTENT_BG);
        field.setForeground(COLOR_PRIMARY_TEXT);
        field.setCaretColor(COLOR_PRIMARY_TEXT);
        field.setBorder(createPaddedBorder(6, 8, 6, 8));
        return field;
    }

    @SuppressWarnings("unchecked")
    public static <T> JComboBox<T> createComboBox(T[] items) {
        JComboBox<T> combo = new JComboBox<>(items);
        combo.setFont(FONT_BODY);
        combo.setBackground(COLOR_CONTENT_BG);
        combo.setForeground(COLOR_PRIMARY_TEXT);
        combo.setBorder(createPaddedBorder(4, 6, 4, 6));
        return combo;
    }

    public static JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(COLOR_CONTENT_BG);
        panel.setBorder(createPaddedBorder(14, 16, 14, 16));
        return panel;
    }
}
