package br.ufpb.aps.grupo12.dominio;

import java.time.LocalDate;

/**
 * Conta a pagar ou a receber. Estados: ABERTA → QUITADA ou ABERTA → CANCELADA.
 * Quitar uma conta já quitada não tem novo efeito (idempotente no serviço).
 */
public record ContaFinanceira(String id, TipoConta tipo, String descricao, long valorCentavos,
                              LocalDate vencimento, SituacaoConta situacao, LocalDate quitadaEm) {
    public ContaFinanceira {
        Validacao.id(id);
        if (tipo == null || vencimento == null || situacao == null) throw new ErroNegocio("Conta incompleta.");
        descricao = Texto.obrigatorio(descricao, "Descrição", 120);
        if (valorCentavos <= 0) throw new ErroNegocio("Valor da conta deve ser positivo.");
        if ((situacao == SituacaoConta.QUITADA) != (quitadaEm != null))
            throw new ErroNegocio("Data de quitação inconsistente com a situação.");
    }
    public ContaFinanceira quitar(LocalDate quando) {
        if (situacao != SituacaoConta.ABERTA) throw new ErroNegocio("Só contas abertas podem ser quitadas.");
        return new ContaFinanceira(id, tipo, descricao, valorCentavos, vencimento, SituacaoConta.QUITADA, quando);
    }
    public ContaFinanceira cancelar() {
        if (situacao != SituacaoConta.ABERTA) throw new ErroNegocio("Só contas abertas podem ser canceladas.");
        return new ContaFinanceira(id, tipo, descricao, valorCentavos, vencimento, SituacaoConta.CANCELADA, null);
    }
    public boolean vencida(LocalDate hoje) { return situacao == SituacaoConta.ABERTA && vencimento.isBefore(hoje); }
}
