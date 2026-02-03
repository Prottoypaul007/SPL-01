import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Random;

public class KnapsackPanel extends JPanel {
    private JTextField nInput, capInput;
    private JTextArea inputArea, resultArea;
    private JLabel statusLabel, profitLabel, weightLabel, timeLabel, efficiencyLabel;
    
    // --- NEW: Class-level labels to prevent crashing during updates ---
    private JLabel allItemsLabel, selectedItemsLabel; 
    
    private JComboBox<String> algoSelector;
    private KnapsackVisPanel visPanel;
    private JTabbedPane tabbedPane;
    private JProgressBar progressBar;
    private JSlider zoomSlider;
    private JTable itemTable, selectedTable;
    
    // Data
    private int[] values;
    private int[] weights;
    private int[] selected;
    private int N = 0;
    private int Capacity = 0;
    private int totalProfit = 0;
    private int totalWeight = 0;
    private long executionTime = 0;

    // Design
    private final Font MAIN_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 16);
    private final Color ACCENT_COLOR = new Color(255, 152, 0); // Orange
    private final Color SUCCESS_COLOR = new Color(76, 175, 80); // Green
    private final Color SIDEBAR_BG = new Color(245, 245, 245);
    private final Color PRIMARY_COLOR = new Color(0, 120, 215);

    public KnapsackPanel() {
        setLayout(new BorderLayout(10, 10));

        // --- SIDEBAR (CONTROLS) ---
        JPanel leftPanel = createControlPanel();
        add(leftPanel, BorderLayout.WEST);

        // --- CENTER (TABBED PANE) ---
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(MAIN_FONT);
        
        // Tab 1: Visualization
        JPanel visTab = createVisualizationTab();
        tabbedPane.addTab("Visualization", new ImageIcon(), visTab, "Bar chart view of items");
        
        // Tab 2: Item Details
        JPanel itemsTab = createItemsTab();
        tabbedPane.addTab("All Items", new ImageIcon(), itemsTab, "Complete item list");
        
        // Tab 3: Selected Items
        JPanel selectedTab = createSelectedItemsTab();
        tabbedPane.addTab("Selected Items", new ImageIcon(), selectedTab, "Items in knapsack");
        
        // Tab 4: Input Data
        JPanel inputTab = createInputDataTab();
        tabbedPane.addTab("Input Data", new ImageIcon(), inputTab, "Edit item values and weights");
        
        add(tabbedPane, BorderLayout.CENTER);

        // --- BOTTOM STATUS BAR ---
        JPanel statusBar = createStatusBar();
        add(statusBar, BorderLayout.SOUTH);
    }

    private JPanel createControlPanel() {
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(SIDEBAR_BG);
        leftPanel.setBorder(new CompoundBorder(
            new LineBorder(Color.LIGHT_GRAY, 1), 
            new EmptyBorder(20, 20, 20, 20)
        ));
        leftPanel.setPreferredSize(new Dimension(350, 0));

        // Title
        JLabel controlTitle = new JLabel("Knapsack Configuration");
        controlTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        controlTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(controlTitle);
        leftPanel.add(Box.createVerticalStrut(20));

        // Problem Setup Section
        JPanel setupSection = createTitledSection("Problem Setup");
        
        setupSection.add(createLabel("Number of Items (N):"));
        nInput = createTextField("10");
        setupSection.add(nInput);
        setupSection.add(Box.createVerticalStrut(10));

        setupSection.add(createLabel("Knapsack Capacity (W):"));
        capInput = createTextField("50");
        setupSection.add(capInput);
        setupSection.add(Box.createVerticalStrut(10));

        setupSection.add(createLabel("Algorithm Strategy:"));
        String[] algos = {
            "Exact (Branch & Bound)", 
            "Heuristic (Smart Greedy)", 
            "Hybrid (Greedy + B&B)"
        };
        algoSelector = new JComboBox<>(algos);
        algoSelector.setFont(MAIN_FONT);
        algoSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        setupSection.add(algoSelector);
        
        leftPanel.add(setupSection);
        leftPanel.add(Box.createVerticalStrut(15));

        // Action Buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonPanel.setOpaque(false);

        JButton genBtn = createButton("🎲 Generate Random Items", new Color(108, 117, 125));
        genBtn.addActionListener(e -> generateData());
        buttonPanel.add(genBtn);
        buttonPanel.add(Box.createVerticalStrut(10));

        JButton solveBtn = createButton("🎯 MAXIMIZE PROFIT", ACCENT_COLOR);
        solveBtn.addActionListener(e -> solve());
        buttonPanel.add(solveBtn);
        buttonPanel.add(Box.createVerticalStrut(10));

        JButton exportBtn = createButton("💾 Export Results", new Color(23, 162, 184));
        exportBtn.addActionListener(e -> exportResults());
        buttonPanel.add(exportBtn);
        buttonPanel.add(Box.createVerticalStrut(10));

        JButton clearBtn = createButton("🗑️ Clear All", new Color(220, 53, 69));
        clearBtn.addActionListener(e -> clearAll());
        buttonPanel.add(clearBtn);

        leftPanel.add(buttonPanel);
        leftPanel.add(Box.createVerticalStrut(20));

        // Progress Bar
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setStringPainted(true);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(progressBar);
        leftPanel.add(Box.createVerticalStrut(15));

        // Results Summary
        JPanel resultsPanel = createTitledSection("Solution Summary");
        
        profitLabel = new JLabel("Total Profit: --");
        profitLabel.setFont(HEADER_FONT);
        profitLabel.setForeground(ACCENT_COLOR);
        resultsPanel.add(profitLabel);
        resultsPanel.add(Box.createVerticalStrut(5));
        
        weightLabel = new JLabel("Total Weight: --");
        weightLabel.setFont(MAIN_FONT);
        resultsPanel.add(weightLabel);
        resultsPanel.add(Box.createVerticalStrut(5));
        
        efficiencyLabel = new JLabel("Efficiency: --");
        efficiencyLabel.setFont(MAIN_FONT);
        resultsPanel.add(efficiencyLabel);
        resultsPanel.add(Box.createVerticalStrut(5));
        
        timeLabel = new JLabel("Execution Time: --");
        timeLabel.setFont(MAIN_FONT);
        resultsPanel.add(timeLabel);
        
        leftPanel.add(resultsPanel);
        leftPanel.add(Box.createVerticalGlue());

        return leftPanel;
    }

    private JPanel createVisualizationTab() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);

        // Visualization Panel with Scroll
        visPanel = new KnapsackVisPanel();
        JScrollPane scrollPane = new JScrollPane(visPanel);
        scrollPane.setPreferredSize(new Dimension(800, 600));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Visualization Controls
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.setBackground(new Color(250, 250, 250));
        controlPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        controlPanel.add(new JLabel("Zoom:"));
        zoomSlider = new JSlider(50, 200, 100);
        zoomSlider.setMajorTickSpacing(50);
        zoomSlider.setMinorTickSpacing(10);
        zoomSlider.setPaintTicks(true);
        zoomSlider.setPaintLabels(true);
        zoomSlider.setPreferredSize(new Dimension(200, 50));
        zoomSlider.addChangeListener(e -> {
            visPanel.setZoom(zoomSlider.getValue() / 100.0);
        });
        controlPanel.add(zoomSlider);

        JButton resetBtn = new JButton("Reset View");
        resetBtn.addActionListener(e -> {
            zoomSlider.setValue(100);
            visPanel.resetView();
        });
        controlPanel.add(resetBtn);

        JCheckBox showValuesBox = new JCheckBox("Show Values", true);
        showValuesBox.setSelected(true);
        showValuesBox.addActionListener(e -> {
            visPanel.setShowValues(showValuesBox.isSelected());
        });
        controlPanel.add(showValuesBox);

        JCheckBox showWeightsBox = new JCheckBox("Show Weights", true);
        showWeightsBox.setSelected(true);
        showWeightsBox.addActionListener(e -> {
            visPanel.setShowWeights(showWeightsBox.isSelected());
        });
        controlPanel.add(showWeightsBox);

        panel.add(controlPanel, BorderLayout.NORTH);

        return panel;
    }

    private JPanel createItemsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- NEW: Save reference to label ---
        allItemsLabel = new JLabel("All Items (N = 0)");
        allItemsLabel.setFont(HEADER_FONT);
        panel.add(allItemsLabel, BorderLayout.NORTH);

        // Items Table
        String[] columnNames = {"Item #", "Value", "Weight", "Value/Weight", "Status"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        itemTable = new JTable(tableModel);
        itemTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        itemTable.setRowHeight(25);
        itemTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        itemTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        
        // Color renderer for status
        itemTable.getColumnModel().getColumn(4).setCellRenderer(new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = new JLabel(value.toString());
                label.setOpaque(true);
                label.setFont(new Font("Segoe UI", Font.BOLD, 11));
                label.setHorizontalAlignment(SwingConstants.CENTER);
                
                if ("SELECTED".equals(value)) {
                    label.setBackground(new Color(200, 255, 200));
                    label.setForeground(new Color(0, 100, 0));
                } else {
                    label.setBackground(new Color(240, 240, 240));
                    label.setForeground(Color.GRAY);
                }
                
                if (isSelected) {
                    label.setBorder(BorderFactory.createLineBorder(table.getSelectionBackground(), 2));
                }
                
                return label;
            }
        });

        JScrollPane tableScroll = new JScrollPane(itemTable);
        panel.add(tableScroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createSelectedItemsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Header with summary
        JPanel headerPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        headerPanel.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        JLabel titleLabel = new JLabel("Items in Knapsack");
        titleLabel.setFont(HEADER_FONT);
        headerPanel.add(titleLabel);
        
        // --- NEW: Save reference to labels to update them safely ---
        selectedItemsLabel = new JLabel("No items selected");
        selectedItemsLabel.setFont(MAIN_FONT);
        headerPanel.add(selectedItemsLabel);
        
        JLabel summaryLabel2 = new JLabel("");
        summaryLabel2.setFont(MAIN_FONT);
        headerPanel.add(summaryLabel2);
        
        panel.add(headerPanel, BorderLayout.NORTH);

        // Selected Items Table
        String[] columnNames = {"Item #", "Value", "Weight", "Value/Weight Ratio"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        selectedTable = new JTable(tableModel);
        selectedTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        selectedTable.setRowHeight(30);
        selectedTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        selectedTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JScrollPane tableScroll = new JScrollPane(selectedTable);
        panel.add(tableScroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createInputDataTab() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel infoLabel = new JLabel("Item Data (Value Weight) - Editable");
        infoLabel.setFont(HEADER_FONT);
        panel.add(infoLabel, BorderLayout.NORTH);

        inputArea = new JTextArea(25, 80);
        inputArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        inputArea.setTabSize(4);
        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        panel.add(inputScroll, BorderLayout.CENTER);

        // Info panel
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.setBackground(new Color(255, 250, 205));
        infoPanel.setBorder(new LineBorder(new Color(255, 193, 7), 1));
        JLabel tipLabel = new JLabel("💡 Format: Each line should contain 'value weight' separated by space");
        tipLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        infoPanel.add(tipLabel);
        panel.add(infoPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(new CompoundBorder(
            new LineBorder(Color.LIGHT_GRAY, 1, true),
            new EmptyBorder(5, 10, 5, 10)
        ));
        statusBar.setBackground(new Color(240, 240, 240));

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusLabel.setForeground(new Color(0, 100, 0));
        statusBar.add(statusLabel, BorderLayout.WEST);

        return statusBar;
    }

    private JPanel createTitledSection(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            title,
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 14)
        ));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        panel.setBackground(SIDEBAR_BG);
        return panel;
    }

    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(MAIN_FONT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }
    
    private JTextField createTextField(String text) {
        JTextField tf = new JTextField(text);
        tf.setFont(MAIN_FONT);
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        return tf;
    }

    private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void generateData() {
        try {
            N = Integer.parseInt(nInput.getText().trim());
            Capacity = Integer.parseInt(capInput.getText().trim());
            
            if (N < 1 || N > 10000) {
                JOptionPane.showMessageDialog(this, 
                    "Please enter N between 1 and 10000", 
                    "Invalid Input", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            if (Capacity < 1) {
                JOptionPane.showMessageDialog(this, 
                    "Capacity must be positive", 
                    "Invalid Input", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            values = new int[N];
            weights = new int[N];
            StringBuilder sb = new StringBuilder();
            Random rand = new Random();
            
            for(int i = 0; i < N; i++) {
                values[i] = rand.nextInt(50) + 10;
                weights[i] = rand.nextInt(20) + 1;
                sb.append(String.format("%3d %3d\n", values[i], weights[i]));
            }
            
            inputArea.setText(sb.toString());
            selected = null;
            totalProfit = 0;
            totalWeight = 0;
            
            // Update tables
            updateAllItemsTable();
            updateSelectedItemsTable();
            
            statusLabel.setText("Generated " + N + " random items");
            statusLabel.setForeground(SUCCESS_COLOR);
            
            profitLabel.setText("Total Profit: --");
            weightLabel.setText("Total Weight: --");
            efficiencyLabel.setText("Efficiency: --");
            timeLabel.setText("Execution Time: --");
            
            visPanel.repaint();
            
            // Switch to input tab
            tabbedPane.setSelectedIndex(3);
            
        } catch(Exception e) { 
            e.printStackTrace(); // Print stack trace to see what actually happened
            JOptionPane.showMessageDialog(this, 
                "Invalid input format (Check Console for Details)", 
                "Input Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void solve() {
        if (inputArea.getText().trim().isEmpty()) {
            int choice = JOptionPane.showConfirmDialog(this, 
                "No item data found. Generate random items now?", 
                "Missing Data", JOptionPane.YES_NO_OPTION);
            
            if (choice == JOptionPane.YES_OPTION) {
                generateData();
            } else {
                return;
            }
        }

        try {
            N = Integer.parseInt(nInput.getText().trim());
            Capacity = Integer.parseInt(capInput.getText().trim());
            
            parseInputArea();
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Invalid input values. Check N, Capacity, and Item List.", 
                "Input Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Auto-switch algorithm for large inputs
        int selectedIndex = algoSelector.getSelectedIndex();
        if (selectedIndex == 0 && N > 25) {
            JOptionPane.showMessageDialog(this,
                "Input size (N=" + N + ") is too large for Exact method.\n" +
                "Switching to Heuristic (Smart Greedy) for better performance.",
                "Auto-Switch Warning",
                JOptionPane.WARNING_MESSAGE);
            algoSelector.setSelectedIndex(1);
        }
        
        new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();
                
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Solving knapsack problem...");
                    statusLabel.setForeground(PRIMARY_COLOR);
                    progressBar.setIndeterminate(true);
                });
                
                BufferedWriter bw = new BufferedWriter(new FileWriter("knapsack_input.txt"));
                bw.write(N + "\n");
                bw.write(inputArea.getText());
                bw.close();

                String mode = String.valueOf(algoSelector.getSelectedIndex() + 1);
                ProcessBuilder pb = new ProcessBuilder("knapsack_solver.exe", "knapsack_input.txt", mode, capInput.getText());
                pb.redirectErrorStream(true);
                Process p = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[Backend]: " + line);
                    if (line.contains("[Safety Stop]")) {
                        SwingUtilities.invokeLater(() -> 
                            JOptionPane.showMessageDialog(this, 
                                "Input size is too large for Exact Solver.\n" +
                                "Skipped B&B Phase to protect RAM.\n" +
                                "Showing Optimized Heuristic Result.", 
                                "Safety Triggered", 
                                JOptionPane.INFORMATION_MESSAGE)
                        );
                    }
                }

                p.waitFor();
                executionTime = System.currentTimeMillis() - startTime;

                File f = new File("solution_knapsack.csv");
                if(!f.exists()) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Backend failed - solution file not found");
                        statusLabel.setForeground(Color.RED);
                        progressBar.setIndeterminate(false);
                    });
                    return;
                }
                
                BufferedReader br = new BufferedReader(new FileReader(f));
                String resLine = br.readLine();
                br.close();

                if (resLine == null) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("No solution found");
                        statusLabel.setForeground(Color.RED);
                        progressBar.setIndeterminate(false);
                    });
                    return;
                }

                String[] parts = resLine.split(",");
                totalProfit = Integer.parseInt(parts[0]);
                selected = new int[N];
                totalWeight = 0;
                
                for(int i = 1; i < parts.length; i++) {
                    if(i - 1 < N) {
                        selected[i - 1] = Integer.parseInt(parts[i]);
                        if(selected[i - 1] == 1) {
                            totalWeight += weights[i - 1];
                        }
                    }
                }
                
                SwingUtilities.invokeLater(() -> {
                    updateResults();
                    statusLabel.setText("✓ Solution found successfully!");
                    statusLabel.setForeground(SUCCESS_COLOR);
                    progressBar.setIndeterminate(false);
                    
                    tabbedPane.setSelectedIndex(0);
                });
                
            } catch(Exception e) { 
                e.printStackTrace(); 
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Error during execution");
                    statusLabel.setForeground(Color.RED);
                    progressBar.setIndeterminate(false);
                });
            }
        }).start();
    }

    private void parseInputArea() {
        String[] lines = inputArea.getText().split("\n");
        // Ensure arrays are initialized
        values = new int[N];
        weights = new int[N];
        
        int count = 0;
        for(String line : lines) {
            if (line.trim().isEmpty()) continue; // Skip empty lines
            if (count >= N) break;
            
            String[] parts = line.trim().split("\\s+");
            if(parts.length >= 2) {
                try {
                    values[count] = Integer.parseInt(parts[0]);
                    weights[count] = Integer.parseInt(parts[1]);
                    count++;
                } catch (NumberFormatException ignored) {
                    // Skip malformed lines
                }
            }
        }
    }

    private void updateResults() {
        profitLabel.setText("Total Profit: " + totalProfit);
        weightLabel.setText("Total Weight: " + totalWeight + " / " + Capacity);
        
        double efficiency = Capacity > 0 ? (totalProfit * 100.0 / Capacity) : 0;
        efficiencyLabel.setText(String.format("Efficiency: %.2f (profit/capacity)", efficiency));
        
        timeLabel.setText("Execution Time: " + executionTime + " ms");
        
        updateAllItemsTable();
        updateSelectedItemsTable();
        
        visPanel.repaint();
    }

    private void updateAllItemsTable() {
        DefaultTableModel model = (DefaultTableModel) itemTable.getModel();
        model.setRowCount(0);
        
        if (values == null || N == 0) {
            return;
        }
        
        // --- SAFE UPDATE: Use class reference instead of component tree traversal ---
        if (allItemsLabel != null) {
            allItemsLabel.setText("All Items (N = " + N + ")");
        }
        
        for (int i = 0; i < N; i++) {
            double ratio = weights[i] > 0 ? (double) values[i] / weights[i] : 0;
            String status = (selected != null && selected[i] == 1) ? "SELECTED" : "Not Selected";
            
            model.addRow(new Object[]{
                i,
                values[i],
                weights[i],
                String.format("%.2f", ratio),
                status
            });
        }
    }

    private void updateSelectedItemsTable() {
        DefaultTableModel model = (DefaultTableModel) selectedTable.getModel();
        model.setRowCount(0);
        
        if (selected == null || values == null) {
            return;
        }
        
        int selectedCount = 0;
        int selProfit = 0;
        int selWeight = 0;
        
        for (int i = 0; i < N; i++) {
            if (selected[i] == 1) {
                selectedCount++;
                selProfit += values[i];
                selWeight += weights[i];
                
                double ratio = weights[i] > 0 ? (double) values[i] / weights[i] : 0;
                model.addRow(new Object[]{
                    i,
                    values[i],
                    weights[i],
                    String.format("%.2f", ratio)
                });
            }
        }
        
        // --- SAFE UPDATE: Use class reference ---
        if (selectedItemsLabel != null) {
            selectedItemsLabel.setText(selectedCount + " items selected (out of " + N + " total)");
            
            // Update the sibling label (summaryLabel2) if needed
            Container parent = selectedItemsLabel.getParent();
            if (parent != null && parent.getComponentCount() > 2 && parent.getComponent(2) instanceof JLabel) {
                 ((JLabel)parent.getComponent(2)).setText(
                    String.format("Total Value: %d | Total Weight: %d / %d (%.1f%% full)", 
                        selProfit, selWeight, Capacity, 
                        Capacity > 0 ? (selWeight * 100.0 / Capacity) : 0));
            }
        }
    }

    private void exportResults() {
        if (selected == null || totalProfit == 0) {
            JOptionPane.showMessageDialog(this,
                "No solution to export. Please solve the problem first.",
                "No Results",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("knapsack_results.txt"));
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fileChooser.getSelectedFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                
                writer.write("Knapsack Problem Solution\n");
                writer.write("=========================\n\n");
                writer.write("Problem Parameters:\n");
                writer.write("  Number of Items: " + N + "\n");
                writer.write("  Knapsack Capacity: " + Capacity + "\n");
                writer.write("  Algorithm: " + algoSelector.getSelectedItem() + "\n\n");
                
                writer.write("Solution:\n");
                writer.write("  Total Profit: " + totalProfit + "\n");
                writer.write("  Total Weight: " + totalWeight + " / " + Capacity + "\n");
                writer.write("  Capacity Used: " + String.format("%.1f%%\n", 
                    Capacity > 0 ? (totalWeight * 100.0 / Capacity) : 0));
                writer.write("  Execution Time: " + executionTime + " ms\n\n");
                
                writer.write("Selected Items:\n");
                writer.write(String.format("%-10s %-10s %-10s %-15s\n", 
                    "Item #", "Value", "Weight", "Value/Weight"));
                writer.write("--------------------------------------------------------\n");
                
                for (int i = 0; i < N; i++) {
                    if (selected[i] == 1) {
                        double ratio = weights[i] > 0 ? (double) values[i] / weights[i] : 0;
                        writer.write(String.format("%-10d %-10d %-10d %-15.2f\n", 
                            i, values[i], weights[i], ratio));
                    }
                }
                
                writer.close();
                
                JOptionPane.showMessageDialog(this,
                    "Results exported successfully to:\n" + file.getAbsolutePath(),
                    "Export Success",
                    JOptionPane.INFORMATION_MESSAGE);
                
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                    "Error exporting results: " + e.getMessage(),
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearAll() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to clear all data?",
            "Confirm Clear",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            N = 0;
            Capacity = 0;
            values = null;
            weights = null;
            selected = null;
            totalProfit = 0;
            totalWeight = 0;
            
            inputArea.setText("");
            
            ((DefaultTableModel) itemTable.getModel()).setRowCount(0);
            ((DefaultTableModel) selectedTable.getModel()).setRowCount(0);
            
            profitLabel.setText("Total Profit: --");
            weightLabel.setText("Total Weight: --");
            efficiencyLabel.setText("Efficiency: --");
            timeLabel.setText("Execution Time: --");
            statusLabel.setText("Ready");
            statusLabel.setForeground(SUCCESS_COLOR);
            
            visPanel.repaint();
        }
    }

    // ===== VISUALIZATION PANEL CLASS =====
    class KnapsackVisPanel extends JPanel {
        private double zoomFactor = 1.0;
        private boolean showValues = true;
        private boolean showWeights = true;
        private int offsetX = 0;
        private Point dragStart = null;

        public KnapsackVisPanel() {
            setPreferredSize(new Dimension(1200, 600));
            setBackground(Color.WHITE);
            
            MouseAdapter ma = new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    dragStart = e.getPoint();
                }
                
                public void mouseDragged(MouseEvent e) {
                    if (dragStart != null) {
                        offsetX += e.getX() - dragStart.x;
                        dragStart = e.getPoint();
                        repaint();
                    }
                }
                
                public void mouseReleased(MouseEvent e) {
                    dragStart = null;
                }
            };
            
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        public void setZoom(double zoom) {
            this.zoomFactor = zoom;
            int newWidth = (int) (1200 * zoom);
            setPreferredSize(new Dimension(newWidth, 600));
            revalidate();
            repaint();
        }

        public void setShowValues(boolean show) {
            this.showValues = show;
            repaint();
        }

        public void setShowWeights(boolean show) {
            this.showWeights = show;
            repaint();
        }

        public void resetView() {
            offsetX = 0;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            if (values == null || N == 0) {
                g.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                g.setColor(Color.GRAY);
                String msg = "Generate items to see the visualization";
                FontMetrics fm = g.getFontMetrics();
                g.drawString(msg, 
                    getWidth()/2 - fm.stringWidth(msg)/2, 
                    getHeight()/2);
                return;
            }

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            
            int barWidth = (int) (Math.min(60, (w - 100) / Math.max(1, N)) * zoomFactor);
            int spacing = (int) (5 * zoomFactor);
            int startX = 50 + offsetX;

            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
            g2.drawString("Knapsack Capacity: " + Capacity, 20, 30);

            if (selected != null) {
                int usedWeight = 0;
                for (int i = 0; i < N; i++) {
                    if (selected[i] == 1) usedWeight += weights[i];
                }

                int barX = 250;
                int barY = 15;
                int barLength = 300;
                int barHeight = 20;

                g2.setColor(Color.LIGHT_GRAY);
                g2.fillRect(barX, barY, barLength, barHeight);

                g2.setColor(usedWeight > Capacity ? Color.RED : new Color(76, 175, 80));
                int fillLength = Math.min(barLength, (int) ((barLength * usedWeight) / Math.max(1, Capacity)));
                g2.fillRect(barX, barY, fillLength, barHeight);

                g2.setColor(Color.DARK_GRAY);
                g2.drawRect(barX, barY, barLength, barHeight);

                g2.setColor(Color.BLACK);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                g2.drawString(String.format("Used: %d / %d (%.1f%%)", 
                    usedWeight, Capacity, 
                    (usedWeight * 100.0 / Math.max(1, Capacity))), 
                    barX + barLength + 10, barY + 15);
            }

            int legendY = 55;
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            
            g2.setColor(SUCCESS_COLOR);
            g2.fillRect(20, legendY, 15, 15);
            g2.setColor(Color.BLACK);
            g2.drawString("Selected", 40, legendY + 12);
            
            g2.setColor(new Color(224, 224, 224));
            g2.fillRect(120, legendY, 15, 15);
            g2.setColor(Color.BLACK);
            g2.drawString("Not Selected", 140, legendY + 12);

            int baselineY = h - 80;
            int maxBarHeight = h - 150;

            int maxValue = 1;
            for (int i = 0; i < N; i++) {
                maxValue = Math.max(maxValue, values[i]);
            }

            for (int i = 0; i < N; i++) {
                int x = startX + i * (barWidth + spacing);
                int barHeight = (int) ((values[i] * maxBarHeight) / (double) maxValue);
                int y = baselineY - barHeight;

                if (selected != null && selected[i] == 1) {
                    g2.setColor(SUCCESS_COLOR);
                } else {
                    g2.setColor(new Color(224, 224, 224));
                }
                g2.fillRoundRect(x, y, barWidth, barHeight, 8, 8);

                g2.setColor(Color.GRAY);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(x, y, barWidth, barHeight, 8, 8);

                if (barWidth >= 20) {
                    g2.setColor(Color.BLACK);
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, (int) (10 * Math.min(1, zoomFactor))));
                    
                    if (showValues) {
                        String valueStr = "V:" + values[i];
                        g2.drawString(valueStr, x + 2, y - 5);
                    }
                    
                    if (showWeights) {
                        String weightStr = "W:" + weights[i];
                        g2.drawString(weightStr, x + 2, baselineY + 15);
                    }
                    
                    if (N <= 100 || i % 5 == 0) {
                        g2.setFont(new Font("Segoe UI", Font.PLAIN, (int) (9 * Math.min(1, zoomFactor))));
                        String itemNum = "#" + i;
                        FontMetrics fm = g2.getFontMetrics();
                        g2.drawString(itemNum, x + (barWidth - fm.stringWidth(itemNum)) / 2, baselineY + 30);
                    }
                }
            }

            g2.setColor(Color.DARK_GRAY);
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(startX - 10, baselineY, startX + N * (barWidth + spacing), baselineY);
        }
    }
}