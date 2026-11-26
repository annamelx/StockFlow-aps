package br.ufpb.aps.grupo12.interfacegrafica;

import br.ufpb.aps.grupo12.dominio.*;
import java.awt.*;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;

/** UC01 — produtos, compras (reposição com fornecedor) e histórico do item. Saldo só muda por movimento. */
final class PaginaProdutos extends PaginaListaDetalhe<Produto> {
    private final JTextField id = Tema.campo(12), nome = Tema.campo(24), preco = Tema.campo(10), custo = Tema.campo(10),
        minimo = Tema.campo(5), inicial = Tema.campo(5);
    private final JComboBox<Categoria> categoria = new JComboBox<>();
    private final JLabel info = new JLabel();
    private final ModeloTabela<Movimentacao> movs = new ModeloTabela<>(
        new ModeloTabela.Coluna<>("Data", m -> m.data().format(Componentes.DATA_HORA)),
        new ModeloTabela.Coluna<>("Movimento", m -> switch (m.tipo()) {
            case ENTRADA_REPOSICAO -> "↑ Compra"; case SAIDA_VENDA -> "↓ Venda"; case ENTRADA_DEVOLUCAO -> "↑ Devolução"; }),
        new ModeloTabela.Coluna<>("Qtd.", Movimentacao::quantidade),
        new ModeloTabela.Coluna<>("Operação", Movimentacao::operacaoId));

