package br.ufpb.aps.grupo12.interfacegrafica;

import br.ufpb.aps.grupo12.dominio.*;
import java.awt.*;
import java.util.List;
import javax.swing.*;

/**
 * UC03 — devolução vinculada a uma venda confirmada. Limite, idempotência e reembolso
 * proporcional ao valor líquido são decididos pelo ServicoLoja; a tela só informa.
 */
final class PaginaDevolucoes extends Pagina {
    private final JComboBox<String> venda = new JComboBox<>();
    private final JComboBox<String> produto = new JComboBox<>();
    private final JTextField quantidade = Tema.campo(5);
    private final JTextField idDevolucao = Tema.campo(10);
    private final JLabel info = Tema.subtitulo(" ");
    private final ModeloTabela<Devolucao> modelo = new ModeloTabela<>(
        new ModeloTabela.Coluna<>("Devolução", Devolucao::id),
        new ModeloTabela.Coluna<>("Venda", Devolucao::vendaId),
        new ModeloTabela.Coluna<>("Produto", Devolucao::produtoId),
        new ModeloTabela.Coluna<>("Qtd.", Devolucao::quantidade),
        new ModeloTabela.Coluna<>("Reembolso", d -> Dinheiro.formatar(d.reembolsoCentavos())));
    private List<Venda> vendas = List.of();
    private List<Devolucao> devolucoes = List.of();

    PaginaDevolucoes(Contexto ctx) {
        super(ctx, "Devoluções", "Somente itens de vendas confirmadas, até a quantidade vendida (perfil gerente)");
        Tema.estilizarCampo(venda);
        Tema.estilizarCampo(produto);
        venda.addActionListener(e -> carregarProdutos());
        produto.addActionListener(e -> mostrarInfo());
        JPanel form = Tema.cartao(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        String[] r = {"Venda confirmada", "Produto", "Quantidade", "ID da devolução"};
        JComponent[] campos = {venda, produto, quantidade, idDevolucao};
        for (int i = 0; i < r.length; i++) {
            c.gridx = (i % 2) * 2; c.gridy = i / 2; c.weightx = 0;
            form.add(Tema.rotulo(r[i]), c);
            c.gridx++; c.weightx = 1;
            form.add(campos[i], c);
        }
        JButton registrar = Tema.botaoPrimario("Registrar devolução");
        registrar.addActionListener(e -> registrar());
        c.gridy = 2; c.gridx = 0; c.gridwidth = 3; form.add(info, c);
        c.gridx = 3; c.gridwidth = 1; c.fill = GridBagConstraints.NONE; c.anchor = GridBagConstraints.EAST;
        form.add(registrar, c);
        corpo.add(form, BorderLayout.NORTH);
        corpo.add(Tema.tabela(new JTable(modelo)), BorderLayout.CENTER);
    }

    @Override void atualizar() {
        vendas = ctx.facade().vendas(ctx.perfil()).stream()
            .filter(v -> v.estado() == EstadoVenda.CONFIRMADA).toList();
        devolucoes = ctx.facade().devolucoes(ctx.perfil());
        Object anterior = venda.getSelectedItem();
        venda.setModel(new DefaultComboBoxModel<>(vendas.stream().map(Venda::id).toArray(String[]::new)));
        if (anterior != null) venda.setSelectedItem(anterior);
        carregarProdutos();
        modelo.definir(devolucoes.reversed());
        if (idDevolucao.getText().isBlank()) idDevolucao.setText("D" + (devolucoes.size() + 1));
    }

    private Venda vendaSelecionada() {
        Object id = venda.getSelectedItem();
        return vendas.stream().filter(v -> v.id().equals(id)).findFirst().orElse(null);
    }

    private void carregarProdutos() {
        Venda v = vendaSelecionada();
        produto.setModel(new DefaultComboBoxModel<>(v == null ? new String[0]
            : v.itens().stream().map(ItemVenda::produtoId).toArray(String[]::new)));
        mostrarInfo();
    }

    /** Apenas informativo; quem impede excesso é o serviço (RN05). */
    private void mostrarInfo() {
        Venda v = vendaSelecionada();
        Object p = produto.getSelectedItem();
        if (v == null || p == null) { info.setText("Nenhuma venda confirmada selecionada."); return; }
        ItemVenda item = v.item((String) p);
        int devolvido = devolucoes.stream().filter(d -> d.vendaId().equals(v.id()) && d.produtoId().equals(p))
            .mapToInt(Devolucao::quantidade).sum();
        info.setText("Vendidos: " + item.quantidade() + "  ·  já devolvidos: " + devolvido
            + "  ·  valor líquido do item: " + Dinheiro.formatar(item.liquidoCentavos()));
    }

    private void registrar() {
        Tema.executar(this, () -> {
            Devolucao d = ctx.facade().devolver(ctx.perfil(), idDevolucao.getText().trim(),
                (String) venda.getSelectedItem(), (String) produto.getSelectedItem(),
                Tema.inteiro(quantidade.getText(), "Quantidade"));
            JOptionPane.showMessageDialog(this, "Devolução " + d.id() + " registrada.\nReembolso: "
                + Dinheiro.formatar(d.reembolsoCentavos()) + "\n(Repetir o mesmo ID não gera novo efeito.)");
        }, null);
        idDevolucao.setText("");
        atualizar();
    }
}
