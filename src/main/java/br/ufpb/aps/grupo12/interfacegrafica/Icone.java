package br.ufpb.aps.grupo12.interfacegrafica;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.Icon;

/** Ícones vetoriais desenhados em Java2D (sem arquivos de imagem nem bibliotecas). */
final class Icone implements Icon {
    enum Tipo { PAINEL, CARRINHO, HISTORICO, CARTEIRA, PASTA, CAIXA, PESSOA, PESSOAS, CAMINHAO, ETIQUETA,
        GRAFICO, SINO, SETA_CIMA, SETA_BAIXO, CIFRAO, BANCO, ALERTA, BUSCA, MAIS, IMPRESSORA, SAIR, DEVOLVER, MOVIMENTO }
    private final Tipo tipo;
    private final int tam;
    private final Color cor;
    Icone(Tipo tipo, int tam, Color cor) { this.tipo = tipo; this.tam = tam; this.cor = cor; }
    @Override public int getIconWidth() { return tam; }
    @Override public int getIconHeight() { return tam; }

    @Override public void paintIcon(Component c, Graphics g0, int x, int y) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(x, y);
        g.scale(tam / 24.0, tam / 24.0);
        g.setColor(cor);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (tipo) {
            case PAINEL -> { g.drawRoundRect(3, 3, 7, 8, 2, 2); g.drawRoundRect(14, 3, 7, 5, 2, 2);
                g.drawRoundRect(14, 12, 7, 9, 2, 2); g.drawRoundRect(3, 15, 7, 6, 2, 2); }
            case CARRINHO -> { Path2D p = new Path2D.Double(); p.moveTo(2, 3); p.lineTo(5, 3); p.lineTo(7.5, 15);
                p.lineTo(19, 15); p.lineTo(21, 7); p.lineTo(6, 7); g.draw(p); g.drawOval(8, 18, 2, 2); g.drawOval(17, 18, 2, 2); }
            case HISTORICO -> { g.draw(new Arc2D.Double(3, 3, 18, 18, 140, 300, Arc2D.OPEN));
                g.drawLine(12, 7, 12, 12); g.drawLine(12, 12, 15, 14); g.drawLine(3, 6, 4, 9); g.drawLine(4, 9, 7, 8); }
            case CARTEIRA -> { g.drawRoundRect(3, 6, 18, 13, 3, 3); g.drawLine(3, 10, 21, 10); g.drawLine(15, 15, 18, 15); }
            case PASTA -> { Path2D p = new Path2D.Double(); p.moveTo(3, 7); p.lineTo(3, 19); p.lineTo(21, 19);
                p.lineTo(21, 9); p.lineTo(12, 9); p.lineTo(10, 6); p.lineTo(3, 6); p.closePath(); g.draw(p); }
            case CAIXA -> { Path2D p = new Path2D.Double(); p.moveTo(12, 3); p.lineTo(21, 7.5); p.lineTo(21, 16.5);
                p.lineTo(12, 21); p.lineTo(3, 16.5); p.lineTo(3, 7.5); p.closePath(); g.draw(p);
                g.draw(new Line2D.Double(3, 7.5, 12, 12)); g.draw(new Line2D.Double(21, 7.5, 12, 12)); g.drawLine(12, 12, 12, 21); }
            case PESSOA -> { g.drawOval(8, 3, 8, 8); g.draw(new Arc2D.Double(4, 13, 16, 14, 0, 180, Arc2D.OPEN)); }
            case PESSOAS -> { g.drawOval(5, 4, 7, 7); g.draw(new Arc2D.Double(2, 13, 13, 12, 0, 180, Arc2D.OPEN));
                g.drawOval(14, 5, 5, 5); g.draw(new Arc2D.Double(14, 13, 8, 10, 0, 180, Arc2D.OPEN)); }
            case CAMINHAO -> { g.drawRect(2, 6, 12, 10); Path2D p = new Path2D.Double(); p.moveTo(14, 9); p.lineTo(18, 9);
                p.lineTo(21, 12); p.lineTo(21, 16); p.lineTo(14, 16); g.draw(p); g.drawOval(5, 16, 3, 3); g.drawOval(16, 16, 3, 3); }
            case ETIQUETA -> { Path2D p = new Path2D.Double(); p.moveTo(3, 3); p.lineTo(11, 3); p.lineTo(21, 13);
                p.lineTo(13, 21); p.lineTo(3, 11); p.closePath(); g.draw(p); g.fillOval(7, 7, 2, 2); }
            case GRAFICO -> { g.drawLine(5, 20, 5, 12); g.drawLine(10, 20, 10, 5); g.drawLine(15, 20, 15, 9); g.drawLine(20, 20, 20, 14); }
            case SINO -> { Path2D p = new Path2D.Double(); p.moveTo(6, 16); p.curveTo(6, 7, 7, 4, 12, 4);
                p.curveTo(17, 4, 18, 7, 18, 16); p.lineTo(20, 18); p.lineTo(4, 18); p.lineTo(6, 16); g.draw(p);
                g.draw(new Arc2D.Double(10, 18, 4, 4, 180, 180, Arc2D.OPEN)); }
            case SETA_CIMA -> { g.drawLine(3, 17, 9, 11); g.drawLine(9, 11, 13, 15); g.drawLine(13, 15, 21, 7);
                g.drawLine(15, 7, 21, 7); g.drawLine(21, 7, 21, 13); }
            case SETA_BAIXO -> { g.drawLine(3, 7, 9, 13); g.drawLine(9, 13, 13, 9); g.drawLine(13, 9, 21, 17);
                g.drawLine(15, 17, 21, 17); g.drawLine(21, 17, 21, 11); }
            case CIFRAO -> { g.drawLine(12, 2, 12, 22); Path2D p = new Path2D.Double(); p.moveTo(17, 6); p.curveTo(15, 4, 7, 4, 7, 8.5);
                p.curveTo(7, 12, 17, 11, 17, 15.5); p.curveTo(17, 20, 9, 20, 7, 18); g.draw(p); }
            case BANCO -> { Path2D p = new Path2D.Double(); p.moveTo(3, 9); p.lineTo(12, 3); p.lineTo(21, 9); p.closePath(); g.draw(p);
                g.drawLine(6, 11, 6, 18); g.drawLine(10, 11, 10, 18); g.drawLine(14, 11, 14, 18); g.drawLine(18, 11, 18, 18); g.drawLine(3, 21, 21, 21); }
            case ALERTA -> { Path2D p = new Path2D.Double(); p.moveTo(12, 3); p.lineTo(22, 20); p.lineTo(2, 20); p.closePath(); g.draw(p);
                g.drawLine(12, 9, 12, 14); g.fillOval(11, 16, 2, 2); }
            case BUSCA -> { g.drawOval(4, 4, 12, 12); g.drawLine(15, 15, 20, 20); }
            case MAIS -> { g.drawLine(12, 5, 12, 19); g.drawLine(5, 12, 19, 12); }
            case IMPRESSORA -> { g.drawRect(6, 3, 12, 6); g.drawRoundRect(3, 9, 18, 8, 2, 2); g.drawRect(7, 14, 10, 7); }
            case SAIR -> { g.drawLine(9, 4, 4, 4); g.drawLine(4, 4, 4, 20); g.drawLine(4, 20, 9, 20);
                g.drawLine(10, 12, 20, 12); g.drawLine(16, 8, 20, 12); g.drawLine(16, 16, 20, 12); }
            case DEVOLVER -> { g.draw(new Arc2D.Double(5, 6, 15, 13, -90, 180, Arc2D.OPEN)); g.drawLine(12, 19, 5, 19);
                g.drawLine(12, 6, 4, 6); g.drawLine(7, 3, 4, 6); g.drawLine(7, 9, 4, 6); }
            case MOVIMENTO -> { g.drawLine(7, 4, 7, 20); g.drawLine(3, 8, 7, 4); g.drawLine(11, 8, 7, 4);
                g.drawLine(17, 4, 17, 20); g.drawLine(13, 16, 17, 20); g.drawLine(21, 16, 17, 20); }
        }
        g.dispose();
    }
}
