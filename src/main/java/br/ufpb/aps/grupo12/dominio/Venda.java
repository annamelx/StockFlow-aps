package br.ufpb.aps.grupo12.dominio;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Venda com estado. clienteId é opcional ("" = consumidor não identificado).
 * confirmadaEm só existe quando estado = CONFIRMADA.
 */
public record Venda(String id, EstadoVenda estado, List<ItemVenda> itens,
                    long subtotalCentavos, long descontoCentavos, long totalCentavos,
                    String nomePolitica, String clienteId, FormaPagamento formaPagamento,
                    String atendente, LocalDateTime criadaEm, LocalDateTime confirmadaEm) {
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
        clienteId = clienteId == null ? "" : clienteId;
        if (!clienteId.isEmpty()) Validacao.id(clienteId);
        if (formaPagamento == null) formaPagamento = FormaPagamento.DINHEIRO;
        atendente = atendente == null ? "" : atendente;
        if (criadaEm == null) throw new ErroNegocio("Data de abertura obrigatória.");
        if ((estado == EstadoVenda.CONFIRMADA) != (confirmadaEm != null))
            throw new ErroNegocio("Data de confirmação inconsistente com o estado.");
    }
    /** Transições permitidas: PENDENTE → CONFIRMADA (com data) ou PENDENTE → CANCELADA. */
    public Venda comEstado(EstadoVenda novo, LocalDateTime quando) {
        if (estado != EstadoVenda.PENDENTE || novo == null || novo == EstadoVenda.PENDENTE)
            throw new ErroNegocio("Transição de estado não permitida.");
        return new Venda(id, novo, itens, subtotalCentavos, descontoCentavos, totalCentavos, nomePolitica,
            clienteId, formaPagamento, atendente, criadaEm, novo == EstadoVenda.CONFIRMADA ? quando : null);
    }
    public ItemVenda item(String produtoId) {
        return itens.stream().filter(i -> i.produtoId().equals(produtoId)).findFirst()
            .orElseThrow(() -> new ErroNegocio("Produto não consta na venda original."));
    }
    public int quantidadeTotal() { return itens.stream().mapToInt(ItemVenda::quantidade).sum(); }
}
