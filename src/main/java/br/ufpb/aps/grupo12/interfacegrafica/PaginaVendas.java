package br.ufpb.aps.grupo12.interfacegrafica;

import br.ufpb.aps.grupo12.dominio.*;
import java.awt.*;
import java.util.List;
import javax.swing.*;

/** UC02 — confirmar ou cancelar vendas; ver itens com desconto rateado. */
final class PaginaVendas extends Pagina {
    private final ModeloTabela<Venda> modelo = new ModeloTabela<>(
        new ModeloTabela.Coluna<>("Venda", Venda::id),
        new ModeloTabela.Coluna<>("Situação", Venda::estado),
        new ModeloTabela.Coluna<>("Itens", v -> v.itens().size()),
        new ModeloTabela.Coluna<>("Subtotal", v -> Dinheiro.formatar(v.subtotalCentavos())),
        new ModeloTabela.Coluna<>("Desconto", v -> Dinheiro.formatar(v.descontoCentavos())),
        new ModeloTabela.Coluna<>("Total", v -> Dinheiro.formatar(v.totalCentavos())),
        new ModeloTabela.Coluna<>("Política", Venda::nomePolitica));
    private final JTable tabela = new JTable(modelo);
    private final ModeloTabela<ItemVenda> itens = new ModeloTabela<>(
        new ModeloTabela.Coluna<>("Produto", ItemVenda::produtoId),
        new ModeloTabela.Coluna<>("Qtd.", ItemVenda::quantidade),
        new ModeloTabela.Coluna<>("Preço unit.", i -> Dinheiro.formatar(i.precoUnitarioCentavos())),
        new ModeloTabela.Coluna<>("Desconto rateado", i -> Dinheiro.formatar(i.descontoCentavos())),
        new ModeloTabela.Coluna<>("Líquido", i -> Dinheiro.formatar(i.liquidoCentavos())));

    PaginaVendas(Contexto ctx) {
        super(ctx, "Vendas", "Pendente → Confirmada (baixa estoque) ou Cancelada. Confirmar de novo não repete a baixa.");
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, Tema.tabela(tabela), Tema.tabela(new JTable(itens)));
        split.setResizeWeight(0.62);
        split.setBorder(null);
        split.setOpaque(false);
        corpo.add(split, BorderLayout.CENTER);
        tabela.getSelectionModel().addListSelectionListener(e -> {
            int i = selecionada(tabela);
            itens.definir(i < 0 ? List.of() : modelo.linha(i).itens());
        });
    }

    @Override protected JComponent acoesCabecalho() {
        JButton cancelar = Tema.botaoPerigo("Cancelar pendente");
        JButton confirmar = Tema.botaoPrimario("Confirmar venda");
        cancelar.addActionListener(e -> agir(false));
        confirmar.addActionListener(e -> agir(true));
        return linha(cancelar, confirmar);
    }

    @Override void atualizar() { modelo.definir(ctx.facade().vendas(ctx.perfil()).reversed()); itens.definir(List.of()); }

    private void agir(boolean confirmar) {
        int i = selecionada(tabela);
        if (i < 0) { JOptionPane.showMessageDialog(this, "Selecione uma venda na tabela."); return; }
        String id = modelo.linha(i).id();
        Tema.executar(this, () -> {
            Venda v = confirmar ? ctx.facade().confirmar(ctx.perfil(), id) : ctx.facade().cancelar(ctx.perfil(), id);
            JOptionPane.showMessageDialog(this, "Venda " + v.id() + " agora está " + v.estado() + ".");
        }, null);
        atualizar();
    }
}
