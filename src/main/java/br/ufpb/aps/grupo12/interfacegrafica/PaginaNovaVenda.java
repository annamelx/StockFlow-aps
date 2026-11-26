package br.ufpb.aps.grupo12.interfacegrafica;

import br.ufpb.aps.grupo12.dominio.*;
import br.ufpb.aps.grupo12.politicas.PoliticaDesconto;
import br.ufpb.aps.grupo12.politicas.fabricas.CriadorPoliticaDesconto;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * UC02 — montar e abrir venda. A tela só junta os dados; preço, desconto, rateio e total
 * são calculados pelo ServicoLoja. A política é criada pelo Factory Method escolhido no combo.
 */
final class PaginaNovaVenda extends Pagina {
    private record ItemCarrinho(Produto produto, int quantidade) {}
    private final Runnable aoConcluir;
    private final JTextField idVenda = Tema.campo(10);
    private final JComboBox<Produto> produto = new JComboBox<>();
    private final JSpinner quantidade = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
    private final JComboBox<CriadorPoliticaDesconto> politica;
    private final JLabel rotuloParametro = Tema.rotulo("Parâmetro");
    private final JTextField parametro = Tema.campo(8);
    private final JCheckBox confirmarJa = new JCheckBox("Confirmar a venda logo após abrir (baixa o estoque)");
    private final Map<String, ItemCarrinho> carrinho = new LinkedHashMap<>();
    private final ModeloTabela<ItemCarrinho> modelo = new ModeloTabela<>(
        new ModeloTabela.Coluna<>("Código", i -> i.produto().id()),
        new ModeloTabela.Coluna<>("Produto", i -> i.produto().nome()),
        new ModeloTabela.Coluna<>("Qtd.", ItemCarrinho::quantidade),
        new ModeloTabela.Coluna<>("Preço unit.", i -> Dinheiro.formatar(i.produto().precoCentavos())),
        new ModeloTabela.Coluna<>("Disponível", i -> i.produto().estoque()));
    private final JTable tabela = new JTable(modelo);

