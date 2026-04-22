import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TSPPanel extends JPanel {
    private JTextField nInput, startNodeInput;
    private JTextArea matrixArea, pathArea;
    private JLabel statusLabel, costLabel, nodesLabel, timeLabel;
    private GraphPanel graphPanel;
    private JComboBox<String> algoSelector;
    private JTabbedPane tabbedPane;
    private JProgressBar progressBar;
    private JSlider zoomSlider;
    private JTable pathTable;
    
    // Data
    private int N = 0;
    private int currentStartNode = 0;
    private List<Integer> solutionPath = new ArrayList<>();
    private int solutionCost = -1;
    private long executionTime = 0;
    
    // Design
    private final Font MAIN_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 16);
    private final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 20);
    private final Color ACCENT_COLOR = new Color(0, 150, 215);
    private final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private final Color SIDEBAR_BG = new Color(245, 245, 245);
    private final Color TABLE_HEADER_BG = new Color(33, 150, 243);
    private final Color TABLE_HEADER_FG = Color.WHITE;
    private final Color STATUS_BG = new Color(240, 240, 245);
    private final Color SECTION_BORDER = new Color(200, 200, 200);
      public TSPPanel() {
        setLayout(new BorderLayout(10, 10));
        
        // --- LEFT SIDEBAR (CONTROLS) ---
        JPanel leftPanel = createControlPanel();
        
        // SCROLL PANE: Ensures UI never gets cut off vertically
        JScrollPane leftScroll = new JScrollPane(leftPanel);
        leftScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        leftScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        leftScroll.setBorder(null);
        leftScroll.setPreferredSize(new Dimension(420, 0));
        
        add(leftScroll, BorderLayout.WEST);
        
        // --- CENTER (TABBED PANE) ---
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(MAIN_FONT);
        
        // Tab 1: Graph Visualization
        JPanel graphTab = createGraphTab();
        tabbedPane.addTab("Visualization", new ImageIcon(), graphTab, "Interactive graph view");
        
        // Tab 2: Path Details
        JPanel pathTab = createPathDetailsTab();
        tabbedPane.addTab("Path Details", new ImageIcon(), pathTab, "Detailed path information");
        
        // Tab 3: Matrix View
        JPanel matrixTab = createMatrixTab();
        tabbedPane.addTab("Distance Matrix", new ImageIcon(), matrixTab, "View/Edit distance matrix");
        
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
            new LineBorder(SECTION_BORDER, 1),
            new EmptyBorder(15, 15, 15, 15)));
        
        // Title
        JLabel controlTitle = new JLabel("TSP Solver Configuration");
        controlTitle.setFont(TITLE_FONT);
        controlTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(controlTitle);
        leftPanel.add(Box.createVerticalStrut(15));
        
        // Input Section
        JPanel inputSection = createTitledSection("Problem Setup");
        
        inputSection.add(createLabel("Number of Cities (N):"));
        nInput = createTextField("12");
        inputSection.add(nInput);
        inputSection.add(Box.createVerticalStrut(8));
        
        inputSection.add(createLabel("Start Node (0 to N-1):"));
        startNodeInput = createTextField("0");
        inputSection.add(startNodeInput);
        inputSection.add(Box.createVerticalStrut(8));
        
        inputSection.add(createLabel("Algorithm Strategy:"));
        
        String[] algorithms = {
            "Exact (Branch & Bound)",
            "Heuristic (ACO)",
            "Massive Scale (Clustered Hybrid)"
        };
        algoSelector = new JComboBox<>(algorithms);
        algoSelector.setFont(MAIN_FONT);
        algoSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        inputSection.add(algoSelector);
        
        leftPanel.add(inputSection);
        leftPanel.add(Box.createVerticalStrut(15));
          // Action Buttons
        JButton genBtn = createButton("🗺️ Generate Random Map", new Color(108, 117, 125));
        genBtn.addActionListener(e -> generateRandomMatrix());
        leftPanel.add(genBtn);
        leftPanel.add(Box.createVerticalStrut(8));
        
        JButton solveBtn = createButton("🚀 SOLVE TSP", ACCENT_COLOR);
        solveBtn.addActionListener(e -> solveTSP());
        leftPanel.add(solveBtn);
        leftPanel.add(Box.createVerticalStrut(8));
        
        JButton exportBtn = createButton("💾 Export Results", new Color(23, 162, 184));
        exportBtn.addActionListener(e -> exportResults());
        leftPanel.add(exportBtn);
        leftPanel.add(Box.createVerticalStrut(8));
        
        JButton clearBtn = createButton("🗑️ Clear All", new Color(220, 53, 69));
        clearBtn.addActionListener(e -> clearAll());
        leftPanel.add(clearBtn);
        leftPanel.add(Box.createVerticalStrut(12));
        
        // Progress Bar
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setStringPainted(true);
        progressBar.setFont(MAIN_FONT);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        leftPanel.add(progressBar);
        leftPanel.add(Box.createVerticalStrut(12));
        
        // Results Summary
        JPanel resultsPanel = createTitledSection("Results Summary");
        resultsPanel.setBorder(new CompoundBorder(
            new TitledBorder(new LineBorder(ACCENT_COLOR, 2), "Results Summary", TitledBorder.LEFT, TitledBorder.TOP, HEADER_FONT, ACCENT_COLOR),
            new EmptyBorder(10, 10, 10, 10)));
        
        costLabel = new JLabel("Total Cost: --");
        costLabel.setFont(HEADER_FONT);
        costLabel.setForeground(ACCENT_COLOR);
        resultsPanel.add(costLabel);
        resultsPanel.add(Box.createVerticalStrut(6));
        
        nodesLabel = new JLabel("Cities Visited: --");
        nodesLabel.setFont(MAIN_FONT);
        resultsPanel.add(nodesLabel);
        resultsPanel.add(Box.createVerticalStrut(4));
        
        timeLabel = new JLabel("Execution Time: --");
        timeLabel.setFont(MAIN_FONT);
        resultsPanel.add(timeLabel);
        
        leftPanel.add(resultsPanel);
        leftPanel.add(Box.createVerticalGlue());
        
        return leftPanel;
    }
      private JPanel createGraphTab() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);
        
        // Graph Panel with Scroll
        graphPanel = new GraphPanel();
        JScrollPane scrollPane = new JScrollPane(graphPanel);
        scrollPane.setPreferredSize(new Dimension(800, 600));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Graph Controls
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.setBackground(new Color(250, 250, 250));
        controlPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        controlPanel.add(new JLabel("Zoom:"));
        zoomSlider = new JSlider(50, 200, 100);
        zoomSlider.setMajorTickSpacing(50);
        zoomSlider.setMinorTickSpacing(10);
        zoomSlider.setPaintTicks(true);
        zoomSlider.setPaintLabels(true);
        zoomSlider.setPreferredSize(new Dimension(200, 50));
        zoomSlider.addChangeListener(e -> {
            graphPanel.setZoom(zoomSlider.getValue() / 100.0);
        });
        controlPanel.add(zoomSlider);
        
        JButton resetZoomBtn = new JButton("Reset View");
        resetZoomBtn.addActionListener(e -> {
            zoomSlider.setValue(100);
            graphPanel.resetView();
        });
        controlPanel.add(resetZoomBtn);
        
        JCheckBox showLabelsBox = new JCheckBox("Show All Labels", true);
        showLabelsBox.addActionListener(e -> {
            graphPanel.setShowAllLabels(showLabelsBox.isSelected());
        });
        controlPanel.add(showLabelsBox);
        
        panel.add(controlPanel, BorderLayout.NORTH);
        
        return panel;
    }
      private JPanel createPathDetailsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(Color.WHITE);
        
        // Path as text area (formatted)
        pathArea = new JTextArea(8, 60);
        pathArea.setEditable(false);
        pathArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        pathArea.setLineWrap(true);
        pathArea.setWrapStyleWord(false);
        pathArea.setBackground(new Color(250, 250, 250));
        pathArea.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane pathScroll = new JScrollPane(pathArea);
        pathScroll.setBorder(BorderFactory.createTitledBorder("Path Sequence"));
        panel.add(pathScroll, BorderLayout.NORTH);
        
        // Path as table (for large N)
        String[] columnNames = {"Step", "From City", "To City", "Distance"};        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        pathTable = new JTable(tableModel);
        styleTable(pathTable);
        
        JScrollPane tableScroll = new JScrollPane(pathTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Step-by-Step Path"));
        panel.add(tableScroll, BorderLayout.CENTER);
        
        return panel;
    }
      private JPanel createMatrixTab() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(Color.WHITE);
        
        JLabel infoLabel = new JLabel("Distance Matrix (editable - modify before solving)");
        infoLabel.setFont(HEADER_FONT);
        panel.add(infoLabel, BorderLayout.NORTH);
        
        matrixArea = new JTextArea(20, 80);
        matrixArea.setFont(new Font("Courier New", Font.PLAIN, 12));
        matrixArea.setTabSize(4);
        matrixArea.setLineWrap(false);
        matrixArea.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane matrixScroll = new JScrollPane(matrixArea);
        matrixScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        matrixScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        panel.add(matrixScroll, BorderLayout.CENTER);
        
        return panel;
    }
      private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(STATUS_BG);
        statusBar.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, SECTION_BORDER),
            new EmptyBorder(8, 15, 8, 15)));
        
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(MAIN_FONT);
        statusLabel.setForeground(new Color(60, 60, 60));
        statusBar.add(statusLabel, BorderLayout.WEST);
        
        return statusBar;
    }
    
    private JPanel createTitledSection(String title) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new CompoundBorder(
            new TitledBorder(new LineBorder(SECTION_BORDER, 1), title, TitledBorder.LEFT, TitledBorder.TOP, HEADER_FONT),
            new EmptyBorder(10, 10, 10, 10)));
        p.setBackground(SIDEBAR_BG);
        return p;
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
        tf.setAlignmentX(Component.LEFT_ALIGNMENT);
        return tf;
    }
      private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        return btn;
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
    
    private void generateRandomMatrix() {
        try {
            N = Integer.parseInt(nInput.getText().trim());
            if (N < 2 || N > 1000) {
                JOptionPane.showMessageDialog(this, 
                    "Please enter N between 2 and 1000", 
                    "Invalid Input", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // GENERATE EUCLIDEAN MAP (Real-World Physics)
            int[] xCoords = new int[N];
            int[] yCoords = new int[N];
            Random rand = new Random();
            
            // 1. Scatter cities randomly on a 200x200 grid
            for (int i = 0; i < N; i++) {
                xCoords[i] = rand.nextInt(200);
                yCoords[i] = rand.nextInt(200);
            }
            
            // 2. Calculate true geometric distances (Pythagorean theorem)
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    if (i == j) {
                        sb.append(String.format("%4d ", 0));
                    } else {
                        double dx = xCoords[i] - xCoords[j];
                        double dy = yCoords[i] - yCoords[j];
                        int dist = (int) Math.round(Math.sqrt(dx*dx + dy*dy));
                        if (dist == 0) dist = 1; // Prevent 0 distance for distinct nodes
                        sb.append(String.format("%4d ", dist));
                    }
                }
                sb.append("\n");
            }
            
            matrixArea.setText(sb.toString());
            statusLabel.setText("Geometric map generated with " + N + " cities");
            statusLabel.setForeground(SUCCESS_COLOR);
            
            // Clear previous results
            solutionPath.clear();
            pathArea.setText("");
            ((DefaultTableModel) pathTable.getModel()).setRowCount(0);
            costLabel.setText("Total Cost: --");
            nodesLabel.setText("Cities Visited: --");
            timeLabel.setText("Execution Time: --");
            graphPanel.repaint();
            
            // Switch to matrix tab
            tabbedPane.setSelectedIndex(2);
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, 
                "Invalid number format for N", 
                "Input Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void solveTSP() {
        final String startNodeStr;
        try {
            N = Integer.parseInt(nInput.getText().trim());
            startNodeStr = startNodeInput.getText().trim();
            int sNode = Integer.parseInt(startNodeStr);
            
            if (sNode < 0 || sNode >= N) {
                JOptionPane.showMessageDialog(this, 
                    "Start node must be between 0 and " + (N-1),
                    "Invalid Start Node",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            currentStartNode = sNode;
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, 
                "Invalid input values",
                "Input Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // UPDATED: Auto-switch algorithm logic.
        // If Exact is selected and N > 22, jump directly to Clustered Hybrid (Index 2)
        int selectedIndex = algoSelector.getSelectedIndex();
        if (selectedIndex == 0) {
            if (N > 22) {
                JOptionPane.showMessageDialog(this,
                    "Input size (N=" + N + ") is massive.\n" +
                    "Switching to Massive Scale Hybrid to prevent crash.",
                    "Auto-Switch Warning",
                    JOptionPane.WARNING_MESSAGE);
                algoSelector.setSelectedIndex(2); // Index 2 is Mode 3 (Clustered Hybrid)
            }
        }
        
        final String mode = String.valueOf(algoSelector.getSelectedIndex() + 1);
        
        // Background execution
        new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();
                  SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Solving TSP...");
                    statusLabel.setForeground(ACCENT_COLOR);
                    progressBar.setIndeterminate(true);
                });
                
                saveInputToFile("input.txt");
                
                // Cross-platform EXE extension handler
                String exeExt = System.getProperty("os.name").toLowerCase().contains("win") ? ".exe" : "";
                ProcessBuilder pb = new ProcessBuilder("tsp_solver" + exeExt, "input.txt", mode, startNodeStr);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[Backend]: " + line);
                }
                
                process.waitFor();
                executionTime = System.currentTimeMillis() - startTime;
                
                SwingUtilities.invokeLater(() -> {
                    progressBar.setIndeterminate(false);
                    loadSolution("solution.csv");
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Error occurred during execution");
                    statusLabel.setForeground(Color.RED);
                    progressBar.setIndeterminate(false);
                });
            }
        }).start();
    }
    
    private void saveInputToFile(String filename) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        writer.write(N + "\n");
        writer.write(matrixArea.getText());
        writer.close();
    }
    
    private void loadSolution(String filename) {
        try {
            File file = new File(filename);
            if (!file.exists()) {
                statusLabel.setText("Backend failed - solution file not found");
                statusLabel.setForeground(Color.RED);
                return;
            }
            
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = br.readLine();
            br.close();
            
            if (line == null || line.startsWith("-1")) {
                statusLabel.setText("No valid path found");
                statusLabel.setForeground(Color.RED);
                pathArea.setText("No Path Found");
                return;
            }
            
            String[] parts = line.split(",");
            solutionCost = Integer.parseInt(parts[0]);
            solutionPath.clear();
            
            // Build path
            for (int i = 1; i < parts.length; i++) {
                solutionPath.add(Integer.parseInt(parts[i]));
            }
            
            // Update UI
            updateResults();
            
            statusLabel.setText("✓ Solution found successfully!");
            statusLabel.setForeground(SUCCESS_COLOR);
            
            // Switch to visualization tab
            tabbedPane.setSelectedIndex(0);
            
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Error loading solution");
            statusLabel.setForeground(Color.RED);
        }
    }
    
    private void updateResults() {
        // Update summary
        costLabel.setText("Total Cost: " + solutionCost);
        nodesLabel.setText("Cities Visited: " + solutionPath.size());
        timeLabel.setText("Execution Time: " + executionTime + " ms");
        
        // Format path text
        StringBuilder pathText = new StringBuilder();
        pathText.append("Optimal Path:\n");
        
        int nodesPerLine = Math.max(8, Math.min(15, 60 / (String.valueOf(N).length() + 4)));
        
        for (int i = 0; i < solutionPath.size(); i++) {
            pathText.append(String.format("%4d", solutionPath.get(i)));
            
            if (i < solutionPath.size() - 1) {
                pathText.append(" → ");
                if ((i + 1) % nodesPerLine == 0) {
                    pathText.append("\n");
                }
            }
        }
        pathText.append(" → ").append(solutionPath.get(0)).append(" (return)\n");
        pathText.append("\nTotal Distance: ").append(solutionCost);
        
        pathArea.setText(pathText.toString());
        
        // Update table
        updatePathTable();
        
        // Repaint graph
        graphPanel.repaint();
    }
    
    private void updatePathTable() {
        DefaultTableModel model = (DefaultTableModel) pathTable.getModel();
        model.setRowCount(0);
        
        // Parse distance matrix for actual distances
        int[][] distMatrix = parseDistanceMatrix();
        
        for (int i = 0; i < solutionPath.size() - 1; i++) {
            int from = solutionPath.get(i);
            int to = solutionPath.get(i + 1);
            int distance = (distMatrix != null && distMatrix.length > from && distMatrix[from].length > to) 
                ? distMatrix[from][to] : 0;
            
            model.addRow(new Object[]{
                i + 1,
                from,
                to,
                distance
            });
        }
        
        // Add return trip
        if (solutionPath.size() > 0) {
            int from = solutionPath.get(solutionPath.size() - 1);
            int to = solutionPath.get(0);
            int distance = (distMatrix != null && distMatrix.length > from && distMatrix[from].length > to) 
                ? distMatrix[from][to] : 0;
            
            model.addRow(new Object[]{
                solutionPath.size(),
                from,
                to + " (return)",
                distance
            });
        }
    }
    
    private int[][] parseDistanceMatrix() {
        try {
            String[] lines = matrixArea.getText().split("\n");
            int[][] matrix = new int[N][N];
            
            for (int i = 0; i < Math.min(N, lines.length); i++) {
                String[] values = lines[i].trim().split("\\s+");
                for (int j = 0; j < Math.min(N, values.length); j++) {
                    matrix[i][j] = Integer.parseInt(values[j].trim());
                }
            }
            return matrix;
        } catch (Exception e) {
            return null;
        }
    }
    
    private void exportResults() {
        if (solutionPath.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No solution to export. Please solve TSP first.",
                "No Results",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("tsp_results.txt"));
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fileChooser.getSelectedFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                
                writer.write("TSP Solution Results\n");
                writer.write("===================\n\n");
                writer.write("Number of Cities: " + N + "\n");
                writer.write("Start Node: " + currentStartNode + "\n");
                writer.write("Algorithm: " + algoSelector.getSelectedItem() + "\n");
                writer.write("Total Cost: " + solutionCost + "\n");
                writer.write("Execution Time: " + executionTime + " ms\n\n");
                writer.write("Path:\n");
                writer.write(pathArea.getText());
                
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
            currentStartNode = 0;
            solutionPath.clear();
            solutionCost = -1;
            
            matrixArea.setText("");
            pathArea.setText("");
            ((DefaultTableModel) pathTable.getModel()).setRowCount(0);
            
            costLabel.setText("Total Cost: --");
            nodesLabel.setText("Cities Visited: --");
            timeLabel.setText("Execution Time: --");
            statusLabel.setText("Ready");
            statusLabel.setForeground(SUCCESS_COLOR);
            
            graphPanel.repaint();
        }
    }
    
    // ===== GRAPH PANEL CLASS =====
    class GraphPanel extends JPanel {
        private double zoomFactor = 1.0;
        private boolean showAllLabels = true;
        private int offsetX = 0, offsetY = 0;
        private Point dragStart = null;
        
        public GraphPanel() {
            setPreferredSize(new Dimension(1000, 800));
            setBackground(Color.WHITE);
            
            // Add mouse drag support
            MouseAdapter ma = new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    dragStart = e.getPoint();
                }
                
                public void mouseDragged(MouseEvent e) {
                    if (dragStart != null) {
                        offsetX += e.getX() - dragStart.x;
                        offsetY += e.getY() - dragStart.y;
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
            int newSize = (int) (800 * zoom);
            setPreferredSize(new Dimension(newSize, newSize));
            revalidate();
            repaint();
        }
        
        public void setShowAllLabels(boolean show) {
            this.showAllLabels = show;
            repaint();
        }
        
        public void resetView() {
            offsetX = 0;
            offsetY = 0;
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            if (N == 0) {
                g.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                g.setColor(Color.GRAY);
                String msg = "Generate a map to see the graph visualization";
                FontMetrics fm = g.getFontMetrics();
                g.drawString(msg, 
                    getWidth()/2 - fm.stringWidth(msg)/2, 
                    getHeight()/2);
                return;
            }
            
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            int w = (int) (getWidth() * zoomFactor);
            int h = (int) (getHeight() * zoomFactor);
            int centerX = w / 2 + offsetX;
            int centerY = h / 2 + offsetY;
            int radius = (int) ((Math.min(w, h) / 2 - 80) * zoomFactor);
            
            Point[] points = new Point[N];
            for (int i = 0; i < N; i++) {
                double angle = 2 * Math.PI * i / N - Math.PI / 2;
                points[i] = new Point(
                    centerX + (int)(radius * Math.cos(angle)),
                    centerY + (int)(radius * Math.sin(angle))
                );
            }
            
            // Draw background edges
            g2.setColor(new Color(230, 230, 230));
            g2.setStroke(new BasicStroke(1));
            for (int i = 0; i < N; i++) {
                for (int j = i + 1; j < N; j++) {
                    g2.drawLine(points[i].x, points[i].y, points[j].x, points[j].y);
                }
            }
            
            // Draw solution path
            if (!solutionPath.isEmpty()) {
                g2.setColor(new Color(220, 53, 69));
                g2.setStroke(new BasicStroke((float)(3 * zoomFactor)));
                
                for (int i = 0; i < solutionPath.size() - 1; i++) {
                    Point p1 = points[solutionPath.get(i)];
                    Point p2 = points[solutionPath.get(i + 1)];
                    drawArrow(g2, p1.x, p1.y, p2.x, p2.y);
                }
                
                // Draw return edge
                Point pLast = points[solutionPath.get(solutionPath.size() - 1)];
                Point pFirst = points[solutionPath.get(0)];
                g2.setStroke(new BasicStroke((float)(2 * zoomFactor), 
                    BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 
                    10.0f, new float[]{10.0f}, 0.0f));
                drawArrow(g2, pLast.x, pLast.y, pFirst.x, pFirst.y);
                
                // Draw cost label
                g2.setColor(Color.BLACK);
                g2.setFont(new Font("Segoe UI", Font.BOLD, (int)(20 * zoomFactor)));
                g2.drawString("Cost: " + solutionCost, 20, 30);
            }
            
            // Draw nodes
            int nodeSize = (int) (30 * zoomFactor);
            for (int i = 0; i < N; i++) {
                boolean isStart = (i == currentStartNode);
                boolean isInPath = solutionPath.contains(i);
                
                // Node color
                if (isStart) {
                    g2.setColor(new Color(40, 167, 69));
                } else if (isInPath) {
                    g2.setColor(new Color(0, 123, 255));
                } else {
                    g2.setColor(new Color(108, 117, 125));
                }
                
                g2.fillOval(points[i].x - nodeSize/2, points[i].y - nodeSize/2, nodeSize, nodeSize);
                
                // Node border
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke((float)(2 * zoomFactor)));
                g2.drawOval(points[i].x - nodeSize/2, points[i].y - nodeSize/2, nodeSize, nodeSize);
                
                // Node label
                if (showAllLabels || N <= 30 || isStart || isInPath) {
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, (int)(12 * zoomFactor)));
                    String label = String.valueOf(i);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(label, 
                        points[i].x - fm.stringWidth(label)/2, 
                        points[i].y + fm.getAscent()/2 - 2);
                }
            }
        }
        
        private void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2) {
            g2.drawLine(x1, y1, x2, y2);
            
            // Arrow head
            double angle = Math.atan2(y2 - y1, x2 - x1);
            int arrowSize = (int) (10 * zoomFactor);
            
            int[] xPoints = {
                x2,
                x2 - (int)(arrowSize * Math.cos(angle - Math.PI/6)),
                x2 - (int)(arrowSize * Math.cos(angle + Math.PI/6))
            };
            int[] yPoints = {
                y2,
                y2 - (int)(arrowSize * Math.sin(angle - Math.PI/6)),
                y2 - (int)(arrowSize * Math.sin(angle + Math.PI/6))
            };
            
            g2.fillPolygon(xPoints, yPoints, 3);
        }
    }
}