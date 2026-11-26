package br.ufpb.aps.grupo12.dominio;

import java.time.LocalDateTime;

public record Devolucao(String id, String vendaId, String produtoId, int quantidade, long reembolsoCentavos,
                        LocalDateTime data) {
    public Devolucao {
        Validacao.id(id); Validacao.id(vendaId); Validacao.id(produtoId);
        Validacao.quantidadePositiva(quantidade);
        if (reembolsoCentavos < 0) throw new ErroNegocio("Reembolso negativo.");
        if (data == null) throw new ErroNegocio("Data da devolução obrigatória.");
    }
}
