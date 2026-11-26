package br.ufpb.aps.grupo12.interfacegrafica;

import br.ufpb.aps.grupo12.dominio.ErroNegocio;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Peças visuais reutilizadas pelas páginas. */
final class Componentes {
    private Componentes() {}
    static final Locale BR = Locale.of("pt", "BR");
    static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    static final DateTimeFormatter EXTENSO = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", BR);

    /** Quadrado arredondado colorido com ícone branco. */
    static JComponent selo(Icone.Tipo tipo, Color cor) {
        JComponent c = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(cor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                new Icone(tipo, 20, Color.WHITE).paintIcon(this, g2, (getWidth() - 20) / 2, (getHeight() - 20) / 2);
                g2.dispose();
            }
        };
        c.setPreferredSize(new Dimension(38, 38));
        return c;
    }

    /** Círculo com as iniciais do nome. */
    static JComponent avatar(String nome, int tam) {
        String[] partes = nome == null ? new String[0] : nome.trim().split("\\s+");
        String ini = partes.length == 0 || partes[0].isEmpty() ? "?" : (partes[0].substring(0, 1)
            + (partes.length > 1 ? partes[partes.length - 1].substring(0, 1) : "")).toUpperCase();
        Color cor = Graficos.PALETA[Math.abs(nome == null ? 0 : nome.hashCode()) % 5];
        JComponent c = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, cor, tam, tam, cor.darker()));
                g2.fillOval(0, 0, tam - 1, tam - 1);
                g2.setColor(Color.WHITE);
                g2.setFont(Tema.sans(tam * 0.38f, Font.BOLD));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(ini, (tam - fm.stringWidth(ini)) / 2, (tam - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        c.setPreferredSize(new Dimension(tam, tam));
        c.setMinimumSize(new Dimension(tam, tam));
        return c;
    }

    /** Etiqueta em forma de pílula (status). */
    static JLabel pilula(String texto, Color fundo, Color frente) {
        JLabel l = new JLabel(texto) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(fundo);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(Tema.sans(11, Font.BOLD));
        l.setForeground(frente);
        l.setBorder(new EmptyBorder(3, 10, 3, 10));
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return l;
    }

    static JLabel tituloSecao(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(Tema.titulo(16));
        l.setForeground(Tema.TEXTO);
        return l;
    }

    static JLabel secao(String texto) {
        JLabel l = new JLabel(texto.toUpperCase());
        l.setFont(Tema.sans(11, Font.BOLD));
        l.setForeground(Tema.TEXTO_SUAVE);
        l.setBorder(new EmptyBorder(0, 2, 0, 0));
        return l;
    }

    /** Cabeçalho de um bloco: título à esquerda, componente opcional à direita. */
    static JPanel cabecalho(String titulo, JComponent direita) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(tituloSecao(titulo), BorderLayout.WEST);
        if (direita != null) p.add(direita, BorderLayout.EAST);
        p.setBorder(new EmptyBorder(0, 0, 10, 0));
        return p;
    }

    static JButton link(String texto, Runnable acao) {
        JButton b = new JButton(texto);
        b.setFont(Tema.sans(12, Font.BOLD));
        b.setForeground(Tema.AZUL);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> acao.run());
        return b;
    }

    /** Botão alternável que pinta o próprio fundo (independe do Look and Feel). */
    static JToggleButton alternavel(String texto, Color fundoSel, Color textoSel) {
        JToggleButton b = new JToggleButton(texto) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isSelected() ? fundoSel : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(isSelected() ? fundoSel.darker() : Tema.BORDA);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFont(Tema.sans(13, Font.BOLD));
        b.setBorder(new EmptyBorder(8, 16, 8, 16));
        b.setForeground(Tema.TEXTO);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addItemListener(e -> b.setForeground(b.isSelected() ? textoSel : Tema.TEXTO));
        return b;
    }

    static JButton comIcone(JButton b, Icone.Tipo tipo) {
        b.setIcon(new Icone(tipo, 16, b.getForeground()));
        b.setIconTextGap(8);
        return b;
    }

    static LocalDate lerData(String texto, String campo) {
        try { return LocalDate.parse(texto.trim(), DATA); }
        catch (DateTimeParseException e) { throw new ErroNegocio(campo + ": use o formato dd/mm/aaaa."); }
    }

    static String capitalizar(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }

    /** Coloca o conteúdo numa rolagem vertical sem borda. */
    static JScrollPane rolagem(JComponent c) {
        JScrollPane s = new JScrollPane(c, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        s.setBorder(null);
        s.getViewport().setBackground(Tema.FUNDO);
        s.getVerticalScrollBar().setUnitIncrement(18);
        return s;
    }
}
