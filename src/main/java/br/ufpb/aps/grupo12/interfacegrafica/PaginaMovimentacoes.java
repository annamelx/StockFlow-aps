package br.ufpb.aps.grupo12.interfacegrafica;

import br.ufpb.aps.grupo12.dominio.Movimentacao;
import java.awt.BorderLayout;
import java.util.Comparator;
import javax.swing.JTable;

/** Histórico completo de entradas e saídas, gravado junto com cada operação. */
final class PaginaMovimentacoes extends Pagina {
    private final ModeloTabela<Movimentacao> modelo = new ModeloTabela<>(
        new ModeloTabela.Coluna<>("Data", m -> m.data().format(Componentes.DATA_HORA)),
        new ModeloTabela.Coluna<>("Tipo", m -> switch (m.tipo()) {
            case ENTRADA_REPOSICAO -> "↑ Compra/reposição"; case SAIDA_VENDA -> "↓ Venda"; case ENTRADA_DEVOLUCAO -> "↑ Devolução"; }),
        new ModeloTabela.Coluna<>("Produto", Movimentacao::produtoId),
        new ModeloTabela.Coluna<>("Quantidade", Movimentacao::quantidade),
        new ModeloTabela.Coluna<>("Operação", Movimentacao::operacaoId));
    PaginaMovimentacoes(Contexto ctx) {
        super(ctx, "Movimentações", "entradas e saídas de estoque");
        add(Tema.tabela(new JTable(modelo)), BorderLayout.CENTER);
    }
    @Override void atualizar() {
        modelo.definir(ctx.facade().historico(ctx.perfil()).stream()
            .sorted(Comparator.comparing(Movimentacao::data).reversed()).toList());
    }
}
