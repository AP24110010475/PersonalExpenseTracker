package com.daybook.ui.components;

import com.daybook.ui.UITheme;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * Editorial Category-wise spending breakdown component.
 * Renders category labels, amounts, and sharp horizontal proportion meters.
 */
public class CategoryBreakdownPanel extends JPanel {

    private final JPanel container;

    public CategoryBreakdownPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.COLOR_CONTENT_BG);

        container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(UITheme.COLOR_CONTENT_BG);

        add(container, BorderLayout.CENTER);
    }

    public void updateData(Map<String, Double> categoryMap, double totalExpense) {
        container.removeAll();

        if (categoryMap == null || categoryMap.isEmpty() || totalExpense <= 0) {
            JLabel emptyLabel = new JLabel("No expense transactions recorded.");
            emptyLabel.setFont(UITheme.FONT_BODY);
            emptyLabel.setForeground(UITheme.COLOR_MUTED_TEXT);
            emptyLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
            container.add(emptyLabel);
        } else {
            for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
                String category = entry.getKey();
                double amount = entry.getValue();
                double pct = (amount / totalExpense) * 100.0;

                container.add(createCategoryRow(category, amount, pct));
                container.add(Box.createVerticalStrut(10));
            }
        }

        container.revalidate();
        container.repaint();
    }

    private JPanel createCategoryRow(String category, double amount, double percentage) {
        JPanel row = new JPanel(new BorderLayout(8, 4));
        row.setBackground(UITheme.COLOR_CONTENT_BG);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        // Top line: Name on left, Amount + % on right
        JPanel headerLine = new JPanel(new BorderLayout());
        headerLine.setBackground(UITheme.COLOR_CONTENT_BG);

        JLabel nameLbl = new JLabel(category);
        nameLbl.setFont(UITheme.FONT_BODY_MEDIUM);
        nameLbl.setForeground(UITheme.COLOR_PRIMARY_TEXT);

        JLabel amountLbl = new JLabel(String.format("%s (%.1f%%)", UITheme.formatCurrency(amount), percentage));
        amountLbl.setFont(UITheme.FONT_BODY);
        amountLbl.setForeground(UITheme.COLOR_MUTED_TEXT);

        headerLine.add(nameLbl, BorderLayout.WEST);
        headerLine.add(amountLbl, BorderLayout.EAST);

        // Bottom line: Sharp thin progress meter
        JComponent bar = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                int w = getWidth();
                int h = getHeight();

                // Track background
                g2.setColor(UITheme.COLOR_LIGHT_BEIGE);
                g2.fillRect(0, 0, w, h);
                g2.setColor(UITheme.COLOR_BORDER);
                g2.drawRect(0, 0, w - 1, h - 1);

                // Fill portion
                int fillW = (int) ((percentage / 100.0) * w);
                if (fillW > 0) {
                    g2.setColor(UITheme.COLOR_TERRACOTTA);
                    g2.fillRect(0, 0, fillW, h);
                }
                g2.dispose();
            }
        };
        bar.setPreferredSize(new Dimension(100, 6));

        row.add(headerLine, BorderLayout.NORTH);
        row.add(bar, BorderLayout.SOUTH);

        return row;
    }
}
