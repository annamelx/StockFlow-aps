package br.ufpb.aps.grupo12.servicos;

import br.ufpb.aps.grupo12.dominio.*;
import br.ufpb.aps.grupo12.persistencia.EstadoDados;
import br.ufpb.aps.grupo12.persistencia.Repositorio;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Indicadores do Dashboard e dos Relatórios. Somente leitura e somente gerente.
 * Todas as contas (faturamento, lucro, ticket médio) ficam aqui — nunca nas telas.
 *
 * Definições:
 *  - Faturamento do período = total das vendas CONFIRMADAS no período − reembolsos de devoluções no período.
 *  - Lucro = Σ(valor líquido do item − custo do item) das vendas confirmadas
 *            − Σ(reembolso − custo das unidades devolvidas) das devoluções do período.
 *  - Ticket médio = total das vendas confirmadas ÷ número de vendas confirmadas (arredondado ao centavo).
 */
public final class ServicoRelatorios {
    private final Repositorio repositorio;
    private final Clock relogio;

    public ServicoRelatorios(Repositorio repositorio, Clock relogio) {
        this.repositorio = Objects.requireNonNull(repositorio);
        this.relogio = Objects.requireNonNull(relogio);
    }

    public record Periodo(LocalDate inicio, LocalDate fim) {
        public Periodo {
            if (inicio == null || fim == null) throw new ErroNegocio("Informe início e fim do período.");
            if (fim.isBefore(inicio)) throw new ErroNegocio("A data final não pode ser anterior à inicial.");
        }
        boolean contem(LocalDateTime t) { return t != null && !t.toLocalDate().isBefore(inicio) && !t.toLocalDate().isAfter(fim); }
        public static Periodo dia(LocalDate d) { return new Periodo(d, d); }
        public static Periodo mes(LocalDate d) { return new Periodo(d.withDayOfMonth(1), d.withDayOfMonth(d.lengthOfMonth())); }
    }
    public record Resumo(long faturamento, long vendasBrutas, long reembolsos, long lucro, int pedidos,
                         long ticketMedio, BigDecimal margemPercentual) {}
    public record ProdutoVendido(String produtoId, String nome, int quantidade, long receita) {}
    public record Painel(LocalDate hoje, Resumo hojeResumo, Resumo mesResumo, long meta, BigDecimal progressoMeta,
                         Map<LocalDate, Long> vendasSemana, List<Produto> estoqueBaixo, List<Movimentacao> recentes) {}
    public record ResumoCompras(long total, int quantidadeItens, List<Compra> compras, Map<String, Long> porFornecedor) {}
    public record ResumoEstoque(int produtos, int unidades, long valorCusto, long valorVenda, List<Produto> emAlerta,
                                Map<String, Integer> unidadesPorCategoria) {}
    public record ResumoFinanceiro(long aPagarAberto, long aReceberAberto, long vencido, long pagoNoPeriodo,
                                   long recebidoNoPeriodo, long saldoPrevisto) {}

    public LocalDate hoje() { return LocalDate.now(relogio); }

    // ---------- Dashboard ----------
    public Painel painel(Perfil perfil) {
        Autorizacao.exigir(perfil, true);
        LocalDate hoje = hoje();
        return repositorio.consultar(e -> {
            Resumo dia = resumo(e, Periodo.dia(hoje));
            Resumo mes = resumo(e, Periodo.mes(hoje));
            long meta = Long.parseLong(e.config.getOrDefault(ServicoFinanceiro.CHAVE_META,
                String.valueOf(ServicoFinanceiro.META_PADRAO)));
            BigDecimal progresso = percentual(mes.faturamento(), meta).min(new BigDecimal("100"));
            if (progresso.signum() < 0) progresso = BigDecimal.ZERO;
            List<Movimentacao> recentes = e.movimentacoes.values().stream()
                .sorted(Comparator.comparing(Movimentacao::data).reversed()).limit(8).toList();
            return new Painel(hoje, dia, mes, meta, progresso, vendasPorDia(e, new Periodo(hoje.minusDays(6), hoje)),
                e.produtos.values().stream().filter(Produto::emAlerta).toList(), recentes);
        });
    }

