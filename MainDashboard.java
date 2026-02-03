import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MainDashboard extends JFrame {

    public MainDashboard() {
        setTitle("Algorithm Comparison Suite: Exact vs Heuristic vs Hybrid");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Custom Header Panel
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(33, 33, 33)); // Dark Gray
        headerPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 15));
        
        JLabel titleLabel = new JLabel("Optimization Algorithms Dashboard");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel);
        
        add(headerPanel, BorderLayout.NORTH);

        // Tabbed Pane with padding
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        tabbedPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Add Tabs
        tabbedPane.addTab("  TSP Solver (Minimization)  ", new TSPPanel());
        tabbedPane.addTab("  Knapsack Solver (Maximization)  ", new KnapsackPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        // Apply Modern Look and Feel (Nimbus)
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
            // Optional: Customize Nimbus colors
            UIManager.put("nimbusBase", new Color(240, 240, 240));
            UIManager.put("nimbusBlueGrey", new Color(220, 220, 220));
            UIManager.put("control", new Color(250, 250, 250));
            
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {}
        }

        SwingUtilities.invokeLater(() -> new MainDashboard().setVisible(true));
    }
}