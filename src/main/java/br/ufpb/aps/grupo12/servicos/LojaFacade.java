package br.ufpb.aps.grupo12.servicos;

import br.ufpb.aps.grupo12.dominio.*;
import br.ufpb.aps.grupo12.politicas.PoliticaDesconto;
import br.ufpb.aps.grupo12.relatorios.ExportadorRelatorio;
import java.util.*;

/** GoF Facade (estrutural): API amigável usada pela interface. */
public final class LojaFacade {
    private final ServicoLoja servico;
    private final ExportadorRelatorio exportador;
    public LojaFacade(ServicoLoja servico, ExportadorRelatorio exportador) {
        this.servico = Objects.requireNonNull(servico);
        this.exportador = Objects.requireNonNull(exportador);
    }
    public List<Produto> estoque(Perfil perfil) { return servico.consultarProdutos(perfil); }
    public void cadastrarProduto(Perfil perfil, Produto p) { servico.cadastrarProduto(perfil, p); }
    public Produto repor(Perfil perfil, String produto, int qtd, String idLancamento) {
        return servico.reporEstoque(perfil, produto, qtd, idLancamento);
    }
    public Venda abrirVenda(Perfil perfil, String id, Map<String,Integer> itens, PoliticaDesconto politica) {
        return servico.abrirVenda(perfil,id,itens,politica);
    }
    public Venda confirmar(Perfil perfil, String id) { return servico.confirmarVenda(perfil,id); }
    public Venda cancelar(Perfil perfil, String id) { return servico.cancelarVendaPendente(perfil,id); }
    public Devolucao devolver(Perfil perfil, String id, String venda, String produto, int qtd) {
        return servico.devolver(perfil,id,venda,produto,qtd);
    }
    public String relatorio(Perfil perfil) { return servico.exportarRelatorio(perfil, exportador); }
    public List<Movimentacao> historico(Perfil perfil) { return servico.consultarMovimentacoes(perfil); }
    public long receitaLiquida(Perfil perfil) { return servico.consultarReceitaLiquidaCentavos(perfil); }
}
