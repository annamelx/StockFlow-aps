package br.ufpb.aps.grupo12.servicos;

import br.ufpb.aps.grupo12.dominio.*;
import br.ufpb.aps.grupo12.observadores.*;
import br.ufpb.aps.grupo12.persistencia.*;
import br.ufpb.aps.grupo12.politicas.PoliticaDesconto;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

/** Casos de uso, integridade e autorização não ficam em App/console. */
public final class ServicoLoja {
    private static final Logger LOG = Logger.getLogger(ServicoLoja.class.getName());
    private final Repositorio repositorio;
    private final List<ObservadorEstoque> observadores = new ArrayList<>();
    private final Clock relogio;

    /** Dados complementares da venda (PDV). clienteId vazio = consumidor não identificado. */
    public record DadosVenda(String clienteId, FormaPagamento formaPagamento, String atendente) {
        public static final DadosVenda BALCAO = new DadosVenda("", FormaPagamento.DINHEIRO, "");
    }
    /** Dados opcionais de compra numa reposição (fornecedor e custo unitário pago). */
    public record DadosCompra(String fornecedorId, long custoUnitarioCentavos) {}

    public ServicoLoja(Repositorio repositorio) { this(repositorio, Clock.systemDefaultZone()); }
    /** O relógio é injetado para que testes e dados de demonstração tenham datas controladas. */
    public ServicoLoja(Repositorio repositorio, Clock relogio) {
        this.repositorio = Objects.requireNonNull(repositorio);
        this.relogio = Objects.requireNonNull(relogio);
    }
    private LocalDateTime agora() { return LocalDateTime.now(relogio).withNano(0); }
    public void observar(ObservadorEstoque o) { observadores.add(Objects.requireNonNull(o)); }

    private static void autorizar(Perfil perfil, boolean requerGerente) {
        if (perfil == null || (requerGerente && perfil != Perfil.GERENTE))
            throw new ErroNegocio("Perfil não tem autorização para esta operação.");
    }
    private static Produto produto(EstadoDados e, String id) {
        Produto p = e.produtos.get(id);
        if (p == null) throw new ErroNegocio("Produto não encontrado: " + id);
        return p;
    }
    private static Venda venda(EstadoDados e, String id) {
        Venda v = e.vendas.get(id);
        if (v == null) throw new ErroNegocio("Venda não encontrada: " + id);
        return v;
    }
    private void publicarDepoisDoCommit(List<Produto> alterados) {
        for (Produto p : alterados) {
            EventoEstoque evento = new EventoEstoque(p.id(), p.estoque(), p.estoqueMinimo());
            for (ObservadorEstoque o : observadores) {
                try { o.aoAlterarEstoque(evento); }
                catch (RuntimeException erro) {
                    // Observador falha sem reverter uma operação que já foi persistida.
                    LOG.warning("Falha no observador de estoque: " + erro.getMessage());
                }
            }
        }
    }
    private static String idMovimento(String categoria, String chave) {
        UUID deterministico = UUID.nameUUIDFromBytes((categoria + ":" + chave).getBytes(StandardCharsets.UTF_8));
        return "MOV_" + deterministico.toString().replace("-", "");
    }

