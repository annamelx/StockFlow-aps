package br.ufpb.aps.grupo12.servicos;

import br.ufpb.aps.grupo12.dominio.*;
import br.ufpb.aps.grupo12.politicas.PoliticaDesconto;
import br.ufpb.aps.grupo12.relatorios.ExportadorRelatorio;
import br.ufpb.aps.grupo12.seguranca.Usuario;
import br.ufpb.aps.grupo12.servicos.ServicoLoja.DadosCompra;
import br.ufpb.aps.grupo12.servicos.ServicoLoja.DadosVenda;
import br.ufpb.aps.grupo12.servicos.ServicoRelatorios.*;
import java.time.LocalDate;
import java.util.*;

/**
 * GoF Facade (estrutural): ponto único de acesso das interfaces (gráfica e console) aos serviços.
 * Não contém regras: apenas encaminha para ServicoLoja, ServicoCadastros, ServicoFinanceiro e
 * ServicoRelatorios.
 */
public final class LojaFacade {
    private final ServicoLoja servico;
    private final ExportadorRelatorio exportador;
    private final ServicoCadastros cadastros;
    private final ServicoFinanceiro financeiro;
    private final ServicoRelatorios relatorios;

    /** Construtor mínimo (vendas/estoque), mantido para os testes originais. */
    public LojaFacade(ServicoLoja servico, ExportadorRelatorio exportador) {
        this(servico, exportador, null, null, null);
    }
    public LojaFacade(ServicoLoja servico, ExportadorRelatorio exportador, ServicoCadastros cadastros,
                      ServicoFinanceiro financeiro, ServicoRelatorios relatorios) {
        this.servico = Objects.requireNonNull(servico);
        this.exportador = Objects.requireNonNull(exportador);
        this.cadastros = cadastros;
        this.financeiro = financeiro;
        this.relatorios = relatorios;
    }
    private static <T> T exigir(T s) {
        if (s == null) throw new IllegalStateException("Serviço não configurado na Facade.");
        return s;
    }

    // ----- Estoque e produtos -----
    public List<Produto> estoque(Perfil perfil) { return servico.consultarProdutos(perfil); }
    public List<Produto> emAlerta(Perfil perfil) { return servico.consultarProdutosEmAlerta(perfil); }
    public void cadastrarProduto(Perfil perfil, Produto p) { servico.cadastrarProduto(perfil, p); }
    public Produto editarProduto(Perfil perfil, String id, String nome, long preco, long custo, int minimo, String categoria) {
        return servico.editarProduto(perfil, id, nome, preco, custo, minimo, categoria);
    }
    public Produto repor(Perfil perfil, String produto, int qtd, String idLancamento) {
        return servico.reporEstoque(perfil, produto, qtd, idLancamento);
    }
    public Produto comprar(Perfil perfil, String produto, int qtd, String idLancamento, String fornecedor, long custoUnit) {
        return servico.reporEstoque(perfil, produto, qtd, idLancamento, new DadosCompra(fornecedor, custoUnit));
    }
    public List<Movimentacao> historico(Perfil perfil) { return servico.consultarMovimentacoes(perfil); }

    // ----- Vendas e devoluções -----
    public Venda abrirVenda(Perfil perfil, String id, Map<String,Integer> itens, PoliticaDesconto politica) {
        return servico.abrirVenda(perfil, id, itens, politica);
    }
    public Venda abrirVenda(Perfil perfil, String id, Map<String,Integer> itens, PoliticaDesconto politica,
                            String clienteId, FormaPagamento forma, Usuario atendente) {
        return servico.abrirVenda(perfil, id, itens, politica,
            new DadosVenda(clienteId, forma, atendente == null ? "" : atendente.nome()));
    }
    public Venda simularVenda(Perfil perfil, Map<String,Integer> itens, PoliticaDesconto politica) {
        return servico.simularVenda(perfil, itens, politica);
    }
    public Venda confirmar(Perfil perfil, String id) { return servico.confirmarVenda(perfil,id); }
    public Venda cancelar(Perfil perfil, String id) { return servico.cancelarVendaPendente(perfil,id); }
    public Devolucao devolver(Perfil perfil, String id, String venda, String produto, int qtd) {
        return servico.devolver(perfil,id,venda,produto,qtd);
    }
    public List<Venda> vendas(Perfil perfil) { return servico.consultarVendas(perfil); }
    public List<Devolucao> devolucoes(Perfil perfil) { return servico.consultarDevolucoes(perfil); }
    public String relatorio(Perfil perfil) { return servico.exportarRelatorio(perfil, exportador); }
    public long receitaLiquida(Perfil perfil) { return servico.consultarReceitaLiquidaCentavos(perfil); }