    PaginaNovaVenda(Contexto ctx, Runnable aoConcluir) {
        super(ctx, "Nova venda", "A venda é aberta como PENDENTE; a baixa de estoque acontece na confirmação");
        this.aoConcluir = aoConcluir;
        politica = new JComboBox<>(ctx.descontos().criadores().toArray(CriadorPoliticaDesconto[]::new));
        for (JComponent c : new JComponent[]{produto, quantidade, politica}) Tema.estilizarCampo(c);
        produto.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v, i, s, f);
                if (v instanceof Produto p)
                    setText(p.nome() + "  ·  " + Dinheiro.formatar(p.precoCentavos()) + "  ·  saldo " + p.estoque());
                return this;
            }
        });
        politica.addActionListener(e -> ajustarParametro());
        confirmarJa.setOpaque(false);
        confirmarJa.setFont(Tema.sans(13, Font.PLAIN));

        JPanel form = Tema.cartao(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        adicionar(form, c, 0, Tema.rotulo("ID da venda"), idVenda);
        JButton add = Tema.botaoSecundario("Adicionar item");
        add.addActionListener(e -> adicionarItem());
        JPanel linhaProduto = new JPanel(new BorderLayout(8, 0));
        linhaProduto.setOpaque(false);
        linhaProduto.add(produto, BorderLayout.CENTER);
        JPanel direita = new JPanel(new BorderLayout(8, 0));
        direita.setOpaque(false);
        quantidade.setPreferredSize(new Dimension(70, 36));
        direita.add(quantidade, BorderLayout.WEST);
        direita.add(add, BorderLayout.EAST);
        linhaProduto.add(direita, BorderLayout.EAST);
        adicionar(form, c, 1, Tema.rotulo("Produto / quantidade"), linhaProduto);
        adicionar(form, c, 2, Tema.rotulo("Política de desconto"), politica);
        adicionar(form, c, 3, rotuloParametro, parametro);
        c.gridy = 4; c.gridx = 1; form.add(confirmarJa, c);
        corpo.add(form, BorderLayout.NORTH);

        JPanel carrinhoPainel = new JPanel(new BorderLayout(0, 10));
        carrinhoPainel.setOpaque(false);
        JLabel t = new JLabel("Itens da venda");
        t.setFont(Tema.titulo(17));
        carrinhoPainel.add(t, BorderLayout.NORTH);
        carrinhoPainel.add(Tema.tabela(tabela), BorderLayout.CENTER);
        JButton remover = Tema.botaoPerigo("Remover item");
        JButton limpar = Tema.botaoSecundario("Limpar");
        JButton abrir = Tema.botaoPrimario("Abrir venda");
        remover.addActionListener(e -> removerItem());
        limpar.addActionListener(e -> { carrinho.clear(); modelo.definir(List.of()); });
        abrir.addActionListener(e -> abrirVenda());
        JPanel botoes = linha(remover, limpar, abrir);
        botoes.setBorder(new EmptyBorder(4, 0, 0, 0));
        carrinhoPainel.add(botoes, BorderLayout.SOUTH);
        corpo.add(carrinhoPainel, BorderLayout.CENTER);
        ajustarParametro();
    }

    private static void adicionar(JPanel p, GridBagConstraints c, int y, JComponent r, JComponent campo) {
        c.gridy = y; c.gridx = 0; c.weightx = 0; p.add(r, c);
        c.gridx = 1; c.weightx = 1; p.add(campo, c);
    }

    private void ajustarParametro() {
        CriadorPoliticaDesconto cr = (CriadorPoliticaDesconto) politica.getSelectedItem();
        if (cr == null) return;
        rotuloParametro.setText(cr.rotuloParametro().toUpperCase());
        parametro.setEnabled(cr.exigeParametro());
        if (!cr.exigeParametro()) parametro.setText("");
    }

    @Override void atualizar() {
        Object anterior = produto.getSelectedItem();
        List<Produto> produtos = ctx.facade().estoque(ctx.perfil());
        produto.setModel(new DefaultComboBoxModel<>(produtos.toArray(Produto[]::new)));
        if (anterior instanceof Produto p)
            produtos.stream().filter(x -> x.id().equals(p.id())).findFirst().ifPresent(produto::setSelectedItem);
        if (idVenda.getText().isBlank()) idVenda.setText(sugerirId());
    }

    /** Sugestão de próximo ID livre (apenas conveniência; a unicidade é garantida pelo serviço). */
    private String sugerirId() {
        Set<String> usados = new HashSet<>();
        ctx.facade().vendas(ctx.perfil()).forEach(v -> usados.add(v.id()));
        int n = usados.size() + 1;
        while (usados.contains("V" + n)) n++;
        return "V" + n;
    }

    private void adicionarItem() {
        if (!(produto.getSelectedItem() instanceof Produto p)) return;
        int qtd = (Integer) quantidade.getValue();
        carrinho.merge(p.id(), new ItemCarrinho(p, qtd),
            (a, b) -> new ItemCarrinho(p, a.quantidade() + b.quantidade()));
        modelo.definir(new ArrayList<>(carrinho.values()));
        quantidade.setValue(1);
    }

    private void removerItem() {
        int i = selecionada(tabela);
        if (i < 0) return;
        carrinho.remove(modelo.linha(i).produto().id());
        modelo.definir(new ArrayList<>(carrinho.values()));
    }

    private void abrirVenda() {
        Map<String, Integer> itens = new LinkedHashMap<>();
        carrinho.values().forEach(i -> itens.put(i.produto().id(), i.quantidade()));
        Venda[] aberta = new Venda[1];
        boolean ok = Tema.executar(this, () -> {
            CriadorPoliticaDesconto criador = (CriadorPoliticaDesconto) politica.getSelectedItem();
            PoliticaDesconto p = criador.criar(parametro.getText()); // Factory Method
            aberta[0] = ctx.facade().abrirVenda(ctx.perfil(), idVenda.getText().trim(), itens, p);
        }, null);
        if (!ok) return;
        Venda v = aberta[0];
        String resumo = "Venda " + v.id() + " aberta (" + v.estado() + ")\n\nSubtotal: "
            + Dinheiro.formatar(v.subtotalCentavos()) + "\nDesconto (" + v.nomePolitica() + "): "
            + Dinheiro.formatar(v.descontoCentavos()) + "\nTotal: " + Dinheiro.formatar(v.totalCentavos());
        if (confirmarJa.isSelected()) {
            boolean confirmou = Tema.executar(this, () -> ctx.facade().confirmar(ctx.perfil(), v.id()), null);
            resumo += confirmou ? "\n\n✔ Confirmada: estoque baixado." : "\n\nA venda continua PENDENTE.";
        }
        JOptionPane.showMessageDialog(this, resumo, "Venda registrada", JOptionPane.INFORMATION_MESSAGE);
        carrinho.clear();
        modelo.definir(List.of());
        idVenda.setText("");
        parametro.setText("");
        aoConcluir.run();
    }
}
