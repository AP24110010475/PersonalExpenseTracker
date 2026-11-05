package com.daybook.ui;

import com.daybook.service.TransactionService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Main application window for DAYBOOK.
 * Integrates the fixed left sidebar and card-layout content area (1200 x 750).
 */
public class MainFrame extends JFrame {

    public static final String PAGE_OVERVIEW = "Overview";
    public static final String PAGE_TRANSACTIONS = "Transactions";
    public static final String PAGE_ADD_TRANSACTION = "Add Transaction";
    public static final String PAGE_REPORTS = "Reports";
    public static final String PAGE_SETTINGS = "Settings";

    private final TransactionService transactionService;

    // Layout components
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private String currentPage = PAGE_OVERVIEW;

    // Sub-panels
    private DashboardPanel dashboardPanel;
    private TransactionPanel transactionPanel;
    private AddTransactionPanel addTransactionPanel;
    private ReportsPanel reportsPanel;
    private SettingsPanel settingsPanel;

    // Sidebar navigation buttons
    private final Map<String, JButton> navButtons = new HashMap<>();

    public MainFrame() {
        this.transactionService = new TransactionService();

        setTitle("DAYBOOK – Personal Finance Manager");
        setSize(1200, 750);
        setMinimumSize(new Dimension(1000, 650));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center on screen

        initUI();
    }

    private void initUI() {
        // Base container layout
        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(UITheme.COLOR_MAIN_BG);

        // 1. Fixed Left Sidebar
        JPanel sidebar = createSidebar();
        rootPanel.add(sidebar, BorderLayout.WEST);

        // 2. Main Content Area with CardLayout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(UITheme.COLOR_MAIN_BG);

        dashboardPanel = new DashboardPanel(transactionService, this);
        transactionPanel = new TransactionPanel(transactionService, this);
        addTransactionPanel = new AddTransactionPanel(transactionService, this);
        reportsPanel = new ReportsPanel(transactionService, this);
        settingsPanel = new SettingsPanel(transactionService, this);

        contentPanel.add(dashboardPanel, PAGE_OVERVIEW);
        contentPanel.add(transactionPanel, PAGE_TRANSACTIONS);
        contentPanel.add(addTransactionPanel, PAGE_ADD_TRANSACTION);
        contentPanel.add(reportsPanel, PAGE_REPORTS);
        contentPanel.add(settingsPanel, PAGE_SETTINGS);

        rootPanel.add(contentPanel, BorderLayout.CENTER);

        setContentPane(rootPanel);

        // Initial navigation
        navigateTo(PAGE_OVERVIEW);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(240, 750));
        sidebar.setBackground(UITheme.COLOR_SIDEBAR_BG);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UITheme.COLOR_BORDER));

        // Brand Title Block
        JPanel brandBlock = new JPanel();
        brandBlock.setLayout(new BoxLayout(brandBlock, BoxLayout.Y_AXIS));
        brandBlock.setBackground(UITheme.COLOR_SIDEBAR_BG);
        brandBlock.setBorder(new EmptyBorder(32, 24, 28, 24));

        JLabel titleLabel = new JLabel("DAYBOOK");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        titleLabel.setForeground(UITheme.COLOR_PRIMARY_TEXT);

        JLabel subtitleLabel = new JLabel("Personal Finance Manager");
        subtitleLabel.setFont(UITheme.FONT_SUBTITLE);
        subtitleLabel.setForeground(UITheme.COLOR_MUTED_TEXT);

        brandBlock.add(titleLabel);
        brandBlock.add(Box.createVerticalStrut(4));
        brandBlock.add(subtitleLabel);

        sidebar.add(brandBlock, BorderLayout.NORTH);

        // Navigation Items List
        JPanel navList = new JPanel();
        navList.setLayout(new BoxLayout(navList, BoxLayout.Y_AXIS));
        navList.setBackground(UITheme.COLOR_SIDEBAR_BG);
        navList.setBorder(new EmptyBorder(8, 16, 16, 16));

        String[] menuItems = {
                PAGE_OVERVIEW,
                PAGE_TRANSACTIONS,
                PAGE_ADD_TRANSACTION,
                PAGE_REPORTS,
                PAGE_SETTINGS
        };

        for (String item : menuItems) {
            JButton btn = createNavButton(item);
            navButtons.put(item, btn);
            navList.add(btn);
            navList.add(Box.createVerticalStrut(4));
        }

        sidebar.add(navList, BorderLayout.CENTER);

        // Footer Meta Information
        JPanel footerBlock = new JPanel();
        footerBlock.setLayout(new BoxLayout(footerBlock, BoxLayout.Y_AXIS));
        footerBlock.setBackground(UITheme.COLOR_SIDEBAR_BG);
        footerBlock.setBorder(new EmptyBorder(16, 24, 24, 24));

        JLabel versionLabel = new JLabel("Academic Project v1.0");
        versionLabel.setFont(UITheme.FONT_SMALL);
        versionLabel.setForeground(UITheme.COLOR_MUTED_TEXT);

        JLabel offlineLabel = new JLabel("● Local SQLite Mode");
        offlineLabel.setFont(UITheme.FONT_SMALL);
        offlineLabel.setForeground(UITheme.COLOR_OLIVE);

        footerBlock.add(versionLabel);
        footerBlock.add(Box.createVerticalStrut(2));
        footerBlock.add(offlineLabel);

        sidebar.add(footerBlock, BorderLayout.SOUTH);

        return sidebar;
    }

    private JButton createNavButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(UITheme.FONT_BODY);
        btn.setForeground(UITheme.COLOR_PRIMARY_TEXT);
        btn.setBackground(UITheme.COLOR_SIDEBAR_BG);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btn.setPreferredSize(new Dimension(208, 38));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 14, 8, 14));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!text.equals(currentPage)) {
                    btn.setBackground(UITheme.COLOR_SIDEBAR_HOVER);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!text.equals(currentPage)) {
                    btn.setBackground(UITheme.COLOR_SIDEBAR_BG);
                }
            }
        });

        btn.addActionListener(e -> navigateTo(text));

        return btn;
    }

    public void navigateTo(String pageName) {
        this.currentPage = pageName;
        cardLayout.show(contentPanel, pageName);

        // Update nav button active states
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            JButton btn = entry.getValue();
            if (entry.getKey().equals(pageName)) {
                btn.setBackground(UITheme.COLOR_SIDEBAR_ACTIVE);
                btn.setFont(UITheme.FONT_BODY_MEDIUM);
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 3, 0, 0, UITheme.COLOR_PRIMARY_TEXT),
                        new EmptyBorder(8, 11, 8, 14)
                ));
            } else {
                btn.setBackground(UITheme.COLOR_SIDEBAR_BG);
                btn.setFont(UITheme.FONT_BODY);
                btn.setBorder(new EmptyBorder(8, 14, 8, 14));
            }
        }

        // Trigger data refresh for destination panel
        switch (pageName) {
            case PAGE_OVERVIEW -> dashboardPanel.refreshData();
            case PAGE_TRANSACTIONS -> transactionPanel.refreshData();
            case PAGE_ADD_TRANSACTION -> addTransactionPanel.resetForm();
            case PAGE_REPORTS -> reportsPanel.refreshData();
            case PAGE_SETTINGS -> settingsPanel.refreshData();
        }
    }

    public void refreshAllPanels() {
        dashboardPanel.refreshData();
        transactionPanel.refreshData();
        reportsPanel.refreshData();
        settingsPanel.refreshData();
    }
}