    // ----- Cadastros -----
    public String proximoId(Perfil perfil, String prefixo) { return exigir(cadastros).proximoId(perfil, prefixo); }
    public List<Categoria> categorias(Perfil perfil) { return exigir(cadastros).categorias(perfil); }
    public Categoria salvarCategoria(Perfil perfil, Categoria c, boolean nova) { return exigir(cadastros).salvarCategoria(perfil, c, nova); }
    public void excluirCategoria(Perfil perfil, String id) { exigir(cadastros).excluirCategoria(perfil, id); }
    public List<Cliente> clientes(Perfil perfil) { return exigir(cadastros).clientes(perfil); }
    public Cliente salvarCliente(Perfil perfil, Cliente c, boolean novo) { return exigir(cadastros).salvarCliente(perfil, c, novo); }
    public void excluirCliente(Perfil perfil, String id) { exigir(cadastros).excluirCliente(perfil, id); }
    public List<Fornecedor> fornecedores(Perfil perfil) { return exigir(cadastros).fornecedores(perfil); }
    public Fornecedor salvarFornecedor(Perfil perfil, Fornecedor f, boolean novo) { return exigir(cadastros).salvarFornecedor(perfil, f, novo); }
    public void excluirFornecedor(Perfil perfil, String id) { exigir(cadastros).excluirFornecedor(perfil, id); }
    public List<Usuario> usuarios(Perfil perfil) { return exigir(cadastros).usuarios(perfil); }
    public Usuario cadastrarUsuario(Perfil perfil, String email, String nome, Perfil novo, String senha) {
        return exigir(cadastros).cadastrarUsuario(perfil, email, nome, novo, senha);
    }
    public Usuario alterarAtivo(Perfil perfil, String email, boolean ativo) { return exigir(cadastros).alterarAtivo(perfil, email, ativo); }

    // ----- Financeiro -----
    public List<ContaFinanceira> contas(Perfil perfil) { return exigir(financeiro).contas(perfil); }
    public ContaFinanceira lancarConta(Perfil perfil, String id, TipoConta tipo, String desc, long valor, LocalDate venc) {
        return exigir(financeiro).lancarConta(perfil, id, tipo, desc, valor, venc);
    }
    public ContaFinanceira quitarConta(Perfil perfil, String id) { return exigir(financeiro).quitar(perfil, id); }
    public ContaFinanceira cancelarConta(Perfil perfil, String id) { return exigir(financeiro).cancelar(perfil, id); }
    public ServicoFinanceiro.Totais totaisContas(Perfil perfil) { return exigir(financeiro).totais(perfil); }
    public long metaMensal(Perfil perfil) { return exigir(financeiro).metaMensal(perfil); }
    public long definirMeta(Perfil perfil, long valor) { return exigir(financeiro).definirMeta(perfil, valor); }

    // ----- Relatórios / Dashboard -----
    public LocalDate hoje() { return exigir(relatorios).hoje(); }
    public Painel painel(Perfil perfil) { return exigir(relatorios).painel(perfil); }
    public Resumo resumo(Perfil perfil, Periodo p) { return exigir(relatorios).resumo(perfil, p); }
    public Map<LocalDate, Long> vendasPorDia(Perfil perfil, Periodo p) { return exigir(relatorios).vendasPorDia(perfil, p); }
    public long[] vendasPorMes(Perfil perfil, int ano) { return exigir(relatorios).vendasPorMes(perfil, ano); }
    public Map<String, Long> vendasPorCategoria(Perfil perfil, Periodo p) { return exigir(relatorios).vendasPorCategoria(perfil, p); }
    public Map<FormaPagamento, Long> vendasPorFormaPagamento(Perfil perfil, Periodo p) { return exigir(relatorios).vendasPorFormaPagamento(perfil, p); }
    public List<ProdutoVendido> maisVendidos(Perfil perfil, Periodo p, int limite) { return exigir(relatorios).maisVendidos(perfil, p, limite); }
    public ResumoCompras compras(Perfil perfil, Periodo p) { return exigir(relatorios).compras(perfil, p); }
    public ResumoEstoque resumoEstoque(Perfil perfil) { return exigir(relatorios).estoque(perfil); }
    public ResumoFinanceiro resumoFinanceiro(Perfil perfil, Periodo p) { return exigir(relatorios).financeiro(perfil, p); }
}
