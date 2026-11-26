package br.ufpb.aps.grupo12.interfacegrafica;

import br.ufpb.aps.grupo12.dominio.ErroNegocio;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

/** Identidade visual do StockFlow e utilitários de tela. Sem regras de negócio. */
final class Tema {
    private static final Logger LOG = Logger.getLogger(Tema.class.getName());
    private Tema() {}

    // Identidade visual própria do StockFlow: azul-marinho/preto com destaque azul elétrico.
    static final Color NOITE = new Color(0x070B16);        // quase preto
    static final Color MARINHO = new Color(0x0F1B3D);      // azul-marinho
    static final Color AZUL = new Color(0x2563EB);         // ação principal
    static final Color AZUL_ESCURO = new Color(0x1D4ED8);
    static final Color AZUL_CLARO = new Color(0xE8F0FE);   // seleção / destaques suaves
    static final Color CIANO = new Color(0x38BDF8);        // detalhe da marca
    static final Color FUNDO = new Color(0xF4F7FC);
    static final Color BORDA = new Color(0xDCE3EF);
    static final Color TEXTO = new Color(0x0F172A);
    static final Color TEXTO_SUAVE = new Color(0x64748B);
    static final Color VERDE = new Color(0x059669);
    static final Color ALERTA = new Color(0xDC2626);

    /** Títulos: sans-serif em negrito (visual técnico/moderno). */
    static Font titulo(float tamanho) {
        return new Font(disponivel("Segoe UI Semibold", "Segoe UI", "Inter", "DejaVu Sans", Font.SANS_SERIF), Font.BOLD, Math.round(tamanho));
    }
    static Font sans(float tamanho, int estilo) {
        return new Font(disponivel("Segoe UI", "Inter", "DejaVu Sans", Font.SANS_SERIF), estilo, Math.round(tamanho));
    }
    private static String disponivel(String... nomes) {
        try {
            var instalados = java.util.Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames());
            for (String n : nomes) if (instalados.contains(n)) return n;
        } catch (RuntimeException e) {
            LOG.log(Level.FINE, "Lista de fontes indisponível", e);
        }
        return nomes[nomes.length - 1];
    }

    static JLabel titulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(titulo(24));
        l.setForeground(TEXTO);
        return l;
    }
    static JLabel subtitulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(sans(13, Font.PLAIN));
        l.setForeground(TEXTO_SUAVE);
        return l;
    }
    static JLabel rotulo(String texto) {
        JLabel l = new JLabel(texto.toUpperCase());
        l.setFont(sans(11, Font.BOLD));
        l.setForeground(TEXTO);
        return l;
    }

    static JButton botaoPrimario(String texto) { return botao(texto, AZUL, Color.WHITE, AZUL_ESCURO); }
    static JButton botaoSecundario(String texto) { return botao(texto, Color.WHITE, TEXTO, BORDA); }
    static JButton botaoPerigo(String texto) { return botao(texto, Color.WHITE, ALERTA, new Color(0xF5C2C2)); }

    private static JButton botao(String texto, Color fundo, Color frente, Color borda) {
        JButton b = new JButton(texto);
        b.setFont(sans(13, Font.BOLD));
        b.setBackground(fundo);
        b.setForeground(frente);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new CompoundBorder(new LineBorder(borda, 1, true), new EmptyBorder(9, 18, 9, 18)));
        return b;
    }

    static JTextField campo(int colunas) {
        JTextField t = new JTextField(colunas);
        estilizarCampo(t);
        return t;
    }
    static void estilizarCampo(JComponent c) {
        c.setFont(sans(14, Font.PLAIN));
        c.setBackground(Color.WHITE);
        c.setForeground(TEXTO);
        if (!(c instanceof JComboBox<?>) && !(c instanceof JSpinner))
            c.setBorder(new CompoundBorder(new LineBorder(BORDA, 1, true), new EmptyBorder(8, 10, 8, 10)));
    }

    /** Caixa branca com borda suave, como os cartões do protótipo. */
    static JPanel cartao(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(new LineBorder(BORDA, 1, true), new EmptyBorder(16, 18, 16, 18)));
        return p;
    }

    static JScrollPane tabela(JTable t) {
        t.setFont(sans(13, Font.PLAIN));
        t.setRowHeight(32);
        t.setShowVerticalLines(false);
        t.setGridColor(BORDA);
        t.setSelectionBackground(AZUL_CLARO);
        t.setSelectionForeground(TEXTO);
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(true);
        JTableHeader h = t.getTableHeader();
        h.setFont(sans(12, Font.BOLD));
        h.setBackground(FUNDO);
        h.setForeground(TEXTO_SUAVE);
        h.setReorderingAllowed(false);
        h.setBorder(new MatteBorder(0, 0, 1, 0, BORDA));
        JScrollPane s = new JScrollPane(t);
        s.setBorder(new LineBorder(BORDA, 1, true));
        s.getViewport().setBackground(Color.WHITE);
        return s;
    }

    /**
     * Executa uma ação da tela. Erros de negócio viram avisos compreensíveis; falhas inesperadas
     * (ex.: persistência) são registradas no log e mostradas com a causa — nunca silenciadas.
     */
    static boolean executar(Component pai, Runnable acao, String mensagemSucesso) {
        try {
            acao.run();
            if (mensagemSucesso != null)
                JOptionPane.showMessageDialog(pai, mensagemSucesso, "Tudo certo", JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (ErroNegocio e) {
            JOptionPane.showMessageDialog(pai, e.getMessage(), "Operação não realizada", JOptionPane.WARNING_MESSAGE);
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Falha inesperada na interface", e);
            JOptionPane.showMessageDialog(pai, "Falha inesperada: " + e.getMessage()
                + (e.getCause() != null ? "\nCausa: " + e.getCause().getMessage() : ""),
                "Erro", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    static int inteiro(String texto, String campo) {
        try { return Integer.parseInt(texto.trim()); }
        catch (NumberFormatException e) { throw new ErroNegocio(campo + " deve ser um número inteiro."); }
    }
}
