package br.ufpb.aps.grupo12.interfacegrafica;

import br.ufpb.aps.grupo12.dominio.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

/** Histórico de vendas (UC02/UC03): filtrar, confirmar, cancelar pendente e devolver itens. */
final class PaginaHistorico extends Pagina {
    private final JTextField de = Tema.campo(8), ate = Tema.campo(8), busca = Tema.campo(14);
    private final JComboBox<String> situacao = new JComboBox<>(new String[]{"Todas", "PENDENTE", "CONFIRMADA", "CANCELADA"});
    private Map<String, String> nomesClientes = Map.of();
    private final ModeloTabela<Venda> vendas = new ModeloTabela<>(
        new ModeloTabela.Coluna<>("Nº", Venda::id),
        new ModeloTabela.Coluna<>("Data", v -> v.criadaEm().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"))),
        new ModeloTabela.Coluna<>("Cliente", v -> v.clienteId().isEmpty() ? "Consumidor final" : nomesClientes.getOrDefault(v.clienteId(), v.clienteId())),
        new ModeloTabela.Coluna<>("Atendente", v -> v.atendente().isEmpty() ? "—" : v.atendente()),
        new ModeloTabela.Coluna<>("Pagamento", Venda::formaPagamento),
        new ModeloTabela.Coluna<>("Total", v -> Dinheiro.formatar(v.totalCentavos())),
        new ModeloTabela.Coluna<>("Situação", Venda::estado));
    private final JTable tabela = new JTable(vendas);
    private final ModeloTabela<ItemVenda> itens = new ModeloTabela<>(
        new ModeloTabela.Coluna<>("Produto", ItemVenda::produtoId),
        new ModeloTabela.Coluna<>("Qtd.", ItemVenda::quantidade),
        new ModeloTabela.Coluna<>("Desconto", i -> Dinheiro.formatar(i.descontoCentavos())),
        new ModeloTabela.Coluna<>("Líquido", i -> Dinheiro.formatar(i.liquidoCentavos())));
    private final ModeloTabela<Devolucao> devolucoes = new ModeloTabela<>(
        new ModeloTabela.Coluna<>("Devolução", Devolucao::id),
        new ModeloTabela.Coluna<>("Data", d -> d.data().format(Componentes.DATA_HORA)),
        new ModeloTabela.Coluna<>("Venda", Devolucao::vendaId),
        new ModeloTabela.Coluna<>("Produto", Devolucao::produtoId),
        new ModeloTabela.Coluna<>("Qtd.", Devolucao::quantidade),
        new ModeloTabela.Coluna<>("Reembolso", d -> Dinheiro.formatar(d.reembolsoCentavos())));
    private final JLabel detalheTitulo = Componentes.tituloSecao("Selecione uma venda");
    private final JLabel detalheInfo = Tema.subtitulo(" ");
    private final CardLayout abas = new CardLayout();
    private final JPanel conteudoAbas = new JPanel(abas);
    private List<Venda> todas = List.of();

    PaginaHistorico(Contexto ctx) {
        super(ctx, "Histórico de Vendas", "vendas e devoluções");
        Tema.estilizarCampo(situacao);
        LocalDate hoje = LocalDate.now();
        de.setText(hoje.minusDays(30).format(Componentes.DATA));
        ate.setText(hoje.format(Componentes.DATA));

        Cartao filtros = new Cartao(new FlowLayout(FlowLayout.LEFT, 10, 0), 12);
        filtros.add(Tema.rotulo("Período")); filtros.add(de); filtros.add(Tema.subtitulo("até")); filtros.add(ate);
        filtros.add(Tema.rotulo("Situação")); filtros.add(situacao);
        filtros.add(Tema.rotulo("Busca")); filtros.add(busca);
        JButton filtrar = Componentes.comIcone(Tema.botaoSecundario("Filtrar"), Icone.Tipo.BUSCA);
        filtrar.addActionListener(e -> Tema.executar(this, this::filtrar, null));
        filtros.add(filtrar);
        JToggleButton abaV = aba("Vendas"), abaD = aba("Devoluções");
        ButtonGroup g = new ButtonGroup(); g.add(abaV); g.add(abaD); abaV.setSelected(true);
        abaV.addActionListener(e -> abas.show(conteudoAbas, "v"));
        abaD.addActionListener(e -> abas.show(conteudoAbas, "d"));
        JPanel topo = new JPanel(new BorderLayout(0, 12));
        topo.setOpaque(false);
        topo.add(filtros, BorderLayout.CENTER);
        JPanel abasLinha = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        abasLinha.setOpaque(false);
        abasLinha.add(abaV); abasLinha.add(abaD);
        topo.add(abasLinha, BorderLayout.SOUTH);
        add(topo, BorderLayout.NORTH);

        JPanel painelVendas = new JPanel(new BorderLayout(16, 0));
        painelVendas.setOpaque(false);
        painelVendas.add(Tema.tabela(tabela), BorderLayout.CENTER);
        Cartao detalhe = new Cartao(new BorderLayout(0, 10));
        detalhe.setPreferredSize(new Dimension(340, 0));
        JPanel dt = new JPanel(new GridLayout(2, 1));
        dt.setOpaque(false);
        dt.add(detalheTitulo); dt.add(detalheInfo);
        detalhe.add(dt, BorderLayout.NORTH);
        detalhe.add(Tema.tabela(new JTable(itens)), BorderLayout.CENTER);
        JButton confirmar = Tema.botaoPrimario("Confirmar"), cancelar = Tema.botaoPerigo("Cancelar"),
            devolver = Tema.botaoSecundario("Devolver");
        confirmar.addActionListener(e -> agir(v -> ctx.facade().confirmar(ctx.perfil(), v.id()), "Venda confirmada; estoque baixado."));
        cancelar.addActionListener(e -> agir(v -> ctx.facade().cancelar(ctx.perfil(), v.id()), "Venda cancelada."));
        devolver.addActionListener(e -> devolver());
        for (JButton b : new JButton[]{confirmar, cancelar, devolver})
            b.setBorder(BorderFactory.createCompoundBorder(b.getBorder() instanceof CompoundBorder cb ? cb.getOutsideBorder() : null,
                new EmptyBorder(9, 6, 9, 6)));
        JPanel bt = new JPanel(new GridLayout(1, 3, 6, 0));
        bt.setOpaque(false);
        bt.add(cancelar); bt.add(devolver); bt.add(confirmar);
        detalhe.add(bt, BorderLayout.SOUTH);
        painelVendas.add(detalhe, BorderLayout.EAST);
        tabela.getSelectionModel().addListSelectionListener(e -> mostrarDetalhe());
        Tema.larguras(tabela, 50, 95, 140, 115, 110, 95, 110);
        conteudoAbas.setOpaque(false);
        conteudoAbas.add(painelVendas, "v");
        conteudoAbas.add(Tema.tabela(new JTable(devolucoes)), "d");
        add(conteudoAbas, BorderLayout.CENTER);
    }

    private static JToggleButton aba(String t) { return Componentes.alternavel(t, Tema.AZUL, Color.WHITE); }

    @Override void atualizar() {
        todas = ctx.facade().vendas(ctx.perfil());
        Map<String, String> nomes = new HashMap<>();
        ctx.facade().clientes(ctx.perfil()).forEach(c -> nomes.put(c.id(), c.nome()));
        nomesClientes = nomes;
        filtrar();
    }

    /** Filtro apenas de exibição (as regras de negócio não dependem dele). */
    private void filtrar() {
        LocalDate ini = Componentes.lerData(de.getText(), "Data inicial"), fim = Componentes.lerData(ate.getText(), "Data final");
        String sit = (String) situacao.getSelectedItem(), q = busca.getText().trim().toLowerCase();
        List<Venda> lista = todas.stream()
            .filter(v -> !v.criadaEm().toLocalDate().isBefore(ini) && !v.criadaEm().toLocalDate().isAfter(fim))
            .filter(v -> "Todas".equals(sit) || v.estado().name().equals(sit))
            .filter(v -> q.isEmpty() || v.id().toLowerCase().contains(q)
                || nomesClientes.getOrDefault(v.clienteId(), "").toLowerCase().contains(q))
            .sorted(Comparator.comparing(Venda::criadaEm).reversed()).toList();
        vendas.definir(lista);
        devolucoes.definir(ctx.facade().devolucoes(ctx.perfil()).stream()
            .sorted(Comparator.comparing(Devolucao::data).reversed()).toList());
        itens.definir(List.of());
        detalheTitulo.setText(lista.size() + " venda(s) no filtro");
        detalheInfo.setText("Total: " + Dinheiro.formatar(lista.stream().filter(v -> v.estado() == EstadoVenda.CONFIRMADA)
            .mapToLong(Venda::totalCentavos).sum()) + " confirmados");
    }

    private Venda selecionadaVenda() {
        int i = selecionada(tabela);
        return i < 0 ? null : vendas.linha(i);
    }

    private void mostrarDetalhe() {
        Venda v = selecionadaVenda();
        if (v == null) return;
        detalheTitulo.setText("Venda " + v.id() + " · " + v.estado());
        detalheInfo.setText("Subtotal " + Dinheiro.formatar(v.subtotalCentavos()) + " · desc. "
            + Dinheiro.formatar(v.descontoCentavos()) + " · " + v.nomePolitica());
        itens.definir(v.itens());
    }

    private void agir(java.util.function.Function<Venda, Venda> acao, String sucesso) {
        Venda v = selecionadaVenda();
        if (v == null) { JOptionPane.showMessageDialog(this, "Selecione uma venda na tabela."); return; }
        executar(() -> acao.apply(v), sucesso);
    }

    private void devolver() {
        Venda v = selecionadaVenda();
        if (v == null) { JOptionPane.showMessageDialog(this, "Selecione uma venda confirmada."); return; }
        JComboBox<String> produto = new JComboBox<>(v.itens().stream().map(ItemVenda::produtoId).toArray(String[]::new));
        JTextField qtd = Tema.campo(5), id = Tema.campo(8);
        id.setText(ctx.facade().proximoId(ctx.perfil(), "D"));
        qtd.setText("1");
        if (!Formulario.mostrar(this, "Devolver item da venda " + v.id(),
            new String[]{"Produto", "Quantidade", "ID da devolução"}, new JComponent[]{produto, qtd, id})) return;
        executar(() -> {
            Devolucao d = ctx.facade().devolver(ctx.perfil(), id.getText().trim(), v.id(),
                (String) produto.getSelectedItem(), Tema.inteiro(qtd.getText(), "Quantidade"));
            JOptionPane.showMessageDialog(this, "Devolução " + d.id() + " registrada.\nReembolso: "
                + Dinheiro.formatar(d.reembolsoCentavos()) + "\nO produto voltou ao estoque.");
        }, null);
    }
}
