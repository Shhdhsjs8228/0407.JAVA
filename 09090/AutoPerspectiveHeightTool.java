import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

public class AutoPerspectiveHeightTool extends JFrame {
    private BufferedImage img;
    private ArrayList<Point2D.Double> points = new ArrayList<>();
    private Point2D.Double vanishingPoint = null;
    private final double refHeight = 180.0; // 教材指定基準身高
    private int step = 0; 
    private ImagePanel imagePanel;
    private JLabel lblStatus;

    public AutoPerspectiveHeightTool() {
        setTitle("JAVA透視測量工具 - 教材作業最終版");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel controlPanel = new JPanel();
        JButton btnLoad = new JButton("1. 載入照片");
        JButton btnReset = new JButton("全部重置");
        lblStatus = new JLabel("請載入照片。基準：穿 Just do it 衣服的學生 (180cm)");
        lblStatus.setFont(new Font("Microsoft JhengHei", Font.BOLD, 14));

        btnLoad.addActionListener(e -> loadImage());
        btnReset.addActionListener(e -> {
            points.clear();
            vanishingPoint = null;
            step = 0;
            lblStatus.setText("已重置。");
            imagePanel.repaint();
        });

        controlPanel.add(btnLoad);
        controlPanel.add(btnReset);
        controlPanel.add(lblStatus);
        add(controlPanel, BorderLayout.NORTH);

        imagePanel = new ImagePanel();
        add(new JScrollPane(imagePanel), BorderLayout.CENTER);

        setSize(1200, 900);
        setLocationRelativeTo(null);
    }

    private void loadImage() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                img = ImageIO.read(chooser.getSelectedFile());
                imagePanel.setPreferredSize(new Dimension(img.getWidth(), img.getHeight()));
                vanishingPoint = autoDetectVanishingPoint(img);
                step = 1;
                lblStatus.setText("步驟一：點擊「Just Do It」同學的【頭頂】與【腳底】");
                imagePanel.revalidate();
                imagePanel.repaint();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "讀取失敗: " + ex.getMessage());
            }
        }
    }

    // 教材核心：自動偵測地平線/消失點邏輯
    private Point2D.Double autoDetectVanishingPoint(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        double sumY = 0;
        int count = 0;
        for (int y = 10; y < h - 10; y += 15) {
            for (int x = 10; x < w - 10; x += 15) {
                int gray = (src.getRGB(x, y) >> 16) & 0xFF;
                int down = (src.getRGB(x, y + 1) >> 16) & 0xFF;
                if (Math.abs(gray - down) > 50) { 
                    sumY += y;
                    count++;
                }
            }
        }
        double finalY = (count > 0) ? (sumY / count) : (h / 3.0);
        return new Point2D.Double(w / 2.0, finalY);
    }

    private void calculateHeight() {
        if (points.size() < 4 || vanishingPoint == null) return;

        Point2D.Double p1 = points.get(0); // 基準頭
        Point2D.Double p2 = points.get(1); // 基準腳
        Point2D.Double p3 = points.get(2); // 目標頭
        Point2D.Double p4 = points.get(3); // 目標腳

        double h_ref_px = Math.abs(p1.y - p2.y);
        double h_tgt_px = Math.abs(p3.y - p4.y);
        double refBaseY = Math.max(p1.y, p2.y);
        double tgtBaseY = Math.max(p3.y, p4.y);

        // 教材原理：相似三角形深度補償
        double d_v_ref = Math.abs(vanishingPoint.y - refBaseY);
        double d_v_tgt = Math.abs(vanishingPoint.y - tgtBaseY);
        if (d_v_tgt < 1) d_v_tgt = 1; 

        double scale = d_v_ref / d_v_tgt;
        double targetH = refHeight * (h_tgt_px / h_ref_px) * scale;

        JOptionPane.showMessageDialog(this, String.format("測量結果 [基準: 180cm]\n目標身高: %.2f cm", targetH));
        
        points.remove(3);
        points.remove(2);
        lblStatus.setText("可繼續點擊下一位同學的【頭】與【腳】");
        repaint();
    }

    class ImagePanel extends JPanel {
        public ImagePanel() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (img == null) return;
                    if (e.isShiftDown()) {
                        vanishingPoint = new Point2D.Double(e.getX(), e.getY());
                        repaint();
                        return;
                    }
                    if (step > 0) {
                        points.add(new Point2D.Double(e.getX(), e.getY()));
                        if (points.size() == 2 && step == 1) {
                            step = 2;
                            lblStatus.setText("基準OK。請點擊【下一位同學】的頭與腳");
                        } else if (points.size() == 4) {
                            calculateHeight();
                        }
                        repaint();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (img != null) g.drawImage(img, 0, 0, null);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (vanishingPoint != null) {
                g2.setColor(Color.CYAN);
                g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0));
                g2.drawLine(0, (int)vanishingPoint.y, getWidth(), (int)vanishingPoint.y);
                g2.drawString("消失線 (Shift+點擊修正)", 20, (int)vanishingPoint.y - 10);
            }

            for (int i = 0; i < points.size(); i++) {
                Point2D.Double p = points.get(i);
                g2.setColor(i < 2 ? Color.GREEN : Color.MAGENTA);
                g2.fillOval((int)p.x - 5, (int)p.y - 5, 10, 10);
                if (vanishingPoint != null) {
                    g2.setStroke(new BasicStroke(1));
                    g2.drawLine((int)p.x, (int)p.y, (int)vanishingPoint.x, (int)vanishingPoint.y);
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AutoPerspectiveHeightTool().setVisible(true));
    }
}