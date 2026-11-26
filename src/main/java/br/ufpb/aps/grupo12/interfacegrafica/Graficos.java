package br.ufpb.aps.grupo12.interfacegrafica;

import br.ufpb.aps.grupo12.dominio.Dinheiro;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;

/** Gráficos desenhados em Java2D. Só exibem valores já calculados pelos serviços. */
final class Graficos {
    private Graficos() {}
    static final Color[] PALETA = {new Color(0x2563EB), new Color(0x38BDF8), new Color(0x6366F1),
        new Color(0x14B8A6), new Color(0x0EA5E9), new Color(0x94A3B8), new Color(0x1E3A8A), new Color(0x22D3EE)};

    private static Graphics2D preparar(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }
    private static String compacto(long centavos) {
        double reais = centavos / 100.0;
        if (reais >= 1000) return String.format("R$%.1fk", reais / 1000).replace('.', ',');
        return String.format("R$%.0f", reais);
    }
    private static long teto(long max) {
        if (max <= 0) return 10000;
        double passo = Math.pow(10, Math.floor(Math.log10(max)));
        return (long) (Math.ceil(max / passo) * passo);
    }

    /** Linha com área preenchida (ex.: vendas da semana). */
    static final class Linha extends JComponent {
        private List<String> rotulos = List.of();
        private List<Long> valores = List.of();
        Linha() { setPreferredSize(new Dimension(400, 220)); setOpaque(false); }
        void definir(List<String> r, List<Long> v) { rotulos = r; valores = v; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = preparar(g);
            int esq = 62, dir = 14, topo = 12, base = 28;
            int w = getWidth() - esq - dir, h = getHeight() - topo - base;
            long max = teto(valores.stream().mapToLong(Long::longValue).max().orElse(0));
            g2.setFont(Tema.sans(11, Font.PLAIN));
            for (int i = 0; i <= 4; i++) {
                int y = topo + h - h * i / 4;
                g2.setColor(Tema.BORDA);
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{4, 4}, 0));
                g2.drawLine(esq, y, esq + w, y);
                g2.setColor(Tema.TEXTO_SUAVE);
                String t = compacto(max * i / 4);
                g2.drawString(t, esq - 8 - g2.getFontMetrics().stringWidth(t), y + 4);
            }
            int n = valores.size();
            if (n == 0) { g2.dispose(); return; }
            double passo = n > 1 ? (double) w / (n - 1) : 0;
            Path2D linha = new Path2D.Double(), area = new Path2D.Double();
            for (int i = 0; i < n; i++) {
                double x = esq + i * passo, y = topo + h - (double) h * valores.get(i) / max;
                if (i == 0) { linha.moveTo(x, y); area.moveTo(x, topo + h); }
                linha.lineTo(x, y); area.lineTo(x, y);
            }
            area.lineTo(esq + (n - 1) * passo, topo + h); area.closePath();
            g2.setPaint(new GradientPaint(0, topo, new Color(0x25, 0x63, 0xEB, 70), 0, topo + h, new Color(0x25, 0x63, 0xEB, 0)));
            g2.fill(area);
            g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(Tema.AZUL);
            g2.draw(linha);
            for (int i = 0; i < n; i++) {
                double x = esq + i * passo, y = topo + h - (double) h * valores.get(i) / max;
                g2.setColor(Color.WHITE); g2.fill(new Ellipse2D.Double(x - 4.5, y - 4.5, 9, 9));
                g2.setColor(Tema.AZUL); g2.setStroke(new BasicStroke(2f)); g2.draw(new Ellipse2D.Double(x - 4.5, y - 4.5, 9, 9));
                g2.setColor(Tema.TEXTO_SUAVE);
                String r = rotulos.get(i);
                g2.drawString(r, (int) x - g2.getFontMetrics().stringWidth(r) / 2, topo + h + 20);
            }
            g2.dispose();
        }
    }

    /** Barras verticais com linha de meta opcional (ex.: vendas mensais vs meta). */
    static final class Barras extends JComponent {
        private List<String> rotulos = List.of();
        private List<Long> valores = List.of();
        private long meta;
        Barras() { setPreferredSize(new Dimension(400, 240)); setOpaque(false); }
        void definir(List<String> r, List<Long> v, long meta) { rotulos = r; valores = v; this.meta = meta; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = preparar(g);
            int esq = 62, dir = 10, topo = 26, base = 26;
            int w = getWidth() - esq - dir, h = getHeight() - topo - base;
            long max = teto(Math.max(meta, valores.stream().mapToLong(Long::longValue).max().orElse(0)));
            g2.setFont(Tema.sans(11, Font.PLAIN));
            for (int i = 0; i <= 4; i++) {
                int y = topo + h - h * i / 4;
                g2.setColor(Tema.BORDA); g2.drawLine(esq, y, esq + w, y);
                g2.setColor(Tema.TEXTO_SUAVE);
                String t = compacto(max * i / 4);
                g2.drawString(t, esq - 8 - g2.getFontMetrics().stringWidth(t), y + 4);
            }
            int n = valores.size();
            if (n > 0) {
                double fatia = (double) w / n, larg = Math.min(34, fatia * 0.58);
                for (int i = 0; i < n; i++) {
                    double bh = (double) h * valores.get(i) / max, x = esq + i * fatia + (fatia - larg) / 2;
                    g2.setPaint(new GradientPaint(0, (float) (topo + h - bh), Tema.CIANO, 0, topo + h, Tema.AZUL));
                    g2.fill(new RoundRectangle2D.Double(x, topo + h - bh, larg, bh, 7, 7));
                    g2.setColor(Tema.TEXTO_SUAVE);
                    String r = rotulos.get(i);
                    g2.drawString(r, (int) (x + larg / 2 - g2.getFontMetrics().stringWidth(r) / 2.0), topo + h + 18);
                }
            }
            if (meta > 0) {
                int y = (int) (topo + h - (double) h * meta / max);
                g2.setColor(Tema.ALERTA);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{7, 5}, 0));
                g2.drawLine(esq, y, esq + w, y);
                g2.setFont(Tema.sans(11, Font.BOLD));
                g2.drawString("Meta " + Dinheiro.formatar(meta), esq + w - 130, y - 6);
            }
            g2.dispose();
        }
    }

    /** Rosca (donut) com legenda e percentuais. */
    static final class Rosca extends JComponent {
        private Map<String, Long> dados = new LinkedHashMap<>();
        Rosca() { setPreferredSize(new Dimension(400, 220)); setOpaque(false); }
        void definir(Map<String, Long> d) { dados = d; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = preparar(g);
            long total = dados.values().stream().mapToLong(Long::longValue).sum();
            int d = Math.min(getHeight() - 20, getWidth() / 2 - 20);
            int x0 = 10, y0 = (getHeight() - d) / 2;
            if (total <= 0) {
                g2.setColor(Tema.TEXTO_SUAVE); g2.setFont(Tema.sans(13, Font.PLAIN));
                g2.drawString("Nenhuma venda no período", 20, getHeight() / 2);
                g2.dispose(); return;
            }
            double ang = 90; int i = 0;
            g2.setFont(Tema.sans(12, Font.PLAIN));
            int ly = Math.max(16, getHeight() / 2 - dados.size() * 13);
            for (var e : dados.entrySet()) {
                double ext = -360.0 * e.getValue() / total;
                g2.setColor(PALETA[i % PALETA.length]);
                g2.fill(new Arc2D.Double(x0, y0, d, d, ang, ext, Arc2D.PIE));
                ang += ext;
                int lx = x0 + d + 24;
                g2.fillRoundRect(lx, ly - 9, 10, 10, 3, 3);
                g2.setColor(Tema.TEXTO);
                double pct = 100.0 * e.getValue() / total;
                g2.drawString(e.getKey() + "  " + String.format("%.0f%%", pct), lx + 16, ly);
                g2.setColor(Tema.TEXTO_SUAVE);
                g2.drawString(Dinheiro.formatar(e.getValue()), lx + 16, ly + 14);
                ly += 34; i++;
            }
            g2.setColor(getParent() != null ? Color.WHITE : Tema.FUNDO);
            int furo = (int) (d * 0.58);
            g2.fillOval(x0 + (d - furo) / 2, y0 + (d - furo) / 2, furo, furo);
            g2.setColor(Tema.TEXTO); g2.setFont(Tema.titulo(13));
            String t = Graficos.compacto(total);
            g2.drawString(t, x0 + d / 2 - g2.getFontMetrics().stringWidth(t) / 2, y0 + d / 2 + 5);
            g2.dispose();
        }
    }

    /** Barras horizontais com valor e percentual (ex.: formas de pagamento). */
    static final class BarrasHorizontais extends JComponent {
        private Map<String, Long> dados = new LinkedHashMap<>();
        private boolean dinheiro = true;
        BarrasHorizontais() { setOpaque(false); setPreferredSize(new Dimension(400, 200)); }
        void definir(Map<String, Long> d, boolean emDinheiro) {
            dados = d; dinheiro = emDinheiro;
            setPreferredSize(new Dimension(400, Math.max(60, d.size() * 46)));
            revalidate(); repaint();
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = preparar(g);
            long total = dados.values().stream().mapToLong(Long::longValue).sum();
            long max = dados.values().stream().mapToLong(Long::longValue).max().orElse(1);
            int y = 4, w = getWidth() - 4, i = 0;
            for (var e : dados.entrySet()) {
                g2.setFont(Tema.sans(13, Font.PLAIN)); g2.setColor(Tema.TEXTO);
                g2.drawString(e.getKey(), 0, y + 14);
                String v = (dinheiro ? Dinheiro.formatar(e.getValue()) : e.getValue() + " un")
                    + (total > 0 ? String.format("   %.0f%%", 100.0 * e.getValue() / total) : "");
                g2.setFont(Tema.sans(12, Font.BOLD));
                g2.drawString(v, w - g2.getFontMetrics().stringWidth(v), y + 14);
                g2.setColor(Tema.AZUL_CLARO); g2.fillRoundRect(0, y + 22, w, 8, 8, 8);
                g2.setColor(PALETA[i % PALETA.length]);
                g2.fillRoundRect(0, y + 22, (int) (max <= 0 ? 0 : (double) w * e.getValue() / max), 8, 8, 8);
                y += 46; i++;
            }
            g2.dispose();
        }
    }

    /** Barra de progresso arredondada (meta). */
    static final class Progresso extends JComponent {
        private double pct;
        private final Color trilho, cor;
        Progresso(Color trilho, Color cor) { this.trilho = trilho; this.cor = cor; setPreferredSize(new Dimension(200, 8)); setOpaque(false); }
        void definir(double percentual) { pct = Math.max(0, Math.min(100, percentual)); repaint(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = preparar(g);
            g2.setColor(trilho); g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.setPaint(new GradientPaint(0, 0, Tema.AZUL, getWidth(), 0, cor));
            g2.fillRoundRect(0, 0, (int) (getWidth() * pct / 100), getHeight(), getHeight(), getHeight());
            g2.dispose();
        }
    }

    static List<String> chaves(Map<?, ?> m) { List<String> r = new ArrayList<>(); m.keySet().forEach(k -> r.add(String.valueOf(k))); return r; }
}
