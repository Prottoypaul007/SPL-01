import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Random;

public class KnapsackPanel extends JPanel {
    private JTextField nInput, capInput;
    private JTextArea inputArea;
    private JLabel statusLabel, profitLabel, weightLabel, timeLabel, efficiencyLabel;
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
    private long executionTime = 0;    // Design
    private final Font MAIN_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 16);
    private final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 20);
    private final Color ACCENT_COLOR = new Color(255, 152, 0); 
    private final Color SUCCESS_COLOR = new Color(76, 175, 80); 
    private final Color SIDEBAR_BG = new Color(245, 245, 245);
    private final Color TABLE_HEADER_BG = new Color(33, 150, 243);
    private final Color TABLE_HEADER_FG = Color.WHITE;
    private final Color STATUS_BG = new Color(240, 240, 245);
    private final Color SECTION_BORDER = new Color(200, 200, 200);    public KnapsackPanel() {
        setLayout(new BorderLayout(10, 10));

        // --- SIDEBAR (CONTROLS) ---
        JPanel leftPanel = createControlPanel();
        
        // SCROLL PANE FIX: Ensures UI never gets cut off vertically
        JScrollPane leftScroll = new JScrollPane(leftPanel);
        leftScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        leftScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        leftScroll.setBorder(null); 
        leftScroll.setPreferredSize(new Dimension(420, 0));
        
        add(leftScroll, BorderLayout.WEST);

        // --- CENTER (TABBED PANE) ---
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(MAIN_FONT);
        
        tabbedPane.addTab("Visualization", createVisualizationTab());
        tabbedPane.addTab("All Items", createItemsTab());
        tabbedPane.addTab("Selected Items", createSelectedItemsTab());
        tabbedPane.addTab("Input Data", createInputDataTab());
        
        add(tabbedPane, BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);
    }    private JPanel createControlPanel() {
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(SIDEBAR_BG);
        leftPanel.setBorder(new CompoundBorder(new LineBorder(SECTION_BORDER, 1), new EmptyBorder(15, 15, 15, 15)));

        JLabel controlTitle = new JLabel("Knapsack Configuration");
        controlTitle.setFont(TITLE_FONT);
        controlTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(controlTitle);
        leftPanel.add(Box.createVerticalStrut(15));JPanel setupSection = createTitledSection("Problem Setup");
        setupSection.add(createLabel("Number of Items (N):"));
        nInput = createTextField("10");
        setupSection.add(nInput);
        setupSection.add(Box.createVerticalStrut(8));

        setupSection.add(createLabel("Knapsack Capacity (W):"));
        capInput = createTextField("50");
        setupSection.add(capInput);
        setupSection.add(Box.createVerticalStrut(8));

        setupSection.add(createLabel("Algorithm Strategy:"));
        
        String[] algos = {
            "Exact (Branch & Bound)", 
            "Heuristic (Smart Greedy)", 
            "Advanced Hybrid (Core-Problem)"
        };
        algoSelector = new JComboBox<>(algos);
        algoSelector.setFont(MAIN_FONT);
        algoSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        setupSection.add(algoSelector);
        
        leftPanel.add(setupSection);
        leftPanel.add(Box.createVerticalStrut(15));        JButton genBtn = createButton("🎲 Generate Random Items", new Color(108, 117, 125));
        genBtn.addActionListener(e -> generateData());
        leftPanel.add(genBtn);
        leftPanel.add(Box.createVerticalStrut(8));

        JButton solveBtn = createButton("🎯 MAXIMIZE PROFIT", ACCENT_COLOR);
        solveBtn.addActionListener(e -> solve());
        leftPanel.add(solveBtn);
        leftPanel.add(Box.createVerticalStrut(8));

        JButton clearBtn = createButton("🗑️ Clear All", new Color(220, 53, 69));
        clearBtn.addActionListener(e -> clearAll());
        leftPanel.add(clearBtn);
        leftPanel.add(Box.createVerticalStrut(12));

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setFont(MAIN_FONT);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        leftPanel.add(progressBar);
        leftPanel.add(Box.createVerticalStrut(12));

        JPanel resultsPanel = createTitledSection("Solution Summary");
        resultsPanel.setBorder(new CompoundBorder(
            new TitledBorder(new LineBorder(ACCENT_COLOR, 2), "Solution Summary", TitledBorder.LEFT, TitledBorder.TOP, HEADER_FONT, ACCENT_COLOR),
            new EmptyBorder(10, 10, 10, 10)));
        
        profitLabel = new JLabel("Total Profit: --");
        profitLabel.setFont(HEADER_FONT);
        profitLabel.setForeground(ACCENT_COLOR);
        resultsPanel.add(profitLabel);
        resultsPanel.add(Box.createVerticalStrut(6));
        
        weightLabel = new JLabel("Total Weight: --");
        weightLabel.setFont(MAIN_FONT);
        resultsPanel.add(weightLabel);
        resultsPanel.add(Box.createVerticalStrut(4));
        
        efficiencyLabel = new JLabel("Efficiency: --");
        efficiencyLabel.setFont(MAIN_FONT);
        resultsPanel.add(efficiencyLabel);
        resultsPanel.add(Box.createVerticalStrut(4));
        
        timeLabel = new JLabel("Execution Time: --");
        timeLabel.setFont(MAIN_FONT);
        resultsPanel.add(timeLabel);
        
        leftPanel.add(resultsPanel);
        leftPanel.add(Box.createVerticalGlue());
        return leftPanel;
    }

    private void solve() {
        if (inputArea.getText().trim().isEmpty()) {
            generateData();
        }

        try {
            N = Integer.parseInt(nInput.getText().trim());
            Capacity = Integer.parseInt(capInput.getText().trim());
            parseInputArea();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Check N, Capacity, and Data format.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int selectedIndex = algoSelector.getSelectedIndex();
        
        // Safety Interceptor: Only warn for Pure Exact (index 0). Hybrid (index 2) is now immune to crashes.
        if (selectedIndex == 0 && N > 28) {
            int choice = JOptionPane.showConfirmDialog(this,
                "N=" + N + " is high for standard Branch & Bound. This might use a lot of RAM.\n" +
                "Switch to Advanced Core-Problem Hybrid?", "Safety Warning", JOptionPane.YES_NO_OPTION);
            
            if (choice == JOptionPane.YES_OPTION) {
                algoSelector.setSelectedIndex(2);
                selectedIndex = 2;
            }
        }

        final int modeValue = selectedIndex + 1; // 1: Exact, 2: Greedy, 3: Advanced Hybrid

        new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Solving Knapsack...");
                    progressBar.setIndeterminate(true);
                });
                
                // Write input for C
                BufferedWriter bw = new BufferedWriter(new FileWriter("knapsack_input.txt"));
                bw.write(N + " " + Capacity + "\n");
                bw.write(inputArea.getText());
                bw.close();

                // Clean old solution
                new File("solution_knapsack.csv").delete();

                String exeExt = System.getProperty("os.name").toLowerCase().contains("win") ? ".exe" : "";
                ProcessBuilder pb = new ProcessBuilder("knapsack_solver" + exeExt, "knapsack_input.txt", String.valueOf(modeValue));
                pb.redirectErrorStream(true);
                Process p = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[Backend]: " + line);
                }

                p.waitFor();
                executionTime = System.currentTimeMillis() - startTime;

                SwingUtilities.invokeLater(() -> {
                    progressBar.setIndeterminate(false);
                    loadSolution();
                });
            } catch(Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadSolution() {
        try {
            File f = new File("solution_knapsack.csv");
            if(!f.exists()) {
                statusLabel.setText("No solution file found.");
                return;
            }
            
            BufferedReader br = new BufferedReader(new FileReader(f));
            String resLine = br.readLine();
            br.close();

            if (resLine != null) {
                String[] parts = resLine.split(",");
                totalProfit = Integer.parseInt(parts[0]);
                selected = new int[N];
                totalWeight = 0;
                
                for(int i = 0; i < N; i++) {
                    if (i + 1 < parts.length) {
                        selected[i] = Integer.parseInt(parts[i + 1].trim());
                        if(selected[i] == 1) totalWeight += weights[i];
                    }
                }
                
                updateResults();
                statusLabel.setText("✓ Optimization complete using " + algoSelector.getSelectedItem());
                tabbedPane.setSelectedIndex(0);
            }
        } catch(Exception e) {
            statusLabel.setText("Error parsing solution.");
        }
    }

    private void generateData() {
        try {
            N = Integer.parseInt(nInput.getText().trim());
            Capacity = Integer.parseInt(capInput.getText().trim());
            values = new int[N]; weights = new int[N];
            StringBuilder sb = new StringBuilder();
            Random rand = new Random();
            for(int i=0; i<N; i++) {
                values[i] = rand.nextInt(50) + 10;
                weights[i] = rand.nextInt(20) + 1;
                sb.append(String.format("%d %d\n", values[i], weights[i]));
            }
            inputArea.setText(sb.toString());
            selected = null;
            updateAllItemsTable();
            tabbedPane.setSelectedIndex(3);
        } catch(Exception e) { JOptionPane.showMessageDialog(this, "Invalid N/W"); }
    }

    private void parseInputArea() {
        String[] lines = inputArea.getText().split("\n");
        values = new int[N]; weights = new int[N];
        int count = 0;
        for(String line : lines) {
            if (line.trim().isEmpty() || count >= N) continue;
            String[] parts = line.trim().split("\\s+");
            if(parts.length >= 2) {
                values[count] = Integer.parseInt(parts[0]);
                weights[count] = Integer.parseInt(parts[1]);
                count++;
            }
        }
    }

    private void updateResults() {
        profitLabel.setText("Total Profit: " + totalProfit);
        weightLabel.setText("Total Weight: " + totalWeight + " / " + Capacity);
        double eff = Capacity > 0 ? (totalProfit * 100.0 / Capacity) : 0;
        efficiencyLabel.setText(String.format("Efficiency: %.2f (profit/cap)", eff));
        timeLabel.setText("Execution Time: " + executionTime + " ms");
        updateAllItemsTable();
        updateSelectedItemsTable();
        visPanel.repaint();
    }

    private void updateAllItemsTable() {
        DefaultTableModel model = (DefaultTableModel) itemTable.getModel();
        model.setRowCount(0);
        if (values == null) return;
        allItemsLabel.setText("All Items (N = " + N + ")");
        for (int i = 0; i < N; i++) {
            double ratio = weights[i] > 0 ? (double) values[i] / weights[i] : 0;
            String status = (selected != null && selected[i] == 1) ? "SELECTED" : "Not Selected";
            model.addRow(new Object[]{i, values[i], weights[i], String.format("%.2f", ratio), status});
        }
    }

    private void updateSelectedItemsTable() {
        DefaultTableModel model = (DefaultTableModel) selectedTable.getModel();
        model.setRowCount(0);
        if (selected == null) return;
        int count = 0;
        for (int i = 0; i < N; i++) {
            if (selected[i] == 1) {
                count++;
                double ratio = weights[i] > 0 ? (double) values[i] / weights[i] : 0;
                model.addRow(new Object[]{i, values[i], weights[i], String.format("%.2f", ratio)});
            }
        }
        selectedItemsLabel.setText(count + " items in knapsack");
    }
    
    private JPanel createVisualizationTab() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);
        visPanel = new KnapsackVisPanel();
        JScrollPane scrollPane = new JScrollPane(visPanel);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.add(new JLabel("Zoom:"));
        zoomSlider = new JSlider(50, 200, 100);
        zoomSlider.addChangeListener(e -> visPanel.setZoom(zoomSlider.getValue() / 100.0));
        controlPanel.add(zoomSlider);
        
        JCheckBox vBox = new JCheckBox("Show Values", true);
        vBox.addActionListener(e -> visPanel.setShowValues(vBox.isSelected()));
        controlPanel.add(vBox);
        
        panel.add(controlPanel, BorderLayout.NORTH);
        return panel;
    }

    class KnapsackVisPanel extends JPanel {
        private double zoom = 1.0;
        private boolean showVal = true;
        public void setZoom(double z) { this.zoom = z; repaint(); }
        public void setShowValues(boolean s) { this.showVal = s; repaint(); }
        public void resetView() { zoom = 1.0; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (values == null) return;
            Graphics2D g2 = (Graphics2D) g;
            int barW = (int)(30 * zoom);
            int baseline = getHeight() - 60;
            for(int i=0; i<N; i++) {
                int h = (int)(values[i] * 5 * zoom);
                g2.setColor(selected != null && selected[i] == 1 ? SUCCESS_COLOR : Color.LIGHT_GRAY);
                g2.fillRect(50 + i*(barW+5), baseline-h, barW, h);
                g2.setColor(Color.BLACK);
                if(showVal) g2.drawString(String.valueOf(values[i]), 50 + i*(barW+5), baseline-h-5);
            }
        }
    }    private JPanel createTitledSection(String title) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new CompoundBorder(
            new TitledBorder(new LineBorder(SECTION_BORDER, 1), title, TitledBorder.LEFT, TitledBorder.TOP, HEADER_FONT),
            new EmptyBorder(10, 10, 10, 10)));
        p.setBackground(SIDEBAR_BG);
        return p;
    }

    private JLabel createLabel(String t) {
        JLabel l = new JLabel(t);
        l.setFont(MAIN_FONT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JTextField createTextField(String t) {
        JTextField tf = new JTextField(t);
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        tf.setFont(MAIN_FONT);
        tf.setAlignmentX(Component.LEFT_ALIGNMENT);
        return tf;
    }

    private JButton createButton(String t, Color bg) {
        JButton b = new JButton(t);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        return b;
    }

    private JPanel createItemsTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(15, 15, 15, 15));
        p.setBackground(Color.WHITE);
        
        allItemsLabel = new JLabel("All Items");
        allItemsLabel.setFont(HEADER_FONT);
        p.add(allItemsLabel, BorderLayout.NORTH);
        
        itemTable = new JTable(new DefaultTableModel(new String[]{"ID", "Value", "Weight", "Ratio", "Status"}, 0));
        styleTable(itemTable);
        p.add(new JScrollPane(itemTable), BorderLayout.CENTER);
        return p;
    }

    private JPanel createSelectedItemsTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(15, 15, 15, 15));
        p.setBackground(Color.WHITE);
        
        selectedItemsLabel = new JLabel("Selected Items");
        selectedItemsLabel.setFont(HEADER_FONT);
        p.add(selectedItemsLabel, BorderLayout.NORTH);
        
        selectedTable = new JTable(new DefaultTableModel(new String[]{"ID", "Value", "Weight", "Ratio"}, 0));
        styleTable(selectedTable);
        p.add(new JScrollPane(selectedTable), BorderLayout.CENTER);
        return p;
    }

    private void styleTable(JTable table) {
        table.setFont(MAIN_FONT);
        table.setRowHeight(28);
        table.setGridColor(new Color(220, 220, 220));
        table.setShowGrid(true);
        
        JTableHeader header = table.getTableHeader();
        header.setBackground(TABLE_HEADER_BG);
        header.setForeground(TABLE_HEADER_FG);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setPreferredSize(new Dimension(0, 35));
    }

    private JPanel createInputDataTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        inputArea = new JTextArea();
        inputArea.setFont(new Font("Courier New", Font.PLAIN, 12));
        inputArea.setLineWrap(false);
        inputArea.setTabSize(4);
        inputArea.setMargin(new Insets(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(inputArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        p.add(scrollPane, BorderLayout.CENTER);
        return p;
    }

    private JPanel createStatusBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(STATUS_BG);
        p.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, SECTION_BORDER),
            new EmptyBorder(8, 15, 8, 15)));
        
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(MAIN_FONT);
        statusLabel.setForeground(new Color(60, 60, 60));
        
        p.add(statusLabel, BorderLayout.WEST);
        return p;
    }
    private void clearAll() {
        inputArea.setText(""); values = null; weights = null; selected = null;
        totalProfit = 0; totalWeight = 0; updateResults();
    }
}