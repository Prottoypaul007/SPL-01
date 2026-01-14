import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TSPSolverUI extends JFrame {
    // UI Components
    private JTextField nInput;
    private JTextField startNodeInput; // <--- NEW FIELD
    private JTextArea matrixArea;
    private JLabel statusLabel;
    private JTextArea pathArea;
    private JPanel graphPanel;
    private JComboBox<String> algoSelector;

    // Data
    private int N = 0;
    private int currentStartNode = 0; // <--- Store current start node
    private List<Integer> solutionPath = new ArrayList<>();
    private int solutionCost = -1;

    public TSPSolverUI() {
        setTitle("TSP Solver - Comparison System");
        setSize(1000, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- LEFT PANEL ---
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        leftPanel.setPreferredSize(new Dimension(320, 750));

        // 1. N Input
        JPanel nPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        nPanel.add(new JLabel("Number of Cities (N):"));
        nInput = new JTextField("12", 5);
        nPanel.add(nInput);
        leftPanel.add(nPanel);

        // 2. Start Node Input (NEW)
        JPanel startPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startPanel.add(new JLabel("Start Node (0 to N-1):"));
        startNodeInput = new JTextField("0", 5);
        startPanel.add(startNodeInput);
        leftPanel.add(startPanel);

        // 3. Algorithm Selector
        JPanel algoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        algoPanel.add(new JLabel("Algorithm:"));
        String[] algorithms = { "1. Exact (Branch & Bound)", "2. Heuristic (Ant Colony)" };
        algoSelector = new JComboBox<>(algorithms);
        algoPanel.add(algoSelector);
        leftPanel.add(algoPanel);

        // 4. Buttons
        JButton genBtn = new JButton("Generate Random Matrix");
        genBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        genBtn.addActionListener(e -> generateRandomMatrix());
        leftPanel.add(genBtn);

        leftPanel.add(Box.createVerticalStrut(10));

        JButton solveBtn = new JButton("SOLVE TSP");
        solveBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        solveBtn.setBackground(new Color(0, 120, 215));
        solveBtn.setForeground(Color.WHITE);
        solveBtn.setFont(new Font("Arial", Font.BOLD, 14));
        solveBtn.addActionListener(e -> solveTSP());
        leftPanel.add(solveBtn);

        // 5. Matrix Area
        leftPanel.add(Box.createVerticalStrut(10));
        leftPanel.add(new JLabel("Cost Matrix:"));
        matrixArea = new JTextArea(10, 20);
        leftPanel.add(new JScrollPane(matrixArea));

        // 6. Output Areas
        leftPanel.add(Box.createVerticalStrut(10));
        statusLabel = new JLabel("Status: Ready");
        statusLabel.setForeground(Color.BLUE);
        leftPanel.add(statusLabel);

        leftPanel.add(Box.createVerticalStrut(5));
        leftPanel.add(new JLabel("Path Sequence:"));
        pathArea = new JTextArea(4, 20);
        pathArea.setEditable(false);
        pathArea.setLineWrap(true);
        pathArea.setWrapStyleWord(true);
        pathArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        leftPanel.add(new JScrollPane(pathArea));

        add(leftPanel, BorderLayout.WEST);

        // --- RIGHT PANEL ---
        graphPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGraph(g);
            }
        };
        graphPanel.setBackground(Color.WHITE);
        add(graphPanel, BorderLayout.CENTER);
    }

    private void generateRandomMatrix() {
        try {
            N = Integer.parseInt(nInput.getText().trim());
            StringBuilder sb = new StringBuilder();
            Random rand = new Random();
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    if (i == j) sb.append("0 ");
                    else sb.append(rand.nextInt(90) + 10).append(" ");
                }
                sb.append("\n");
            }
            matrixArea.setText(sb.toString());
            statusLabel.setText("Status: Random Matrix Generated.");
            solutionPath.clear();
            pathArea.setText("");
            repaint();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid N.");
        }
    }

    private void solveTSP() {
        try {
            N = Integer.parseInt(nInput.getText().trim());
            
            // Get Start Node
            String startNodeStr = startNodeInput.getText().trim();
            int sNode = Integer.parseInt(startNodeStr);
            
            // Validate Start Node
            if (sNode < 0 || sNode >= N) {
                JOptionPane.showMessageDialog(this, "Start Node must be between 0 and " + (N-1));
                return;
            }
            currentStartNode = sNode; // Update global variable for drawing

            String mode = (algoSelector.getSelectedIndex() == 0) ? "1" : "2";

            if (mode.equals("1") && N > 15) {
                int choice = JOptionPane.showConfirmDialog(this, 
                    "N is too large for Exact Solver. Switch to Ant Colony?",
                    "Warning", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    mode = "2";
                    algoSelector.setSelectedIndex(1);
                }
            }

            statusLabel.setText("Status: Solving...");
            saveInputToFile("input.txt");

            // PASS 3 ARGUMENTS: File, Mode, StartNode
            ProcessBuilder pb = new ProcessBuilder("tsp_solver.exe", "input.txt", mode, startNodeStr);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[C-Backend]: " + line);
            }
            process.waitFor();

            loadSolution("solution.csv");

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Status: Error.");
        }
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
                statusLabel.setText("Status: Backend failed.");
                return;
            }
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = br.readLine();
            br.close();

            if (line == null || line.startsWith("-1")) {
                statusLabel.setText("Status: No path found.");
                pathArea.setText("No Path");
                return;
            }

            String[] parts = line.split(",");
            solutionCost = Integer.parseInt(parts[0]);
            solutionPath.clear();
            
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < parts.length; i++) {
                int node = Integer.parseInt(parts[i]);
                solutionPath.add(node);
                sb.append(node).append(i < parts.length - 1 ? " -> " : "");
            }

            statusLabel.setText("Status: Solved! Cost: " + solutionCost);
            pathArea.setText(sb.toString());
            repaint();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawGraph(Graphics g) {
        if (N == 0) return;
        int width = graphPanel.getWidth();
        int height = graphPanel.getHeight();
        int centerX = width / 2;
        int centerY = height / 2;
        int radius = Math.min(width, height) / 2 - 50;

        Point[] points = new Point[N];
        for (int i = 0; i < N; i++) {
            double angle = 2 * Math.PI * i / N;
            points[i] = new Point(
                centerX + (int) (radius * Math.cos(angle)),
                centerY + (int) (radius * Math.sin(angle))
            );
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Connections
        g2.setColor(new Color(230, 230, 230));
        for (int i = 0; i < N; i++) {
            for (int j = i + 1; j < N; j++) {
                g2.drawLine(points[i].x, points[i].y, points[j].x, points[j].y);
            }
        }

        // Path
        if (!solutionPath.isEmpty()) {
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(3));
            for (int i = 0; i < solutionPath.size() - 1; i++) {
                Point p1 = points[solutionPath.get(i)];
                Point p2 = points[solutionPath.get(i+1)];
                g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.BOLD, 20));
            g2.drawString("Cost: " + solutionCost, centerX - 40, centerY);
        }

        // Nodes
        for (int i = 0; i < N; i++) {
            // Draw CURRENT START NODE as Green
            if (i == currentStartNode) g2.setColor(new Color(34, 139, 34)); 
            else g2.setColor(new Color(70, 130, 180));
            
            g2.fillOval(points[i].x - 15, points[i].y - 15, 30, 30);
            
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            String label = String.valueOf(i);
            int txtWidth = g2.getFontMetrics().stringWidth(label);
            g2.drawString(label, points[i].x - (txtWidth/2), points[i].y + 5);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TSPSolverUI().setVisible(true));
    }
}