    PaginaProdutos(Contexto ctx) {
        super(ctx, "Produtos", "produto", "Novo produto", Icone.Tipo.CAIXA);
        Tema.estilizarCampo(categoria);
        categoria.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v, i, s, f);
                if (v instanceof Categoria c) setText(c.nome());
                return this;
            }
        });
        JPanel topo = new JPanel(new BorderLayout(12, 0));
        topo.setOpaque(false);
        info.setFont(Tema.sans(13, Font.PLAIN));
        topo.add(info, BorderLayout.CENTER);
        JButton compra = Componentes.comIcone(Tema.botaoSecundario("Registrar compra"), Icone.Tipo.CAMINHAO);
        compra.addActionListener(e -> registrarCompra());
        topo.add(compra, BorderLayout.EAST);
        topo.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        extras.add(topo, BorderLayout.NORTH);
        JScrollPane t = Tema.tabela(new JTable(movs));
        t.setPreferredSize(new Dimension(300, 160));
        extras.add(t, BorderLayout.CENTER);
        montarFormulario();
    }

    @Override List<Produto> carregar() {
        DefaultComboBoxModel<Categoria> m = new DefaultComboBoxModel<>();
        m.addElement(new Categoria(Categoria.GERAL, "Geral"));
        ctx.facade().categorias(ctx.perfil()).forEach(m::addElement);
        Object sel = categoria.getSelectedItem();
        categoria.setModel(m);
        if (sel != null) categoria.setSelectedItem(sel);
        return ctx.facade().estoque(ctx.perfil());
    }
    @Override String nome(Produto p) { return p.nome(); }
    @Override String detalhe(Produto p) { return p.id(); }
    @Override JComponent etiqueta(Produto p) {
        if (p.estoque() == 0) return Componentes.pilula("Esgotado", new Color(0xFEE2E2), Tema.ALERTA);
        if (p.emAlerta()) return Componentes.pilula(p.estoque() + " un", new Color(0xFEF3C7), new Color(0xB45309));
        return Componentes.pilula(p.estoque() + " un", Tema.AZUL_CLARO, Tema.AZUL_ESCURO);
    }
    @Override String[] rotulos() { return new String[]{"Código", "Nome", "Categoria", "Preço de venda (R$)", "Custo (R$)", "Estoque mínimo", "Estoque inicial"}; }
    @Override JComponent[] campos() { return new JComponent[]{id, nome, categoria, preco, custo, minimo, inicial}; }

    @Override void preencher(Produto p) {
        boolean novo = p == null;
        id.setEditable(novo);
        inicial.setEnabled(novo);
        id.setText(novo ? "" : p.id());
        nome.setText(novo ? "" : p.nome());
        preco.setText(novo ? "" : Dinheiro.formatar(p.precoCentavos()).replace("R$ ", ""));
        custo.setText(novo ? "0,00" : Dinheiro.formatar(p.custoCentavos()).replace("R$ ", ""));
        minimo.setText(novo ? "2" : String.valueOf(p.estoqueMinimo()));
        inicial.setText(novo ? "0" : String.valueOf(p.estoque()));
        inicial.setToolTipText("Depois do cadastro o saldo só muda por compra, venda ou devolução.");
        for (int i = 0; i < categoria.getItemCount(); i++)
            if (categoria.getItemAt(i).id().equals(novo ? Categoria.GERAL : p.categoriaId())) categoria.setSelectedIndex(i);
        if (novo) { info.setText(" "); movs.definir(List.of()); return; }
        long margem = p.precoCentavos() == 0 ? 0 : 100 * p.margemCentavos() / p.precoCentavos();
        info.setText("<html><b>Saldo: " + p.estoque() + " un</b> (mínimo " + p.estoqueMinimo() + ")  ·  margem bruta "
            + Dinheiro.formatar(p.margemCentavos()) + " (" + margem + "%)"
            + (p.emAlerta() ? "  ·  <font color='#DC2626'><b>estoque baixo</b></font>" : "") + "</html>");
        movs.definir(ctx.facade().historico(ctx.perfil()).stream().filter(m -> m.produtoId().equals(p.id()))
            .sorted(Comparator.comparing(Movimentacao::data).reversed()).toList());
    }

    @Override void salvar(boolean novo) {
        Categoria cat = (Categoria) categoria.getSelectedItem();
        long pv = Dinheiro.lerReais(preco.getText()), pc = Dinheiro.lerReais(custo.getText());
        int min = Tema.inteiro(minimo.getText(), "Estoque mínimo");
        if (novo) {
            Produto p = new Produto(id.getText().trim().toUpperCase(), nome.getText().trim(), pv,
                Tema.inteiro(inicial.getText(), "Estoque inicial"), min, cat.id(), pc);
            ctx.facade().cadastrarProduto(ctx.perfil(), p);
            selecionado = p;
        } else {
            selecionado = ctx.facade().editarProduto(ctx.perfil(), selecionado.id(), nome.getText().trim(), pv, pc, min, cat.id());
        }
    }
    @Override boolean permiteExcluir() { return false; }

    private void registrarCompra() {
        if (selecionado == null) { JOptionPane.showMessageDialog(this, "Salve ou selecione um produto primeiro."); return; }
        Produto p = selecionado;
        JComboBox<String> forn = new JComboBox<>(ctx.facade().fornecedores(ctx.perfil()).stream()
            .map(f -> f.id() + " — " + f.nome()).toArray(String[]::new));
        JTextField qtd = Tema.campo(5), cu = Tema.campo(8), lanc = Tema.campo(10);
        cu.setText(Dinheiro.formatar(p.custoCentavos()).replace("R$ ", ""));
        lanc.setText(ctx.facade().proximoId(ctx.perfil(), "REP"));
        if (!Formulario.mostrar(this, "Registrar compra — " + p.nome(),
            new String[]{"Fornecedor", "Quantidade", "Custo unitário (R$)", "ID do lançamento"},
            new JComponent[]{forn, qtd, cu, lanc})) return;
        executar(() -> {
            String f = (String) forn.getSelectedItem();
            if (f == null) throw new ErroNegocio("Cadastre um fornecedor antes de registrar compras.");
            selecionado = ctx.facade().comprar(ctx.perfil(), p.id(), Tema.inteiro(qtd.getText(), "Quantidade"),
                lanc.getText().trim(), f.substring(0, f.indexOf(" — ")), Dinheiro.lerReais(cu.getText()));
        }, "Compra registrada: estoque, movimentação e custo atualizados juntos.");
    }
}
