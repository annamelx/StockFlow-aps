package br.ufpb.aps.grupo12.interfacegrafica;

import br.ufpb.aps.grupo12.dominio.*;
import br.ufpb.aps.grupo12.politicas.PoliticaDesconto;
import br.ufpb.aps.grupo12.politicas.fabricas.CriadorPoliticaDesconto;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * PDV / Caixa (UC02). A tela monta o carrinho; o resumo (subtotal, desconto rateado, total) vem de
 * LojaFacade.simularVenda — o mesmo cálculo usado ao gravar. A política vem do Factory Method.
 */
final class PaginaPdv extends Pagina {
    private final Runnable aoConcluir;
    private final JTextField busca = Tema.campo(20);
    private final JPanel grade = new JPanel(new GridLayout(0, 3, 12, 12));
    private final Map<String, Integer> carrinho = new LinkedHashMap<>();
    private final JPanel itensCarrinho = new JPanel();
    private final JComboBox<Object> cliente = new JComboBox<>();
    private final Map<FormaPagamento, JToggleButton> formas = new EnumMap<>(FormaPagamento.class);
    private final JComboBox<CriadorPoliticaDesconto> politica;
    private final JTextField parametro = Tema.campo(6);
    private final JLabel subtotal = valor(), desconto = valor(), total = new JLabel("R$ 0,00"), aviso = new JLabel(" ");
    private final JTextField idVenda = Tema.campo(6);
    private List<Produto> produtos = List.of();

