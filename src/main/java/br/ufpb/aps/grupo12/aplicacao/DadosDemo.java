package br.ufpb.aps.grupo12.aplicacao;

import br.ufpb.aps.grupo12.dominio.*;
import br.ufpb.aps.grupo12.persistencia.EstadoDados;
import br.ufpb.aps.grupo12.persistencia.Repositorio;
import br.ufpb.aps.grupo12.persistencia.RepositorioMemoria;
import br.ufpb.aps.grupo12.politicas.*;
import br.ufpb.aps.grupo12.seguranca.ContasDemo;
import br.ufpb.aps.grupo12.servicos.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/**
 * Gera a base fictícia de demonstração executando OPERAÇÕES REAIS dos serviços (compras, vendas,
 * confirmações, devoluções, contas) com um relógio ajustável — assim o histórico respeita todas
 * as regras e os gráficos têm dados verdadeiros.
 *
 * Reprodutível: o sorteio usa semente fixa (12) e as datas são relativas ao dia da restauração.
 * Rodando --reset-demo no mesmo dia, o resultado é idêntico. Os saldos finais são sempre os de
 * {@link #SALDO_FINAL}.
 */
public final class DadosDemo {
    private DadosDemo() {}
    private static final Perfil G = Perfil.GERENTE;
    private static final int DIAS_HISTORICO = 75;

    record ProdutoDemo(String id, String nome, String categoria, long preco, long custo, int minimo, int peso) {}
    static final List<ProdutoDemo> PRODUTOS = List.of(
        new ProdutoDemo("BOLSA01", "Bolsa transversal", "BOLSAS", 15000, 7000, 3, 9),
        new ProdutoDemo("BOLSA02", "Bolsa de ombro", "BOLSAS", 20000, 9500, 2, 6),
        new ProdutoDemo("CARTEIRA01", "Carteira feminina", "CARTEIRAS", 4500, 1800, 2, 8),
        new ProdutoDemo("MOCHILA01", "Mochila couro sintético", "MOCHILAS", 18990, 8500, 2, 4),
        new ProdutoDemo("CLUTCH01", "Clutch festa dourada", "BOLSAS", 12990, 5500, 2, 3),
        new ProdutoDemo("NECESSAIRE01", "Nécessaire matelassê", "ACESSORIOS", 5990, 2200, 3, 5),
        new ProdutoDemo("TOTE01", "Bolsa tote grande", "BOLSAS", 24990, 12000, 2, 3),
        new ProdutoDemo("CHAVEIRO01", "Chaveiro pompom", "ACESSORIOS", 1990, 600, 5, 7));
    /** Saldos ao final da restauração (dois produtos ficam em alerta de estoque mínimo). */
    public static final Map<String, Integer> SALDO_FINAL = Map.of("BOLSA01", 10, "BOLSA02", 6, "CARTEIRA01", 8,
        "MOCHILA01", 5, "CLUTCH01", 1, "NECESSAIRE01", 12, "TOTE01", 1, "CHAVEIRO01", 20);

    /** Restaura a base de demonstração no repositório informado (uma única transação). */
    public static void restaurar(Repositorio destino, LocalDate hoje) {
        EstadoDados gerado = gerar(hoje);
        destino.transacionar(e -> {
            e.limpar();
            e.produtos.putAll(gerado.produtos); e.vendas.putAll(gerado.vendas);
            e.devolucoes.putAll(gerado.devolucoes); e.movimentacoes.putAll(gerado.movimentacoes);
            e.categorias.putAll(gerado.categorias); e.clientes.putAll(gerado.clientes);
            e.fornecedores.putAll(gerado.fornecedores); e.compras.putAll(gerado.compras);
            e.contas.putAll(gerado.contas); e.usuarios.putAll(gerado.usuarios); e.config.putAll(gerado.config);
            return null;
        });
    }