    // ---------- Aba Vendas ----------
    public Resumo resumo(Perfil perfil, Periodo p) {
        Autorizacao.exigir(perfil, true);
        return repositorio.consultar(e -> resumo(e, p));
    }
    public Map<LocalDate, Long> vendasPorDia(Perfil perfil, Periodo p) {
        Autorizacao.exigir(perfil, true);
        return repositorio.consultar(e -> vendasPorDia(e, p));
    }
    /** Total vendido (vendas confirmadas) em cada mês do ano; índice 0 = janeiro. */
    public long[] vendasPorMes(Perfil perfil, int ano) {
        Autorizacao.exigir(perfil, true);
        return repositorio.consultar(e -> {
            long[] meses = new long[12];
            for (Venda v : confirmadas(e, x -> x.confirmadaEm().getYear() == ano))
                meses[v.confirmadaEm().getMonthValue() - 1] += v.totalCentavos();
            return meses;
        });
    }
    public Map<String, Long> vendasPorCategoria(Perfil perfil, Periodo p) {
        Autorizacao.exigir(perfil, true);
        return repositorio.consultar(e -> {
            Map<String, Long> r = new LinkedHashMap<>();
            for (Venda v : confirmadas(e, x -> p.contem(x.confirmadaEm())))
                for (ItemVenda i : v.itens()) {
                    Produto prod = e.produtos.get(i.produtoId());
                    String cat = prod == null ? Categoria.GERAL : prod.categoriaId();
                    Categoria c = e.categorias.get(cat);
                    r.merge(c == null ? "Geral" : c.nome(), i.liquidoCentavos(), Long::sum);
                }
            return ordenarDesc(r);
        });
    }
    public Map<FormaPagamento, Long> vendasPorFormaPagamento(Perfil perfil, Periodo p) {
        Autorizacao.exigir(perfil, true);
        return repositorio.consultar(e -> {
            Map<FormaPagamento, Long> r = new EnumMap<>(FormaPagamento.class);
            for (FormaPagamento f : FormaPagamento.values()) r.put(f, 0L);
            for (Venda v : confirmadas(e, x -> p.contem(x.confirmadaEm())))
                r.merge(v.formaPagamento(), v.totalCentavos(), Long::sum);
            return r;
        });
    }
    public List<ProdutoVendido> maisVendidos(Perfil perfil, Periodo p, int limite) {
        Autorizacao.exigir(perfil, true);
        return repositorio.consultar(e -> {
            Map<String, int[]> qtd = new HashMap<>();
            Map<String, Long> receita = new HashMap<>();
            for (Venda v : confirmadas(e, x -> p.contem(x.confirmadaEm())))
                for (ItemVenda i : v.itens()) {
                    qtd.computeIfAbsent(i.produtoId(), k -> new int[1])[0] += i.quantidade();
                    receita.merge(i.produtoId(), i.liquidoCentavos(), Long::sum);
                }
            return qtd.entrySet().stream()
                .map(x -> new ProdutoVendido(x.getKey(), Optional.ofNullable(e.produtos.get(x.getKey()))
                    .map(Produto::nome).orElse(x.getKey()), x.getValue()[0], receita.get(x.getKey())))
                .sorted(Comparator.comparingInt(ProdutoVendido::quantidade).reversed()
                    .thenComparing(ProdutoVendido::produtoId))
                .limit(limite).toList();
        });
    }

