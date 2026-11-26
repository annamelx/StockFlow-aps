package br.ufpb.aps.grupo12.interfacegrafica;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Molde das telas de cadastro: lista pesquisável à esquerda e formulário à direita.
 * Cada subclasse informa campos, como preencher e quais operações da Facade chamar.
 */
abstract class PaginaListaDetalhe<T> extends Pagina {
    private final String entidade;
    private final JLabel contagem = Tema.subtitulo(" ");
    private final JTextField busca = Tema.campo(16);
    private final JPanel lista = new JPanel();
    private final CardLayout estados = new CardLayout();
    private final JPanel direita = new JPanel(estados);
    private final JLabel tituloForm = Componentes.tituloSecao("");
    protected final JPanel extras = new JPanel(new BorderLayout());
    private final JButton excluir = Tema.botaoPerigo("Excluir");
    private List<T> itens = List.of();
    protected T selecionado;
    private boolean novo;

    PaginaListaDetalhe(Contexto ctx, String titulo, String entidade, String artigoNovo) {
        this(ctx, titulo, entidade, artigoNovo, Icone.Tipo.PESSOA);
    }
    PaginaListaDetalhe(Contexto ctx, String titulo, String entidade, String artigoNovo, Icone.Tipo icone) {
        super(ctx, titulo, "cadastro");
        this.entidade = entidade;
        JPanel cab = new JPanel(new BorderLayout());
        cab.setOpaque(false);
        JPanel t = new JPanel(new GridLayout(2, 1));
        t.setOpaque(false);
        JLabel h = new JLabel("Gestão de " + titulo);
        h.setFont(Tema.titulo(20));
        t.add(h); t.add(contagem);
        cab.add(t, BorderLayout.WEST);
        JButton botaoNovo = Componentes.comIcone(Tema.botaoPrimario(artigoNovo), Icone.Tipo.MAIS);
        botaoNovo.addActionListener(e -> abrirNovo());
        cab.add(botaoNovo, BorderLayout.EAST);
        add(cab, BorderLayout.NORTH);

        JPanel esquerda = new JPanel(new BorderLayout(0, 12));
        esquerda.setOpaque(false);
        esquerda.setPreferredSize(new Dimension(340, 0));
        JPanel b = new JPanel(new BorderLayout(8, 0));
        b.setOpaque(false);
        b.add(new JLabel(new Icone(Icone.Tipo.BUSCA, 18, Tema.TEXTO_SUAVE)), BorderLayout.WEST);
        b.add(busca, BorderLayout.CENTER);
        esquerda.add(b, BorderLayout.NORTH);
        lista.setOpaque(false);
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        JPanel topoLista = new JPanel(new BorderLayout());
        topoLista.setOpaque(false);
        topoLista.add(lista, BorderLayout.NORTH);
        esquerda.add(Componentes.rolagem(topoLista), BorderLayout.CENTER);
        busca.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { montarLista(); }
            public void removeUpdate(DocumentEvent e) { montarLista(); }
            public void changedUpdate(DocumentEvent e) { montarLista(); }
        });

        Cartao vazio = new Cartao(new GridBagLayout());
        JLabel msg = new JLabel("Selecione um(a) " + entidade + " ou cadastre um(a) novo(a)",
            new Icone(icone, 40, Tema.AZUL), SwingConstants.CENTER);
        msg.setVerticalTextPosition(SwingConstants.BOTTOM);
        msg.setHorizontalTextPosition(SwingConstants.CENTER);
        msg.setIconTextGap(14);
        msg.setFont(Tema.sans(14, Font.PLAIN));
        msg.setForeground(Tema.TEXTO_SUAVE);
        vazio.add(msg);
        direita.setOpaque(false);
        direita.add(vazio, "vazio");

        JPanel centro = new JPanel(new BorderLayout(18, 0));
        centro.setOpaque(false);
        centro.add(esquerda, BorderLayout.WEST);
        centro.add(direita, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);
    }

    /** Deve ser chamado no FIM do construtor da subclasse (os campos já existem). */
    protected final void montarFormulario() { direita.add(formulario(), "form"); }

    // ----- ganchos das subclasses -----
    abstract List<T> carregar();
    abstract String nome(T t);
    abstract String detalhe(T t);
    abstract String[] rotulos();
    abstract JComponent[] campos();
    /** Preenche o formulário; {@code t == null} significa novo cadastro. */
    abstract void preencher(T t);
    abstract void salvar(boolean novo);
    /** Exclusão; subclasses sem exclusão devolvem false em {@link #permiteExcluir()}. */
    void excluir(T t) {}
    boolean permiteExcluir() { return true; }
    /** Etiqueta opcional à direita do item da lista. */
    JComponent etiqueta(T t) { return null; }

    private JComponent formulario() {
        Cartao c = new Cartao(new BorderLayout(0, 16));
        c.add(tituloForm, BorderLayout.NORTH);
        JPanel f = new JPanel(new GridBagLayout());
        f.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 4, 6, 4);
        g.anchor = GridBagConstraints.WEST;
        String[] r = rotulos();
        JComponent[] cs = campos();
        for (int i = 0; i < r.length; i++) {
            g.gridy = i; g.gridx = 0; g.weightx = 0; g.fill = GridBagConstraints.NONE;
            f.add(Tema.rotulo(r[i]), g);
            g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
            f.add(cs[i], g);
        }
        g.gridy = r.length; g.gridx = 0; g.gridwidth = 2; g.weighty = 1; g.fill = GridBagConstraints.BOTH;
        extras.setOpaque(false);
        f.add(extras, g);
        c.add(f, BorderLayout.CENTER);
        JButton salvar = Tema.botaoPrimario("Salvar");
        salvar.addActionListener(e -> {
            boolean ok = executar(() -> salvar(novo), entidade.substring(0, 1).toUpperCase() + entidade.substring(1) + " salvo(a).");
            if (ok) { novo = false; }
        });
        excluir.addActionListener(e -> {
            if (selecionado == null) return;
            if (JOptionPane.showConfirmDialog(this, "Excluir " + nome(selecionado) + "?", "Confirmar exclusão",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            T alvo = selecionado;
            if (executar(() -> excluir(alvo), "Excluído(a).")) { selecionado = null; estados.show(direita, "vazio"); }
        });
        JButton cancelar = Tema.botaoSecundario("Fechar");
        cancelar.addActionListener(e -> { selecionado = null; estados.show(direita, "vazio"); montarLista(); });
        c.add(linha(excluir, cancelar, salvar), BorderLayout.SOUTH);
        return c;
    }

    private void abrirNovo() {
        selecionado = null;
        novo = true;
        tituloForm.setText("Novo(a) " + entidade);
        excluir.setVisible(false);
        preencher(null);
        estados.show(direita, "form");
        montarLista();
    }

    private void abrir(T t) {
        selecionado = t;
        novo = false;
        tituloForm.setText(nome(t));
        excluir.setVisible(permiteExcluir());
        preencher(t);
        estados.show(direita, "form");
        montarLista();
    }

    @Override void atualizar() {
        itens = carregar();
        contagem.setText(itens.size() + " " + entidade + "(s) cadastrado(s)");
        if (selecionado != null) {
            String chave = detalhe(selecionado);
            selecionado = itens.stream().filter(x -> detalhe(x).equals(chave)).findFirst().orElse(null);
            if (selecionado != null) { tituloForm.setText(nome(selecionado)); preencher(selecionado); }
        }
        montarLista();
    }

    private void montarLista() {
        lista.removeAll();
        String q = busca.getText().trim().toLowerCase();
        for (T t : itens) {
            if (!q.isEmpty() && !(nome(t).toLowerCase().contains(q) || detalhe(t).toLowerCase().contains(q))) continue;
            boolean ativo = selecionado != null && detalhe(selecionado).equals(detalhe(t));
            Cartao linha = new Cartao(new BorderLayout(12, 0), 10);
            if (ativo) linha.cores(Tema.AZUL_CLARO, Tema.AZUL);
            linha.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
            linha.add(Componentes.avatar(nome(t), 38), BorderLayout.WEST);
            JLabel txt = new JLabel("<html><b>" + nome(t) + "</b><br><font face='monospaced' color='#64748B' size='2'>"
                + detalhe(t) + "</font></html>");
            txt.setFont(Tema.sans(13, Font.PLAIN));
            linha.add(txt, BorderLayout.CENTER);
            JComponent et = etiqueta(t);
            if (et != null) { JPanel w = new JPanel(new GridBagLayout()); w.setOpaque(false); w.add(et); linha.add(w, BorderLayout.EAST); }
            linha.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            linha.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { abrir(t); } });
            linha.setAlignmentX(Component.LEFT_ALIGNMENT);
            lista.add(linha);
            lista.add(Box.createVerticalStrut(8));
        }
        lista.revalidate();
        lista.repaint();
    }
}
