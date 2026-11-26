package br.ufpb.aps.grupo12.interfacegrafica;

import java.awt.*;
import javax.swing.*;

/** Logotipo do StockFlow: caixas empilhadas com seta de fluxo, desenhado em Java2D. */
final class Marca {
    private Marca() {}

    static JComponent icone(int tamanho) {
        JComponent c = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                float s = tamanho / 40f;
                g2.setPaint(new GradientPaint(0, 0, Tema.AZUL, tamanho, tamanho, Tema.CIANO));
                g2.fillRoundRect(0, 0, tamanho, tamanho, Math.round(12 * s), Math.round(12 * s));
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.2f * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawRect(Math.round(9 * s), Math.round(20 * s), Math.round(10 * s), Math.round(10 * s));
                g2.drawRect(Math.round(21 * s), Math.round(20 * s), Math.round(10 * s), Math.round(10 * s));
                g2.drawRect(Math.round(15 * s), Math.round(9 * s), Math.round(10 * s), Math.round(10 * s));
                g2.dispose();
            }
        };
        c.setPreferredSize(new Dimension(tamanho, tamanho));
        return c;
    }

    static JPanel criar(boolean claro) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        p.setOpaque(false);
        JPanel textos = new JPanel(new GridLayout(2, 1));
        textos.setOpaque(false);
        JLabel nome = new JLabel("<html>Stock<span style='color:#38BDF8'>Flow</span></html>");
        nome.setFont(Tema.titulo(18));
        nome.setForeground(claro ? Color.WHITE : Tema.TEXTO);
        JLabel sub = new JLabel("ESTOQUE & VENDAS");
        sub.setFont(Tema.sans(10, Font.BOLD));
        sub.setForeground(claro ? new Color(0x94A3B8) : Tema.TEXTO_SUAVE);
        textos.add(nome);
        textos.add(sub);
        p.add(icone(38));
        p.add(textos);
        return p;
    }
}
