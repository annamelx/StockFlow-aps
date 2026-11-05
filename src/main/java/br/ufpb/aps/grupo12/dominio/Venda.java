package br.ufpb.aps.grupo12.dominio;

import java.util.List;
public record Venda(String id, EstadoVenda estado, List<ItemVenda> itens,
                    long subtotalCentavos, long descontoCentavos, long totalCentavos,
                    String nomePolitica) {
    public Venda {
        Validacao.id(id);
        if (estado == null || itens == null || itens.isEmpty() || nomePolitica == null)
            throw new ErroNegocio("Venda incompleta.");
        itens = List.copyOf(itens);
        if (subtotalCentavos < 0 || descontoCentavos < 0 || totalCentavos < 0
            || subtotalCentavos - descontoCentavos != totalCentavos)
            throw new ErroNegocio("Totais da venda inconsistentes.");
        long subtotalCalculado = itens.stream().mapToLong(ItemVenda::brutoCentavos).reduce(0, Math::addExact);
        long descontoCalculado = itens.stream().mapToLong(ItemVenda::descontoCentavos).reduce(0, Math::addExact);
        if (subtotalCalculado != subtotalCentavos || descontoCalculado != descontoCentavos)
            throw new ErroNegocio("Valores dos itens inconsistentes com total da venda.");
    }
    public Venda comEstado(EstadoVenda novo) {
        if (estado != EstadoVenda.PENDENTE || novo == EstadoVenda.PENDENTE)
            throw new ErroNegocio("Transição de estado não permitida.");
        return new Venda(id, novo, itens, subtotalCentavos, descontoCentavos, totalCentavos, nomePolitica);
    }
    public ItemVenda item(String produtoId) {
        return itens.stream().filter(i -> i.produtoId().equals(produtoId)).findFirst()
            .orElseThrow(() -> new ErroNegocio("Produto não consta na venda original."));
    }
}