    /** Garante que exista ao menos o login de demonstração (arquivos antigos não tinham usuárias). */
    public static void garantirUsuarias(Repositorio repo) {
        repo.transacionar(e -> {
            if (e.usuarios.isEmpty()) ContasDemo.USUARIOS.forEach(u -> e.usuarios.put(u.email(), u));
            return null;
        });
    }

    static EstadoDados gerar(LocalDate hoje) {
        RepositorioMemoria mem = new RepositorioMemoria();
        LocalDate inicio = hoje.minusDays(DIAS_HISTORICO);
        RelogioAjustavel relogio = new RelogioAjustavel(inicio.atTime(8, 0), ZoneId.systemDefault());
        ServicoLoja loja = new ServicoLoja(mem, relogio);
        ServicoCadastros cad = new ServicoCadastros(mem);
        ServicoFinanceiro fin = new ServicoFinanceiro(mem, relogio);
        Random sorteio = new Random(12);

        garantirUsuarias(mem);
        fin.definirMeta(G, ServicoFinanceiro.META_PADRAO);
        cad.salvarCategoria(G, new Categoria("BOLSAS", "Bolsas"), true);
        cad.salvarCategoria(G, new Categoria("CARTEIRAS", "Carteiras"), true);
        cad.salvarCategoria(G, new Categoria("MOCHILAS", "Mochilas"), true);
        cad.salvarCategoria(G, new Categoria("ACESSORIOS", "Acessórios"), true);
        for (ProdutoDemo p : PRODUTOS)
            loja.cadastrarProduto(G, new Produto(p.id(), p.nome(), p.preco(), 0, p.minimo(), p.categoria(), p.custo()));
        String[][] clientes = {{"Anna Karolina", "(83) 99811-2040", "anna.karolina@exemplo.com"},
            {"Awanna Costa", "(83) 98720-3311", "awanna@exemplo.com"}, {"Cynthia Souza", "(83) 99654-0087", ""},
            {"Caio", "(83) 98102-5566", "caio@exemplo.com"}};
        for (int i = 0; i < clientes.length; i++)
            cad.salvarCliente(G, new Cliente(String.format("CLI-%03d", i + 1), clientes[i][0], clientes[i][1], clientes[i][2]), true);
        cad.salvarFornecedor(G, new Fornecedor("FOR-001", "Couro & Arte Distribuidora", "12.345.678/0001-90", "(81) 3222-1100"), true);
        cad.salvarFornecedor(G, new Fornecedor("FOR-002", "Acessórios Nordeste Ltda", "98.765.432/0001-10", "(83) 3333-4455"), true);

        int[] seq = {0, 0}; // [compras, vendas]
        Map<String, Integer> saldo = new HashMap<>();
        PRODUTOS.forEach(p -> saldo.put(p.id(), 0));
        // estoque inicial comprado no primeiro dia
        for (ProdutoDemo p : PRODUTOS) comprar(loja, p, 12 + p.peso(), seq, saldo, sorteio);

        FormaPagamento[] formas = FormaPagamento.values();
        int pesoTotal = PRODUTOS.stream().mapToInt(ProdutoDemo::peso).sum();
        for (int d = 0; d < DIAS_HISTORICO; d++) {
            LocalDate dia = inicio.plusDays(d);
            int vendasDia = dia.getDayOfWeek() == DayOfWeek.SUNDAY ? 0
                : 1 + sorteio.nextInt(dia.getDayOfWeek() == DayOfWeek.SATURDAY ? 5 : 3);
            for (int k = 0; k < vendasDia; k++) {
                relogio.ajustar(dia.atTime(9 + sorteio.nextInt(9), sorteio.nextInt(60)));
                Map<String, Integer> itens = new TreeMap<>();
                int nItens = 1 + (sorteio.nextInt(4) == 0 ? 1 : 0);
                for (int j = 0; j < nItens; j++) {
                    ProdutoDemo p = sortearProduto(sorteio, pesoTotal);
                    itens.merge(p.id(), 1 + (p.preco() < 6000 ? sorteio.nextInt(2) : 0), Integer::sum);
                }
                for (var it : itens.entrySet()) {
                    ProdutoDemo p = PRODUTOS.stream().filter(x -> x.id().equals(it.getKey())).findFirst().orElseThrow();
                    if (saldo.get(p.id()) < it.getValue() + 1) comprar(loja, p, 6 + sorteio.nextInt(4), seq, saldo, sorteio);
                }
                PoliticaDesconto politica = switch (sorteio.nextInt(6)) {
                    case 0 -> new DescontoPercentual(new BigDecimal("10"));
                    case 1 -> DescontoPorFaixa.padraoDaLoja();
                    case 2 -> new DescontoFixo(1000);
                    default -> new DescontoPercentual(BigDecimal.ZERO);
                };
                String cliente = sorteio.nextInt(3) == 0 ? "" : String.format("CLI-%03d", 1 + sorteio.nextInt(clientes.length));
                String atendente = sorteio.nextBoolean() ? "Karol" : "Cynthia Souza";
                String id = "V" + (++seq[1]);
                try {
                    loja.abrirVenda(G, id, itens, politica,
                        new ServicoLoja.DadosVenda(cliente, formas[sorteio.nextInt(formas.length)], atendente));
                } catch (ErroNegocio descontoMaiorQueSubtotal) {
                    loja.abrirVenda(G, id, itens, new DescontoPercentual(BigDecimal.ZERO),
                        new ServicoLoja.DadosVenda(cliente, FormaPagamento.PIX, atendente));
                }
                relogio.ajustar(LocalDateTime.now(relogio).plusMinutes(3));
                if (d == 20 && k == 0) { loja.cancelarVendaPendente(G, id); continue; }
                loja.confirmarVenda(G, id);
                itens.forEach((pid, q) -> saldo.merge(pid, -q, Integer::sum));
                if (sorteio.nextInt(25) == 0) { // devolução ocasional de 1 unidade
                    String pid = itens.keySet().iterator().next();
                    relogio.ajustar(LocalDateTime.now(relogio).plusDays(1));
                    loja.devolver(G, "D" + (loja.consultarDevolucoes(G).size() + 1), id, pid, 1);
                    saldo.merge(pid, 1, Integer::sum);
                }
            }
        }
        // Vendas já feitas "hoje" (manhã), para o Dashboard mostrar o dia corrente.
        record VendaHoje(String produto, int qtd, int hora, int minuto, FormaPagamento forma, String cliente) {}
        List<VendaHoje> hojeVendas = List.of(
            new VendaHoje("BOLSA01", 1, 9, 12, FormaPagamento.PIX, "CLI-001"),
            new VendaHoje("CARTEIRA01", 2, 9, 47, FormaPagamento.CARTAO_CREDITO, ""),
            new VendaHoje("CHAVEIRO01", 3, 10, 5, FormaPagamento.DINHEIRO, "CLI-004"),
            new VendaHoje("NECESSAIRE01", 1, 10, 52, FormaPagamento.CARTAO_DEBITO, "CLI-003"));
        Map<String, Integer> vendidoHoje = new HashMap<>();
        hojeVendas.forEach(v -> vendidoHoje.merge(v.produto(), v.qtd(), Integer::sum));
        // Ajuste nos últimos dias: compras ou pequenas vendas extras para chegar aos saldos documentados.
        int extra = 0;
        for (ProdutoDemo p : PRODUTOS) {
            int alvo = SALDO_FINAL.get(p.id()) + vendidoHoje.getOrDefault(p.id(), 0);
            int atual = saldo.get(p.id());
            if (atual < alvo) {
                relogio.ajustar(hoje.minusDays(1).atTime(17, 0));
                comprar(loja, p, alvo - atual, seq, saldo, sorteio);
            }
            while (saldo.get(p.id()) > alvo) {
                int q = Math.min(2, saldo.get(p.id()) - alvo);
                LocalDate dia = hoje.minusDays(1 + (extra % 6));
                if (dia.getDayOfWeek() == DayOfWeek.SUNDAY) dia = dia.minusDays(1);
                relogio.ajustar(dia.atTime(11 + (extra % 7), 10 + (extra * 7) % 50));
                extra++;
                String id = "V" + (++seq[1]);
                loja.abrirVenda(G, id, Map.of(p.id(), q), new DescontoPercentual(BigDecimal.ZERO),
                    new ServicoLoja.DadosVenda(String.format("CLI-%03d", 1 + extra % clientes.length),
                        formas[extra % formas.length], extra % 2 == 0 ? "Karol" : "Cynthia Souza"));
                relogio.ajustar(LocalDateTime.now(relogio).plusMinutes(2));
                loja.confirmarVenda(G, id);
                saldo.merge(p.id(), -q, Integer::sum);
            }
        }
        for (VendaHoje v : hojeVendas) {
            relogio.ajustar(hoje.atTime(v.hora(), v.minuto()));
            String id = "V" + (++seq[1]);
            loja.abrirVenda(G, id, Map.of(v.produto(), v.qtd()), new DescontoPercentual(BigDecimal.ZERO),
                new ServicoLoja.DadosVenda(v.cliente(), v.forma(), "Karol"));
            relogio.ajustar(LocalDateTime.now(relogio).plusMinutes(2));
            loja.confirmarVenda(G, id);
            saldo.merge(v.produto(), -v.qtd(), Integer::sum);
        }
        // uma venda pendente para demonstrar confirmação
        relogio.ajustar(hoje.atTime(10, 30));
        loja.abrirVenda(G, "V" + (++seq[1]), Map.of("CARTEIRA01", 1, "CHAVEIRO01", 2), DescontoPorFaixa.padraoDaLoja(),
            new ServicoLoja.DadosVenda("CLI-002", FormaPagamento.CARTAO_DEBITO, "Karol"));

        // contas a pagar e a receber
        relogio.ajustar(hoje.minusDays(20).atTime(9, 0));
        fin.lancarConta(G, "CT-001", TipoConta.PAGAR, "Aluguel da loja", 180000, hoje.minusDays(15));
        fin.quitar(G, "CT-001");
        fin.lancarConta(G, "CT-002", TipoConta.PAGAR, "Energia elétrica", 32000, hoje.minusDays(3));
        fin.lancarConta(G, "CT-003", TipoConta.PAGAR, "Couro & Arte — boleto 2/3", 145000, hoje.plusDays(7));
        fin.lancarConta(G, "CT-004", TipoConta.RECEBER, "Encomenda corporativa (10 carteiras)", 40000, hoje.plusDays(5));
        fin.lancarConta(G, "CT-005", TipoConta.RECEBER, "Cartão — repasse da operadora", 268000, hoje.minusDays(1));
        fin.lancarConta(G, "CT-006", TipoConta.PAGAR, "Internet e telefone", 15990, hoje.plusDays(12));
        relogio.ajustar(hoje.minusDays(1).atTime(16, 0));
        fin.quitar(G, "CT-005");
        return mem.consultar(e -> e);
    }

    private static ProdutoDemo sortearProduto(Random r, int pesoTotal) {
        int x = r.nextInt(pesoTotal);
        for (ProdutoDemo p : PRODUTOS) { x -= p.peso(); if (x < 0) return p; }
        return PRODUTOS.getLast();
    }

    private static void comprar(ServicoLoja loja, ProdutoDemo p, int qtd, int[] seq, Map<String, Integer> saldo, Random r) {
        String forn = p.categoria().equals("ACESSORIOS") || p.categoria().equals("CARTEIRAS") ? "FOR-002" : "FOR-001";
        loja.reporEstoque(G, p.id(), qtd, String.format("REP-%03d", ++seq[0]),
            new ServicoLoja.DadosCompra(forn, p.custo()));
        saldo.merge(p.id(), qtd, Integer::sum);
    }
}
