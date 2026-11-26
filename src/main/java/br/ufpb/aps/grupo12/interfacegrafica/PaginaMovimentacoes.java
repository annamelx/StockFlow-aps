package br.ufpb.aps.grupo12.interfacegrafica;

import br.ufpb.aps.grupo12.dominio.Movimentacao;
import java.awt.BorderLayout;
import javax.swing.JTable;

/** UC04 — histórico de entradas e saídas gravado junto com cada operação. */
final class PaginaMovimentacoes extends Pagina {
    private final ModeloTabela<Movimentacao> modelo = new ModeloTabela<>(
        new ModeloTabela.Coluna<>("Tipo", m -> switch (m.tipo()) {
            case ENTRADA_REPOSICAO -> "↑ Reposição";
            case SAIDA_VENDA -> "↓ Venda";
            case ENTRADA_DEVOLUCAO -> "↑ Devolução";
        }),
        new ModeloTabela.Coluna<>("Produto", Movimentacao::produtoId),
        new ModeloTabela.Coluna<>("Quantidade", Movimentacao::quantidade),
        new ModeloTabela.Coluna<>("Operação", Movimentacao::operacaoId),
        new ModeloTabela.Coluna<>("ID do movimento", Movimentacao::id));

    PaginaMovimentacoes(Contexto ctx) {
        super(ctx, "Movimentações", "Cada venda confirmada, devolução e reposição gera exatamente um movimento por produto");
        corpo.add(Tema.tabela(new JTable(modelo)), BorderLayout.CENTER);
    }

    @Override void atualizar() { modelo.definir(ctx.facade().historico(ctx.perfil()).reversed()); }
}