    // ---------- Abas Compras, Estoque e Financeiro ----------
    public ResumoCompras compras(Perfil perfil, Periodo p) {
        Autorizacao.exigir(perfil, true);
        return repositorio.consultar(e -> {
            List<Compra> lista = e.compras.values().stream().filter(c -> p.contem(c.data()))
                .sorted(Comparator.comparing(Compra::data).reversed()).toList();
            Map<String, Long> porForn = new LinkedHashMap<>();
            for (Compra c : lista) {
                Fornecedor f = e.fornecedores.get(c.fornecedorId());
                porForn.merge(f == null ? c.fornecedorId() : f.nome(), c.totalCentavos(), Long::sum);
            }
            return new ResumoCompras(lista.stream().mapToLong(Compra::totalCentavos).sum(),
                lista.stream().mapToInt(Compra::quantidade).sum(), lista, ordenarDesc(porForn));
        });
    }
    public ResumoEstoque estoque(Perfil perfil) {
        Autorizacao.exigir(perfil, true);
        return repositorio.consultar(e -> {
            Map<String, Integer> porCat = new LinkedHashMap<>();
            long custo = 0, venda = 0;
            int unidades = 0;
            for (Produto p : e.produtos.values()) {
                custo = Math.addExact(custo, Math.multiplyExact(p.custoCentavos(), p.estoque()));
                venda = Math.addExact(venda, Math.multiplyExact(p.precoCentavos(), p.estoque()));
                unidades += p.estoque();
                Categoria c = e.categorias.get(p.categoriaId());
                porCat.merge(c == null ? "Geral" : c.nome(), p.estoque(), Integer::sum);
            }
            return new ResumoEstoque(e.produtos.size(), unidades, custo, venda,
                e.produtos.values().stream().filter(Produto::emAlerta).toList(), porCat);
        });
    }
    public ResumoFinanceiro financeiro(Perfil perfil, Periodo p) {
        Autorizacao.exigir(perfil, true);
        LocalDate hoje = hoje();
        return repositorio.consultar(e -> {
            long pagar = 0, receber = 0, vencido = 0, pago = 0, recebido = 0;
            for (ContaFinanceira c : e.contas.values()) {
                if (c.situacao() == SituacaoConta.ABERTA) {
                    if (c.tipo() == TipoConta.PAGAR) pagar += c.valorCentavos(); else receber += c.valorCentavos();
                    if (c.vencida(hoje)) vencido += c.valorCentavos();
                } else if (c.situacao() == SituacaoConta.QUITADA && !c.quitadaEm().isBefore(p.inicio())
                    && !c.quitadaEm().isAfter(p.fim())) {
                    if (c.tipo() == TipoConta.PAGAR) pago += c.valorCentavos(); else recebido += c.valorCentavos();
                }
            }
            return new ResumoFinanceiro(pagar, receber, vencido, pago, recebido, receber - pagar);
        });
    }

    // ---------- cálculos internos ----------
    private static List<Venda> confirmadas(EstadoDados e, Predicate<Venda> filtro) {
        return e.vendas.values().stream().filter(v -> v.estado() == EstadoVenda.CONFIRMADA).filter(filtro).toList();
    }
    static Resumo resumo(EstadoDados e, Periodo p) {
        List<Venda> vendas = confirmadas(e, v -> p.contem(v.confirmadaEm()));
        long brutas = vendas.stream().mapToLong(Venda::totalCentavos).reduce(0, Math::addExact);
        long lucro = vendas.stream().flatMap(v -> v.itens().stream())
            .mapToLong(i -> i.liquidoCentavos() - i.custoTotalCentavos()).reduce(0, Math::addExact);
        long reembolsos = 0;
        for (Devolucao d : e.devolucoes.values()) {
            if (!p.contem(d.data())) continue;
            reembolsos += d.reembolsoCentavos();
            Venda v = e.vendas.get(d.vendaId());
            long custoDevolvido = v == null ? 0 : Math.multiplyExact(v.item(d.produtoId()).custoUnitarioCentavos(), d.quantidade());
            lucro -= d.reembolsoCentavos() - custoDevolvido;
        }
        long faturamento = brutas - reembolsos;
        long ticket = vendas.isEmpty() ? 0 : BigDecimal.valueOf(brutas)
            .divide(BigDecimal.valueOf(vendas.size()), 0, RoundingMode.HALF_UP).longValueExact();
        return new Resumo(faturamento, brutas, reembolsos, lucro, vendas.size(), ticket, percentual(lucro, faturamento));
    }
    private static Map<LocalDate, Long> vendasPorDia(EstadoDados e, Periodo p) {
        Map<LocalDate, Long> dias = new LinkedHashMap<>();
        for (LocalDate d = p.inicio(); !d.isAfter(p.fim()); d = d.plusDays(1)) dias.put(d, 0L);
        for (Venda v : confirmadas(e, x -> p.contem(x.confirmadaEm())))
            dias.merge(v.confirmadaEm().toLocalDate(), v.totalCentavos(), Long::sum);
        return dias;
    }
    static BigDecimal percentual(long parte, long todo) {
        if (todo <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(parte).multiply(new BigDecimal("100"))
            .divide(BigDecimal.valueOf(todo), 1, RoundingMode.HALF_UP);
    }
    private static <K> Map<K, Long> ordenarDesc(Map<K, Long> m) {
        return m.entrySet().stream().sorted(Map.Entry.<K, Long>comparingByValue().reversed())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }
}