    public void cadastrarProduto(Perfil perfil, Produto p) {
        autorizar(perfil, true);
        Objects.requireNonNull(p);
        repositorio.transacionar(e -> {
            if (!p.categoriaId().equals(Categoria.GERAL) && !e.categorias.containsKey(p.categoriaId()))
                throw new ErroNegocio("Categoria inexistente: " + p.categoriaId());
            if (e.produtos.putIfAbsent(p.id(), p) != null) throw new ErroNegocio("Produto duplicado.");
            return null;
        });
    }
    /**
     * Altera nome, preço, custo, mínimo e categoria. O SALDO não é editável aqui: só muda por
     * venda, devolução ou reposição, para que o histórico de movimentações explique o estoque.
     */
    public Produto editarProduto(Perfil perfil, String id, String nome, long preco, long custo, int minimo,
                                 String categoriaId) {
        autorizar(perfil, true);
        return repositorio.transacionar(e -> {
            Produto atual = produto(e, id);
            String cat = categoriaId == null || categoriaId.isBlank() ? Categoria.GERAL : categoriaId;
            if (!cat.equals(Categoria.GERAL) && !e.categorias.containsKey(cat))
                throw new ErroNegocio("Categoria inexistente: " + cat);
            Produto novo = new Produto(id, nome, preco, atual.estoque(), minimo, cat, custo);
            e.produtos.put(id, novo);
            return novo;
        });
    }
    public Produto reporEstoque(Perfil perfil, String produtoId, int quantidade, String lancamentoId) {
        return reporEstoque(perfil, produtoId, quantidade, lancamentoId, null);
    }
    /**
     * Reposição; com {@code compra} informada, registra também a compra do fornecedor e atualiza o
     * custo do produto para o custo pago — tudo na MESMA transação (estoque, movimento, compra).
     */
    public Produto reporEstoque(Perfil perfil, String produtoId, int quantidade, String lancamentoId,
                                DadosCompra compra) {
        autorizar(perfil, true);
        Validacao.id(lancamentoId);
        Validacao.quantidadePositiva(quantidade);
        LocalDateTime quando = agora();
        Produto atualizado = repositorio.transacionar(e -> {
            String movimentoId = idMovimento("REPOSICAO", lancamentoId);
            if (e.movimentacoes.containsKey(movimentoId))
                throw new ErroNegocio("Lançamento de reposição duplicado.");
            Produto antigo = produto(e, produtoId);
            Produto novo = antigo.comEstoque(Math.addExact(antigo.estoque(), quantidade));
            if (compra != null) {
                if (!e.fornecedores.containsKey(compra.fornecedorId()))
                    throw new ErroNegocio("Fornecedor inexistente: " + compra.fornecedorId());
                e.compras.put(lancamentoId, new Compra(lancamentoId, compra.fornecedorId(), produtoId, quantidade,
                    compra.custoUnitarioCentavos(), quando));
                novo = new Produto(novo.id(), novo.nome(), novo.precoCentavos(), novo.estoque(),
                    novo.estoqueMinimo(), novo.categoriaId(), compra.custoUnitarioCentavos());
            }
            e.produtos.put(produtoId, novo);
            e.movimentacoes.put(movimentoId, new Movimentacao(movimentoId, lancamentoId,
                produtoId, TipoMovimentacao.ENTRADA_REPOSICAO, quantidade, quando));
            return novo;
        });
        publicarDepoisDoCommit(List.of(atualizado));
        return atualizado;
    }

    public Venda abrirVenda(Perfil perfil, String vendaId, Map<String, Integer> quantidades,
                           PoliticaDesconto politica) {
        return abrirVenda(perfil, vendaId, quantidades, politica, DadosVenda.BALCAO);
    }
    public Venda abrirVenda(Perfil perfil, String vendaId, Map<String, Integer> quantidades,
                           PoliticaDesconto politica, DadosVenda dados) {
        autorizar(perfil, false);
        Validacao.id(vendaId);
        Objects.requireNonNull(dados);
        LocalDateTime quando = agora();
        return repositorio.transacionar(e -> {
            if (e.vendas.containsKey(vendaId)) throw new ErroNegocio("ID de venda duplicado.");
            String cliente = dados.clienteId() == null ? "" : dados.clienteId();
            if (!cliente.isEmpty() && !e.clientes.containsKey(cliente))
                throw new ErroNegocio("Cliente inexistente: " + cliente);
            Venda v = montarVenda(e, vendaId, quantidades, politica, cliente, dados, quando);
            e.vendas.put(v.id(), v);
            return v;
        });
    }
    /**
     * Calcula subtotal, desconto rateado e total SEM gravar nada — usado pelo PDV para mostrar
     * o resumo. Usa exatamente o mesmo código de abrirVenda, então a prévia nunca diverge.
     */
    public Venda simularVenda(Perfil perfil, Map<String, Integer> quantidades, PoliticaDesconto politica) {
        autorizar(perfil, false);
        LocalDateTime quando = agora();
        return repositorio.consultar(e -> montarVenda(e, "PREVIA", quantidades, politica, "", DadosVenda.BALCAO, quando));
    }
    private Venda montarVenda(EstadoDados e, String vendaId, Map<String, Integer> quantidades,
                              PoliticaDesconto politica, String cliente, DadosVenda dados, LocalDateTime quando) {
        Objects.requireNonNull(politica);
        if (quantidades == null || quantidades.isEmpty()) throw new ErroNegocio("Venda sem itens.");
        List<String> ids = new ArrayList<>(quantidades.keySet());
        Collections.sort(ids);
        List<ItemVenda> brutos = new ArrayList<>();
        long subtotal = 0;
        for (String id : ids) {
            Integer qtd = quantidades.get(id);
            if (qtd == null) throw new ErroNegocio("Quantidade obrigatória.");
            Validacao.quantidadePositiva(qtd);
            Produto p = produto(e, id);
            ItemVenda item = new ItemVenda(id, qtd, p.precoCentavos(), 0, p.custoCentavos());
            subtotal = Math.addExact(subtotal, item.brutoCentavos());
            brutos.add(item);
        }
        long desconto = politica.descontoEmCentavos(subtotal);
        if (desconto < 0 || desconto > subtotal) throw new ErroNegocio("Desconto inválido.");
        List<ItemVenda> rateados = distribuirDesconto(brutos, subtotal, desconto);
        return new Venda(vendaId, EstadoVenda.PENDENTE, rateados, subtotal, desconto, subtotal - desconto,
            politica.nome(), cliente, dados.formaPagamento(), dados.atendente(), quando, null);
    }