    PaginaPdv(Contexto ctx, Runnable aoConcluir) {
        super(ctx, "PDV / Caixa", "registrar venda");
        this.aoConcluir = aoConcluir;
        politica = new JComboBox<>(ctx.descontos().criadores().toArray(CriadorPoliticaDesconto[]::new));
        Tema.estilizarCampo(politica);
        Tema.estilizarCampo(cliente);

        // ----- catálogo -----
        JPanel esquerda = new JPanel(new BorderLayout(0, 14));
        esquerda.setOpaque(false);
        busca.putClientProperty("JTextField.placeholderText", "Buscar produto");
        JPanel linhaBusca = new JPanel(new BorderLayout(10, 0));
        linhaBusca.setOpaque(false);
        JLabel lupa = new JLabel(new Icone(Icone.Tipo.BUSCA, 18, Tema.TEXTO_SUAVE));
        linhaBusca.add(lupa, BorderLayout.WEST);
        linhaBusca.add(busca, BorderLayout.CENTER);
        esquerda.add(linhaBusca, BorderLayout.NORTH);
        busca.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { montarGrade(); }
            public void removeUpdate(DocumentEvent e) { montarGrade(); }
            public void changedUpdate(DocumentEvent e) { montarGrade(); }
        });
        grade.setOpaque(false);
        JPanel gradeTopo = new JPanel(new BorderLayout());
        gradeTopo.setOpaque(false);
        gradeTopo.add(grade, BorderLayout.NORTH);
        esquerda.add(Componentes.rolagem(gradeTopo), BorderLayout.CENTER);
        add(esquerda, BorderLayout.CENTER);

        // ----- carrinho -----
        Cartao direita = new Cartao(new BorderLayout(0, 12));
        direita.setPreferredSize(new Dimension(400, 0));
        JPanel cab = new JPanel(new BorderLayout());
        cab.setOpaque(false);
        JLabel tc = Componentes.tituloSecao("Carrinho");
        tc.setIcon(new Icone(Icone.Tipo.CARRINHO, 18, Tema.AZUL));
        tc.setIconTextGap(8);
        cab.add(tc, BorderLayout.WEST);
        JPanel idp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        idp.setOpaque(false);
        idp.add(Tema.subtitulo("Venda nº"));
        idVenda.setColumns(5);
        idp.add(idVenda);
        cab.add(idp, BorderLayout.EAST);
        direita.add(cab, BorderLayout.NORTH);

        itensCarrinho.setOpaque(false);
        itensCarrinho.setLayout(new BoxLayout(itensCarrinho, BoxLayout.Y_AXIS));
        JPanel itensTopo = new JPanel(new BorderLayout());
        itensTopo.setOpaque(false);
        itensTopo.add(itensCarrinho, BorderLayout.NORTH);
        JScrollPane rolagemItens = Componentes.rolagem(itensTopo);
        rolagemItens.getViewport().setBackground(Color.WHITE);
        direita.add(rolagemItens, BorderLayout.CENTER);

        JPanel baixo = new JPanel();
        baixo.setOpaque(false);
        baixo.setLayout(new BoxLayout(baixo, BoxLayout.Y_AXIS));
        baixo.add(campo("Cliente", cliente));
        JPanel pag = new JPanel(new GridLayout(2, 2, 6, 6));
        pag.setOpaque(false);
        ButtonGroup grupo = new ButtonGroup();
        for (FormaPagamento f : FormaPagamento.values()) {
            JToggleButton b = Componentes.alternavel(f.rotulo(), Tema.AZUL_CLARO, Tema.AZUL_ESCURO);
            b.setFont(Tema.sans(12, Font.BOLD));
            b.setBorder(new EmptyBorder(7, 6, 7, 6));
            grupo.add(b);
            pag.add(b);
            formas.put(f, b);
        }
        formas.get(FormaPagamento.PIX).setSelected(true);
        baixo.add(campo("Forma de pagamento", pag));
        JPanel desc = new JPanel(new BorderLayout(8, 0));
        desc.setOpaque(false);
        desc.add(politica, BorderLayout.CENTER);
        parametro.setColumns(6);
        desc.add(parametro, BorderLayout.EAST);
        baixo.add(campo("Desconto", desc));
        politica.addActionListener(e -> { ajustarParametro(); recalcular(); });
        parametro.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { recalcular(); }
            public void removeUpdate(DocumentEvent e) { recalcular(); }
            public void changedUpdate(DocumentEvent e) { recalcular(); }
        });

        JPanel resumo = new JPanel(new GridLayout(2, 2, 0, 4));
        resumo.setOpaque(false);
        resumo.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, Tema.BORDA), new EmptyBorder(10, 0, 4, 0)));
        resumo.setAlignmentX(Component.LEFT_ALIGNMENT);
        resumo.add(Tema.subtitulo("Subtotal")); resumo.add(subtotal);
        resumo.add(Tema.subtitulo("Desconto")); resumo.add(desconto);
        baixo.add(resumo);
        JPanel tot = new JPanel(new BorderLayout());
        tot.setOpaque(false);
        JLabel tl = new JLabel("Total");
        tl.setFont(Tema.titulo(16));
        tot.add(tl, BorderLayout.WEST);
        total.setFont(Tema.titulo(26));
        total.setForeground(Tema.AZUL_ESCURO);
        tot.add(total, BorderLayout.EAST);
        tot.setAlignmentX(Component.LEFT_ALIGNMENT);
        baixo.add(tot);
        aviso.setFont(Tema.sans(11, Font.PLAIN));
        aviso.setForeground(Tema.ALERTA);
        aviso.setAlignmentX(Component.LEFT_ALIGNMENT);
        baixo.add(aviso);
        baixo.add(Box.createVerticalStrut(8));
        JButton finalizar = Componentes.comIcone(Tema.botaoPrimario("Finalizar"), Icone.Tipo.CARRINHO);
        JButton pendente = Tema.botaoSecundario("Deixar pendente");
        finalizar.addActionListener(e -> registrar(true));
        pendente.addActionListener(e -> registrar(false));
        JPanel botoes = new JPanel(new GridLayout(1, 2, 8, 0));
        botoes.setOpaque(false);
        botoes.add(pendente); botoes.add(finalizar);
        botoes.setAlignmentX(Component.LEFT_ALIGNMENT);
        baixo.add(botoes);
        direita.add(baixo, BorderLayout.SOUTH);
        JPanel envolve = new JPanel(new BorderLayout());
        envolve.setOpaque(false);
        envolve.setBorder(new EmptyBorder(0, 18, 0, 0));
        envolve.add(direita);
        add(envolve, BorderLayout.EAST);
        ajustarParametro();
    }

    private static JLabel valor() {
        JLabel l = new JLabel("R$ 0,00", SwingConstants.RIGHT);
        l.setFont(Tema.sans(13, Font.BOLD));
        return l;
    }
    private static JPanel campo(String rotulo, JComponent c) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 0, 10, 0));
        p.add(Tema.rotulo(rotulo), BorderLayout.NORTH);
        p.add(c, BorderLayout.CENTER);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }
    private void ajustarParametro() {
        CriadorPoliticaDesconto c = (CriadorPoliticaDesconto) politica.getSelectedItem();
        parametro.setEnabled(c != null && c.exigeParametro());
        parametro.setToolTipText(c == null ? "" : c.rotuloParametro());
        if (c != null && !c.exigeParametro()) parametro.setText("");
        if (c != null && c.exigeParametro() && parametro.getText().isBlank()) parametro.setText("0");
    }

    @Override void atualizar() {
        produtos = ctx.facade().estoque(ctx.perfil());
        Object sel = cliente.getSelectedItem();
        DefaultComboBoxModel<Object> m = new DefaultComboBoxModel<>();
        m.addElement("Consumidor final (sem cadastro)");
        ctx.facade().clientes(ctx.perfil()).forEach(m::addElement);
        cliente.setModel(m);
        cliente.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v, i, s, f);
                if (v instanceof Cliente c) setText(c.nome() + "  (" + c.id() + ")");
                return this;
            }
        });
        if (sel != null) cliente.setSelectedItem(sel);
        if (idVenda.getText().isBlank()) idVenda.setText(ctx.facade().proximoId(ctx.perfil(), "V"));
        carrinho.keySet().removeIf(id -> produtos.stream().noneMatch(p -> p.id().equals(id)));
        montarGrade();
        montarCarrinho();
    }

    private void montarGrade() {
        grade.removeAll();
        String filtro = busca.getText().trim().toLowerCase();
        for (Produto p : produtos) {
            if (!filtro.isEmpty() && !(p.nome().toLowerCase().contains(filtro) || p.id().toLowerCase().contains(filtro))) continue;
            grade.add(cartaoProduto(p));
        }
        if (grade.getComponentCount() == 0) grade.add(Tema.subtitulo("Nenhum produto encontrado."));
        grade.revalidate();
        grade.repaint();
    }

    private JComponent cartaoProduto(Produto p) {
        Cartao c = new Cartao(new BorderLayout(0, 8), 14);
        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JPanel topo = new JPanel(new BorderLayout());
        topo.setOpaque(false);
        topo.add(Componentes.avatar(p.nome(), 36), BorderLayout.WEST);
        topo.add(p.estoque() == 0 ? Componentes.pilula("Esgotado", new Color(0xFEE2E2), Tema.ALERTA)
            : p.emAlerta() ? Componentes.pilula(p.estoque() + " un", new Color(0xFEF3C7), new Color(0xB45309))
            : Componentes.pilula(p.estoque() + " un", Tema.AZUL_CLARO, Tema.AZUL_ESCURO), BorderLayout.EAST);
        c.add(topo, BorderLayout.NORTH);
        JLabel nome = new JLabel("<html><b>" + p.nome() + "</b><br><font color='#64748B' size='2'>" + p.id() + "</font></html>");
        nome.setFont(Tema.sans(13, Font.PLAIN));
        c.add(nome, BorderLayout.CENTER);
        JLabel preco = new JLabel(Dinheiro.formatar(p.precoCentavos()));
        preco.setFont(Tema.titulo(16));
        preco.setForeground(Tema.AZUL_ESCURO);
        c.add(preco, BorderLayout.SOUTH);
        c.setPreferredSize(new Dimension(180, 132));
        c.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { alterar(p.id(), 1); }
            @Override public void mouseEntered(MouseEvent e) { c.cores(Tema.AZUL_CLARO, Tema.AZUL); c.repaint(); }
            @Override public void mouseExited(MouseEvent e) { c.cores(Color.WHITE, Tema.BORDA); c.repaint(); }
        });
        return c;
    }

    private void alterar(String id, int delta) {
        int nova = carrinho.getOrDefault(id, 0) + delta;
        if (nova <= 0) carrinho.remove(id); else carrinho.put(id, nova);
        montarCarrinho();
    }

    private void montarCarrinho() {
        itensCarrinho.removeAll();
        if (carrinho.isEmpty()) {
            JLabel vazio = Tema.subtitulo("Clique nos produtos para adicionar.");
            vazio.setBorder(new EmptyBorder(16, 0, 16, 0));
            itensCarrinho.add(vazio);
        }
        for (var e : carrinho.entrySet()) {
            Produto p = produtos.stream().filter(x -> x.id().equals(e.getKey())).findFirst().orElse(null);
            if (p == null) continue;
            JPanel l = new JPanel(new BorderLayout(8, 0));
            l.setOpaque(false);
            l.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Tema.BORDA), new EmptyBorder(8, 0, 8, 0)));
            l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
            JLabel n = new JLabel("<html><b>" + p.nome() + "</b><br><font color='#64748B'>"
                + Dinheiro.formatar(p.precoCentavos()) + " un.</font></html>");
            n.setFont(Tema.sans(12, Font.PLAIN));
            l.add(n, BorderLayout.CENTER);
            JPanel qtd = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            qtd.setOpaque(false);
            JButton menos = mini("−"), mais = mini("+");
            menos.addActionListener(a -> alterar(p.id(), -1));
            mais.addActionListener(a -> alterar(p.id(), 1));
            JLabel q = new JLabel(String.valueOf(e.getValue()));
            q.setFont(Tema.sans(13, Font.BOLD));
            q.setPreferredSize(new Dimension(24, 20));
            q.setHorizontalAlignment(SwingConstants.CENTER);
            qtd.add(menos); qtd.add(q); qtd.add(mais);
            l.add(qtd, BorderLayout.EAST);
            itensCarrinho.add(l);
        }
        itensCarrinho.revalidate();
        itensCarrinho.repaint();
        recalcular();
    }

    private static JButton mini(String t) {
        JButton b = new JButton(t);
        b.setFont(Tema.sans(14, Font.BOLD));
        b.setForeground(Tema.AZUL_ESCURO);
        b.setBackground(Tema.AZUL_CLARO);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(2, 9, 2, 9));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** Pede ao serviço o cálculo da venda (sem gravar) para exibir subtotal, desconto e total. */
    private void recalcular() {
        aviso.setText(" ");
        if (carrinho.isEmpty()) { subtotal.setText("R$ 0,00"); desconto.setText("R$ 0,00"); total.setText("R$ 0,00"); return; }
        try {
            Venda previa = ctx.facade().simularVenda(ctx.perfil(), carrinho, criarPolitica());
            subtotal.setText(Dinheiro.formatar(previa.subtotalCentavos()));
            desconto.setText("− " + Dinheiro.formatar(previa.descontoCentavos()));
            total.setText(Dinheiro.formatar(previa.totalCentavos()));
        } catch (ErroNegocio e) {
            aviso.setText(e.getMessage());
            total.setText("—");
        }
    }

    private PoliticaDesconto criarPolitica() {
        CriadorPoliticaDesconto c = (CriadorPoliticaDesconto) politica.getSelectedItem();
        return c.criar(parametro.getText()); // Factory Method
    }

    private FormaPagamento formaSelecionada() {
        return formas.entrySet().stream().filter(e -> e.getValue().isSelected()).map(Map.Entry::getKey)
            .findFirst().orElse(FormaPagamento.DINHEIRO);
    }

    private void registrar(boolean confirmar) {
        if (carrinho.isEmpty()) { JOptionPane.showMessageDialog(this, "Adicione ao menos um produto."); return; }
        Venda[] aberta = new Venda[1];
        boolean ok = Tema.executar(this, () -> aberta[0] = ctx.facade().abrirVenda(ctx.perfil(), idVenda.getText().trim(),
            carrinho, criarPolitica(), cliente.getSelectedItem() instanceof Cliente c ? c.id() : "",
            formaSelecionada(), ctx.usuario()), null);
        if (!ok) return;
        Venda v = aberta[0];
        String msg = "Venda " + v.id() + " registrada.\n\nSubtotal: " + Dinheiro.formatar(v.subtotalCentavos())
            + "\nDesconto (" + v.nomePolitica() + "): " + Dinheiro.formatar(v.descontoCentavos())
            + "\nTotal: " + Dinheiro.formatar(v.totalCentavos()) + "\nPagamento: " + v.formaPagamento();
        if (confirmar) {
            boolean conf = Tema.executar(this, () -> ctx.facade().confirmar(ctx.perfil(), v.id()), null);
            msg += conf ? "\n\n✔ Venda confirmada e estoque baixado." : "\n\nA venda ficou PENDENTE (veja o motivo acima).";
        } else msg += "\n\nSituação: PENDENTE (confirme no Histórico de Vendas).";
        JOptionPane.showMessageDialog(this, msg, "Venda", JOptionPane.INFORMATION_MESSAGE);
        carrinho.clear();
        idVenda.setText("");
        cliente.setSelectedIndex(0);
        atualizar();
        aoConcluir.run();
    }
}
