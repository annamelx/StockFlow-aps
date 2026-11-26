package br.ufpb.aps.grupo12.dominio;

import java.time.LocalDateTime;

/** Entrada de mercadoria comprada de um fornecedor; gravada junto com a reposição do estoque. */
public record Compra(String id, String fornecedorId, String produtoId, int quantidade,
                     long custoUnitarioCentavos, LocalDateTime data) {
    public Compra {
        Validacao.id(id); Validacao.id(fornecedorId); Validacao.id(produtoId);
        Validacao.quantidadePositiva(quantidade);
        if (custoUnitarioCentavos < 0) throw new ErroNegocio("Custo unitário não pode ser negativo.");
        if (data == null) throw new ErroNegocio("Data da compra obrigatória.");
    }
    public long totalCentavos() { return Math.multiplyExact(custoUnitarioCentavos, quantidade); }
}