    /** Rateio por maior resto: soma exata de centavos, mesmo com itens baratos. */
    private static List<ItemVenda> distribuirDesconto(List<ItemVenda> itens, long subtotal, long desconto) {
        int n = itens.size();
        long[] partes = new long[n];
        BigInteger[] restos = new BigInteger[n];
        long atribuido = 0;
        BigInteger total = BigInteger.valueOf(subtotal);
        for (int i = 0; i < n; i++) {
            BigInteger[] div = BigInteger.valueOf(desconto)
                .multiply(BigInteger.valueOf(itens.get(i).brutoCentavos())).divideAndRemainder(total);
            partes[i] = div[0].longValueExact();
            restos[i] = div[1];
            atribuido += partes[i];
        }
        List<Integer> ordem = new ArrayList<>();
        for (int i = 0; i < n; i++) ordem.add(i);
        ordem.sort((a, b) -> { int c = restos[b].compareTo(restos[a]); return c != 0 ? c : Integer.compare(a,b); });
        long restante = desconto - atribuido;
        for (int i = 0; i < restante; i++) partes[ordem.get(i)]++;
        List<ItemVenda> resposta = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ItemVenda original = itens.get(i);
            resposta.add(original.comDesconto(partes[i]));
        }
        return resposta;
    }
    private record Confirmacao(Venda venda, List<Produto> alterados) {}

    public Venda confirmarVenda(Perfil perfil, String vendaId) {
        autorizar(perfil, false);
        LocalDateTime quando = agora();
        Confirmacao resultado = repositorio.transacionar(e -> {
            Venda v = venda(e, vendaId);
            if (v.estado() == EstadoVenda.CONFIRMADA) return new Confirmacao(v, List.of());
            if (v.estado() != EstadoVenda.PENDENTE)
                throw new ErroNegocio("Venda cancelada não pode ser confirmada.");
            // Pré-validação integral: não altera nenhum item antes de verificar todos.
            for (ItemVenda i : v.itens()) {
                Produto p = produto(e, i.produtoId());
                if (p.estoque() < i.quantidade())
                    throw new ErroNegocio("Estoque insuficiente: " + p.id());
            }
            List<Produto> alterados = new ArrayList<>();
            for (ItemVenda i : v.itens()) {
                Produto p = produto(e, i.produtoId());
                Produto atualizado = p.comEstoque(p.estoque() - i.quantidade());
                e.produtos.put(p.id(), atualizado);
                alterados.add(atualizado);
                String movId = idMovimento("VENDA", v.id() + ":" + p.id());
                e.movimentacoes.put(movId, new Movimentacao(movId, v.id(), p.id(),
                    TipoMovimentacao.SAIDA_VENDA, i.quantidade(), quando));
            }
            Venda confirmada = v.comEstado(EstadoVenda.CONFIRMADA, quando);
            e.vendas.put(v.id(), confirmada);
            return new Confirmacao(confirmada, List.copyOf(alterados));
        });
        publicarDepoisDoCommit(resultado.alterados());
        return resultado.venda();
    }
    public Venda cancelarVendaPendente(Perfil perfil, String vendaId) {
        autorizar(perfil, false);
        return repositorio.transacionar(e -> {
            Venda v = venda(e, vendaId);
            Venda cancelada = v.comEstado(EstadoVenda.CANCELADA, null);
            e.vendas.put(cancelada.id(), cancelada);
            return cancelada;
        });
    }
    private record ResultadoDevolucao(Devolucao devolucao, List<Produto> alterados) {}

    public Devolucao devolver(Perfil perfil, String devolucaoId, String vendaId,
                               String produtoId, int quantidade) {
        autorizar(perfil, true);
        Validacao.id(devolucaoId); Validacao.id(vendaId); Validacao.id(produtoId);
        Validacao.quantidadePositiva(quantidade);
        LocalDateTime quando = agora();
        ResultadoDevolucao r = repositorio.transacionar(e -> {
            Devolucao anterior = e.devolucoes.get(devolucaoId);
            if (anterior != null) {
                if (anterior.vendaId().equals(vendaId) && anterior.produtoId().equals(produtoId)
                    && anterior.quantidade() == quantidade)
                    return new ResultadoDevolucao(anterior, List.of());
                throw new ErroNegocio("ID de devolução reutilizado com dados diferentes.");
            }
            Venda v = venda(e, vendaId);
            if (v.estado() != EstadoVenda.CONFIRMADA)
                throw new ErroNegocio("Devolução exige venda confirmada.");
            ItemVenda item = v.item(produtoId);
            int jaDevolvido = e.devolucoes.values().stream()
                .filter(d -> d.vendaId().equals(vendaId) && d.produtoId().equals(produtoId))
                .mapToInt(Devolucao::quantidade).reduce(0, Math::addExact);
            if (quantidade > item.quantidade() - jaDevolvido)
                throw new ErroNegocio("Devolução excede a quantidade efetivamente vendida.");
            long reembolso = item.restituiAte(jaDevolvido + quantidade) - item.restituiAte(jaDevolvido);
            Produto atual = produto(e, produtoId);
            Produto novo = atual.comEstoque(Math.addExact(atual.estoque(), quantidade));
            Devolucao d = new Devolucao(devolucaoId, vendaId, produtoId, quantidade, reembolso, quando);
            e.produtos.put(produtoId, novo);
            e.devolucoes.put(devolucaoId, d);
            String movId = idMovimento("DEVOLUCAO", devolucaoId);
            e.movimentacoes.put(movId, new Movimentacao(movId, devolucaoId, produtoId,
                TipoMovimentacao.ENTRADA_DEVOLUCAO, quantidade, quando));
            return new ResultadoDevolucao(d, List.of(novo));
        });
        publicarDepoisDoCommit(r.alterados());
        return r.devolucao();
    }
    public List<Produto> consultarProdutos(Perfil perfil) {
        autorizar(perfil, false);
        return repositorio.consultar(e -> List.copyOf(e.produtos.values()));
    }
    public List<Produto> consultarProdutosEmAlerta(Perfil perfil) {
        autorizar(perfil, false);
        return repositorio.consultar(e -> e.produtos.values().stream().filter(Produto::emAlerta).toList());
    }
    public List<Venda> consultarVendas(Perfil perfil) {
        autorizar(perfil, false);
        return repositorio.consultar(e -> List.copyOf(e.vendas.values()));
    }
    public List<Devolucao> consultarDevolucoes(Perfil perfil) {
        autorizar(perfil, false);
        return repositorio.consultar(e -> List.copyOf(e.devolucoes.values()));
    }
    public List<Movimentacao> consultarMovimentacoes(Perfil perfil) {
        autorizar(perfil, false);
        return repositorio.consultar(e -> List.copyOf(e.movimentacoes.values()));
    }
    /** Líquido de vendas confirmadas após reembolsos: não é saldo bancário real. */
    public long consultarReceitaLiquidaCentavos(Perfil perfil) {
        autorizar(perfil, false);
        return repositorio.consultar(e -> {
            long vendas = e.vendas.values().stream().filter(v -> v.estado() == EstadoVenda.CONFIRMADA)
                .mapToLong(Venda::totalCentavos).reduce(0, Math::addExact);
            long reembolsos = e.devolucoes.values().stream().mapToLong(Devolucao::reembolsoCentavos)
                .reduce(0, Math::addExact);
            return vendas - reembolsos;
        });
    }
    public String exportarRelatorio(Perfil perfil, br.ufpb.aps.grupo12.relatorios.ExportadorRelatorio formato) {
        autorizar(perfil, false);
        Objects.requireNonNull(formato);
        return repositorio.consultar(formato::exportar);
    }
}
