package br.ufpb.aps.grupo12.dominio;
public record Devolucao(String id, String vendaId, String produtoId, int quantidade, long reembolsoCentavos) {
    public Devolucao {
        Validacao.id(id); Validacao.id(vendaId); Validacao.id(produtoId);
        Validacao.quantidadePositiva(quantidade);
        if (reembolsoCentavos < 0) throw new ErroNegocio("Reembolso negativo.");
    }
}
