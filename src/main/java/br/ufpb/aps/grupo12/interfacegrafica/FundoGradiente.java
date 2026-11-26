package br.ufpb.aps.grupo12.interfacegrafica;

import java.awt.*;
import java.awt.geom.Point2D;
import javax.swing.JPanel;

/** Fundo escuro (preto → azul-marinho) com brilho azul e grade sutil. */
final class FundoGradiente extends JPanel {
    private final boolean comGrade;
    FundoGradiente(LayoutManager layout) { this(layout, false); }
    FundoGradiente(LayoutManager layout, boolean comGrade) { super(layout); this.comGrade = comGrade; setOpaque(false); }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        g2.setPaint(new GradientPaint(0, 0, Tema.NOITE, w, h, Tema.MARINHO));
        g2.fillRect(0, 0, w, h);
        if (comGrade) {
            g2.setColor(new Color(255, 255, 255, 10));
            for (int x = 0; x < w; x += 36) g2.drawLine(x, 0, x, h);
            for (int y = 0; y < h; y += 36) g2.drawLine(0, y, w, y);
        }
        float raio = Math.max(w, h) * 0.55f;
        if (raio > 0) {
            g2.setPaint(new RadialGradientPaint(new Point2D.Float(w * 0.85f, h * 0.1f), raio,
                new float[]{0f, 1f}, new Color[]{new Color(0x25, 0x63, 0xEB, 90), new Color(0x25, 0x63, 0xEB, 0)}));
            g2.fillRect(0, 0, w, h);
            g2.setPaint(new RadialGradientPaint(new Point2D.Float(w * 0.1f, h * 0.95f), raio * 0.7f,
                new float[]{0f, 1f}, new Color[]{new Color(0x38, 0xBD, 0xF8, 45), new Color(0x38, 0xBD, 0xF8, 0)}));
            g2.fillRect(0, 0, w, h);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
