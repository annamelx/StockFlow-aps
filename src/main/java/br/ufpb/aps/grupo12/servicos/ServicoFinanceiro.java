package br.ufpb.aps.grupo12.servicos;

import br.ufpb.aps.grupo12.dominio.*;
import br.ufpb.aps.grupo12.persistencia.Repositorio;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Contas a pagar/receber e meta mensal de faturamento. */
public final class ServicoFinanceiro {
    public static final String CHAVE_META = "metaMensalCentavos";
    public static final long META_PADRAO = 1_500_000; // R$ 15.000,00
    private final Repositorio repositorio;
    private final Clock relogio;

    public ServicoFinanceiro(Repositorio repositorio, Clock relogio) {
        this.repositorio = Objects.requireNonNull(repositorio);
        this.relogio = Objects.requireNonNull(relogio);
    }

    public List<ContaFinanceira> contas(Perfil perfil) {
        Autorizacao.exigir(perfil, false);
        return repositorio.consultar(e -> List.copyOf(e.contas.values()));
    }
    public ContaFinanceira lancarConta(Perfil perfil, String id, TipoConta tipo, String descricao,
                                       long valorCentavos, LocalDate vencimento) {
        Autorizacao.exigir(perfil, true);
        ContaFinanceira c = new ContaFinanceira(id, tipo, descricao, valorCentavos, vencimento, SituacaoConta.ABERTA, null);
        return repositorio.transacionar(e -> {
            if (e.contas.containsKey(id)) throw new ErroNegocio("Conta duplicada: " + id);
            e.contas.put(id, c);
            return c;
        });
    }
    /** Quitar duas vezes não gera novo efeito: devolve a conta como já está. */
    public ContaFinanceira quitar(Perfil perfil, String id) {
        Autorizacao.exigir(perfil, true);
        LocalDate hoje = LocalDate.now(relogio);
        return repositorio.transacionar(e -> {
            ContaFinanceira c = e.contas.get(id);
            if (c == null) throw new ErroNegocio("Conta inexistente: " + id);
            if (c.situacao() == SituacaoConta.QUITADA) return c;
            ContaFinanceira q = c.quitar(hoje);
            e.contas.put(id, q);
            return q;
        });
    }
    public ContaFinanceira cancelar(Perfil perfil, String id) {
        Autorizacao.exigir(perfil, true);
        return repositorio.transacionar(e -> {
            ContaFinanceira c = e.contas.get(id);
            if (c == null) throw new ErroNegocio("Conta inexistente: " + id);
            ContaFinanceira x = c.cancelar();
            e.contas.put(id, x);
            return x;
        });
    }

    public record Totais(long aPagar, long aReceber, long vencido) {}
    /** Totais das contas em aberto (consulta permitida aos dois perfis). */
    public Totais totais(Perfil perfil) {
        Autorizacao.exigir(perfil, false);
        LocalDate hoje = LocalDate.now(relogio);
        return repositorio.consultar(e -> {
            long pagar = 0, receber = 0, vencido = 0;
            for (ContaFinanceira c : e.contas.values()) {
                if (c.situacao() != SituacaoConta.ABERTA) continue;
                if (c.tipo() == TipoConta.PAGAR) pagar += c.valorCentavos(); else receber += c.valorCentavos();
                if (c.vencida(hoje)) vencido += c.valorCentavos();
            }
            return new Totais(pagar, receber, vencido);
        });
    }
    public long metaMensal(Perfil perfil) {
        Autorizacao.exigir(perfil, true);
        return repositorio.consultar(e -> Long.parseLong(e.config.getOrDefault(CHAVE_META, String.valueOf(META_PADRAO))));
    }
    public long definirMeta(Perfil perfil, long valorCentavos) {
        Autorizacao.exigir(perfil, true);
        if (valorCentavos <= 0) throw new ErroNegocio("A meta deve ser maior que zero.");
        return repositorio.transacionar(e -> { e.config.put(CHAVE_META, String.valueOf(valorCentavos)); return valorCentavos; });
    }
}
