package br.ufpb.aps.grupo12.interfacegrafica;

import java.awt.*;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/** Painel branco de cantos arredondados com sombra leve. */
class Cartao extends JPanel {
    private Color fundo = Color.WHITE;
    private Color borda = Tema.BORDA;
    Cartao(LayoutManager layout) { this(layout, 18); }
    Cartao(LayoutManager layout, int margem) {
        super(layout);
        setOpaque(false);
        setBorder(new EmptyBorder(margem, margem + 2, margem, margem + 2));
    }
    Cartao cores(Color fundo, Color borda) { this.fundo = fundo; this.borda = borda; return this; }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        g2.setColor(new Color(15, 23, 42, 10));
        g2.fillRoundRect(1, 3, w - 2, h - 3, 18, 18);
        g2.setColor(fundo);
        g2.fillRoundRect(0, 0, w - 1, h - 3, 16, 16);
        g2.setColor(borda);
        g2.drawRoundRect(0, 0, w - 1, h - 3, 16, 16);
        g2.dispose();
        super.paintComponent(g);
    }
}
