package br.ufpb.aps.grupo12.interfacegrafica;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Base das páginas do menu: cabeçalho padrão e recarga dos dados ao ser exibida. */
abstract class Pagina extends JPanel {
    protected final Contexto ctx;
    protected final JPanel corpo = new JPanel(new BorderLayout(0, 16));

    Pagina(Contexto ctx, String titulo, String subtitulo) {
        super(new BorderLayout(0, 20));
        this.ctx = ctx;
        setBackground(Tema.FUNDO);
        setBorder(new EmptyBorder(28, 32, 28, 32));
        JPanel cab = new JPanel(new BorderLayout());
        cab.setOpaque(false);
        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 2));
        textos.setOpaque(false);
        textos.add(Tema.titulo(titulo));
        textos.add(Tema.subtitulo(subtitulo));
        cab.add(textos, BorderLayout.WEST);
        JComponent acoes = acoesCabecalho();
        if (acoes != null) cab.add(acoes, BorderLayout.EAST);
        add(cab, BorderLayout.NORTH);
        corpo.setOpaque(false);
        add(corpo, BorderLayout.CENTER);
    }

    /** Botões no canto superior direito (opcional). */
    protected JComponent acoesCabecalho() { return null; }

    /** Recarrega dados da Facade; chamada sempre que a página é aberta ou após uma operação. */
    abstract void atualizar();

    protected static JPanel linha(JComponent... componentes) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        p.setOpaque(false);
        for (JComponent c : componentes) p.add(c);
        return p;
    }

    /** Linha selecionada já convertida para o índice do modelo (a tabela pode estar ordenada). */
    protected static int selecionada(JTable t) {
        int v = t.getSelectedRow();
        return v < 0 ? -1 : t.convertRowIndexToModel(v);
    }
}
