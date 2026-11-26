package br.ufpb.aps.grupo12.dominio;

import java.time.LocalDateTime;

public record Movimentacao(String id, String operacaoId, String produtoId, TipoMovimentacao tipo, int quantidade,
                           LocalDateTime data) {
    public Movimentacao {
        Validacao.id(id); Validacao.id(operacaoId); Validacao.id(produtoId);
        if (tipo == null) throw new ErroNegocio("Tipo de movimentação obrigatório.");
        Validacao.quantidadePositiva(quantidade);
        if (data == null) throw new ErroNegocio("Data da movimentação obrigatória.");
    }
}
