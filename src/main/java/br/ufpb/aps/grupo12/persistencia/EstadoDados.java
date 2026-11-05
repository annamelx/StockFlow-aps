package br.ufpb.aps.grupo12.persistencia;

import br.ufpb.aps.grupo12.dominio.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Snapshot de trabalho: transações alteram somente uma cópia até o commit. */
public final class EstadoDados {
    public final Map<String, Produto> produtos = new LinkedHashMap<>();
    public final Map<String, Venda> vendas = new LinkedHashMap<>();
    public final Map<String, Devolucao> devolucoes = new LinkedHashMap<>();
    public final Map<String, Movimentacao> movimentacoes = new LinkedHashMap<>();
    public EstadoDados copiar() {
        EstadoDados novo = new EstadoDados();
        novo.produtos.putAll(produtos);
        novo.vendas.putAll(vendas);
        novo.devolucoes.putAll(devolucoes);
        novo.movimentacoes.putAll(movimentacoes);
        return novo;
    }
    public boolean mesmoConteudo(EstadoDados outro) {
        return produtos.equals(outro.produtos) && vendas.equals(outro.vendas)
            && devolucoes.equals(outro.devolucoes) && movimentacoes.equals(outro.movimentacoes);
    }
}
