package br.ufpb.aps.grupo12.dominio;
public record Movimentacao(String id, String operacaoId, String produtoId, TipoMovimentacao tipo, int quantidade) {
    public Movimentacao {
        Validacao.id(id); Validacao.id(operacaoId); Validacao.id(produtoId);
        if (tipo == null) throw new ErroNegocio("Tipo de movimentação obrigatório.");
        Validacao.quantidadePositiva(quantidade);
    }
}
