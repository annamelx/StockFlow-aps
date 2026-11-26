package br.ufpb.aps.grupo12.interfacegrafica;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Base das páginas: título/subtítulo mostrados na barra superior e recarga ao abrir. */
abstract class Pagina extends JPanel {
    protected final Contexto ctx;
    final String titulo;
    final String subtitulo;

    Pagina(Contexto ctx, String titulo, String subtitulo) {
        super(new BorderLayout(0, 18));
        this.ctx = ctx;
        this.titulo = titulo;
        this.subtitulo = subtitulo;
        setBackground(Tema.FUNDO);
        setBorder(new EmptyBorder(24, 28, 24, 28));
    }

    /** Recarrega os dados pela Facade; chamada ao abrir a página e após cada operação. */
    abstract void atualizar();

    protected static JPanel linha(JComponent... componentes) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        p.setOpaque(false);
        for (JComponent c : componentes) p.add(c);
        return p;
    }
    protected static JPanel grade(int colunas, int espaco, JComponent... itens) {
        JPanel p = new JPanel(new GridLayout(1, colunas, espaco, 0));
        p.setOpaque(false);
        for (JComponent c : itens) p.add(c);
        return p;
    }
    /** Linha selecionada já convertida para o índice do modelo (a tabela pode estar ordenada). */
    protected static int selecionada(JTable t) {
        int v = t.getSelectedRow();
        return v < 0 ? -1 : t.convertRowIndexToModel(v);
    }
    protected boolean executar(Runnable acao, String sucesso) {
        boolean ok = Tema.executar(this, acao, sucesso);
        atualizar();
        return ok;
    }
}
