package br.ufpb.aps.grupo12.persistencia;

import br.ufpb.aps.grupo12.dominio.*;
import br.ufpb.aps.grupo12.seguranca.Usuario;
import java.util.LinkedHashMap;
import java.util.Map;

/** Snapshot de trabalho: transações alteram somente uma cópia até o commit. */
public final class EstadoDados {
    public final Map<String, Produto> produtos = new LinkedHashMap<>();
    public final Map<String, Venda> vendas = new LinkedHashMap<>();
    public final Map<String, Devolucao> devolucoes = new LinkedHashMap<>();
    public final Map<String, Movimentacao> movimentacoes = new LinkedHashMap<>();
    public final Map<String, Categoria> categorias = new LinkedHashMap<>();
    public final Map<String, Cliente> clientes = new LinkedHashMap<>();
    public final Map<String, Fornecedor> fornecedores = new LinkedHashMap<>();
    public final Map<String, Compra> compras = new LinkedHashMap<>();
    public final Map<String, ContaFinanceira> contas = new LinkedHashMap<>();
    public final Map<String, Usuario> usuarios = new LinkedHashMap<>();
    /** Configurações simples (ex.: meta de faturamento do mês). */
    public final Map<String, String> config = new LinkedHashMap<>();

    public EstadoDados copiar() {
        EstadoDados novo = new EstadoDados();
        novo.produtos.putAll(produtos);
        novo.vendas.putAll(vendas);
        novo.devolucoes.putAll(devolucoes);
        novo.movimentacoes.putAll(movimentacoes);
        novo.categorias.putAll(categorias);
        novo.clientes.putAll(clientes);
        novo.fornecedores.putAll(fornecedores);
        novo.compras.putAll(compras);
        novo.contas.putAll(contas);
        novo.usuarios.putAll(usuarios);
        novo.config.putAll(config);
        return novo;
    }
    public boolean mesmoConteudo(EstadoDados o) {
        return produtos.equals(o.produtos) && vendas.equals(o.vendas) && devolucoes.equals(o.devolucoes)
            && movimentacoes.equals(o.movimentacoes) && categorias.equals(o.categorias)
            && clientes.equals(o.clientes) && fornecedores.equals(o.fornecedores) && compras.equals(o.compras)
            && contas.equals(o.contas) && usuarios.equals(o.usuarios) && config.equals(o.config);
    }
    public void limpar() {
        produtos.clear(); vendas.clear(); devolucoes.clear(); movimentacoes.clear(); categorias.clear();
        clientes.clear(); fornecedores.clear(); compras.clear(); contas.clear(); usuarios.clear(); config.clear();
    }
}
