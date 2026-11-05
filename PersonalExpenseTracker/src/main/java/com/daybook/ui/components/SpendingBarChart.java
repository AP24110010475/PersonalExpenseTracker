package com.daybook.ui.components;

import com.daybook.ui.UITheme;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Editorial Bar Chart component for monthly income and expense comparison.
 * Adheres strictly to the warm neutral palette without blue, gradients, or rounded pills.
 */
public class SpendingBarChart extends JPanel {

    private Map<String, Double[]> data = new LinkedHashMap<>(); // Key: "YYYY-MM", Value: [Income, Expense]

    public SpendingBarChart() {
        setBackground(UITheme.COLOR_CONTENT_BG);
        setPreferredSize(new Dimension(500, 220));
    }

    public void setData(Map<String, Double[]> newData) {
        this.data = newData != null ? newData : new LinkedHashMap<>();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Padding
        int padLeft = 70;
        int padRight = 30;
        int padTop = 30;
        int padBottom = 40;

        int plotWidth = width - padLeft - padRight;
        int plotHeight = height - padTop - padBottom;

        if (plotWidth <= 0 || plotHeight <= 0) {
            g2.dispose();
            return;
        }

        // Find max value
        double maxVal = 1000.0;
        for (Double[] pair : data.values()) {
            if (pair[0] != null && pair[0] > maxVal) maxVal = pair[0];
            if (pair[1] != null && pair[1] > maxVal) maxVal = pair[1];
        }
        // Round up to nice number
        maxVal = Math.ceil(maxVal / 1000.0) * 1000.0;
        if (maxVal == 0) maxVal = 1000.0;

        // Draw horizontal grid lines (4 intervals)
        g2.setColor(UITheme.COLOR_BORDER);
        g2.setFont(UITheme.FONT_SMALL);
        FontMetrics fm = g2.getFontMetrics();

        for (int i = 0; i <= 4; i++) {
            double val = (maxVal / 4.0) * i;
            int y = padTop + plotHeight - (int) ((val / maxVal) * plotHeight);

            g2.setColor(new Color(0xE8, 0xE2, 0xD6));
            g2.drawLine(padLeft, y, width - padRight, y);

            g2.setColor(UITheme.COLOR_MUTED_TEXT);
            String label = UITheme.formatCompactCurrency(val);
            g2.drawString(label, padLeft - fm.stringWidth(label) - 8, y + 4);
        }

        // Draw base line
        g2.setColor(UITheme.COLOR_PRIMARY_TEXT);
        g2.drawLine(padLeft, padTop + plotHeight, width - padRight, padTop + plotHeight);

        // Draw Bars
        int count = data.size();
        if (count == 0) {
            g2.setColor(UITheme.COLOR_MUTED_TEXT);
            g2.setFont(UITheme.FONT_BODY);
            String empty = "No transaction data available yet";
            g2.drawString(empty, padLeft + (plotWidth - fm.stringWidth(empty)) / 2, padTop + plotHeight / 2);
            g2.dispose();
            return;
        }

        int groupWidth = plotWidth / count;
        int barWidth = Math.max(8, Math.min(22, (groupWidth - 24) / 2));

        int idx = 0;
        for (Map.Entry<String, Double[]> entry : data.entrySet()) {
            String monthKey = entry.getKey();
            Double[] pair = entry.getValue();
            double income = pair[0] != null ? pair[0] : 0.0;
            double expense = pair[1] != null ? pair[1] : 0.0;

            int groupCenterX = padLeft + (idx * groupWidth) + (groupWidth / 2);

            int incomeH = (int) ((income / maxVal) * plotHeight);
            int expenseH = (int) ((expense / maxVal) * plotHeight);

            int incomeX = groupCenterX - barWidth - 2;
            int incomeY = padTop + plotHeight - incomeH;

            int expenseX = groupCenterX + 2;
            int expenseY = padTop + plotHeight - expenseH;

            // Income bar (Muted Olive #777D5A)
            g2.setColor(UITheme.COLOR_OLIVE);
            g2.fillRect(incomeX, incomeY, barWidth, incomeH);
            g2.setColor(UITheme.COLOR_PRIMARY_TEXT);
            g2.drawRect(incomeX, incomeY, barWidth, incomeH);

            // Expense bar (Soft Terracotta #B9785B)
            g2.setColor(UITheme.COLOR_TERRACOTTA);
            g2.fillRect(expenseX, expenseY, barWidth, expenseH);
            g2.setColor(UITheme.COLOR_PRIMARY_TEXT);
            g2.drawRect(expenseX, expenseY, barWidth, expenseH);

            // Month Label
            g2.setColor(UITheme.COLOR_PRIMARY_TEXT);
            g2.setFont(UITheme.FONT_SMALL);
            String displayMonth = formatMonthLabel(monthKey);
            int textW = g2.getFontMetrics().stringWidth(displayMonth);
            g2.drawString(displayMonth, groupCenterX - (textW / 2), padTop + plotHeight + 18);

            idx++;
        }

        // Legend at top-right
        int legendX = width - padRight - 160;
        int legendY = 12;

        g2.setFont(UITheme.FONT_SMALL);
        // Olive
        g2.setColor(UITheme.COLOR_OLIVE);
        g2.fillRect(legendX, legendY, 10, 10);
        g2.setColor(UITheme.COLOR_PRIMARY_TEXT);
        g2.drawRect(legendX, legendY, 10, 10);
        g2.drawString("Income", legendX + 15, legendY + 9);

        // Terracotta
        g2.setColor(UITheme.COLOR_TERRACOTTA);
        g2.fillRect(legendX + 75, legendY, 10, 10);
        g2.setColor(UITheme.COLOR_PRIMARY_TEXT);
        g2.drawRect(legendX + 75, legendY, 10, 10);
        g2.drawString("Expense", legendX + 90, legendY + 9);

        g2.dispose();
    }

    private String formatMonthLabel(String yyyyMm) {
        try {
            String[] parts = yyyyMm.split("-");
            if (parts.length == 2) {
                int month = Integer.parseInt(parts[1]);
                String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                if (month >= 1 && month <= 12) {
                    return months[month - 1] + " '" + parts[0].substring(2);
                }
            }
        } catch (Exception ignored) {}
        return yyyyMm;
    }
}
