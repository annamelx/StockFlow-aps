package br.ufpb.aps.grupo12.interfacegrafica;

import br.ufpb.aps.grupo12.dominio.Categoria;
import br.ufpb.aps.grupo12.dominio.Produto;
import java.util.List;
import javax.swing.*;

final class PaginaCategorias extends PaginaListaDetalhe<Categoria> {
    private final JTextField id = Tema.campo(12), nome = Tema.campo(24);
    private List<Produto> produtos = List.of();
    PaginaCategorias(Contexto ctx) { super(ctx, "Categorias", "categoria", "Nova categoria", Icone.Tipo.ETIQUETA); montarFormulario(); }
    @Override List<Categoria> carregar() {
        produtos = ctx.facade().estoque(ctx.perfil());
        return ctx.facade().categorias(ctx.perfil());
    }
    @Override String nome(Categoria c) { return c.nome(); }
    @Override String detalhe(Categoria c) { return c.id(); }
    @Override JComponent etiqueta(Categoria c) {
        long n = produtos.stream().filter(p -> p.categoriaId().equals(c.id())).count();
        return Componentes.pilula(n + " produto(s)", Tema.AZUL_CLARO, Tema.AZUL_ESCURO);
    }
    @Override String[] rotulos() { return new String[]{"Código", "Nome"}; }
    @Override JComponent[] campos() { return new JComponent[]{id, nome}; }
    @Override void preencher(Categoria c) {
        id.setEditable(c == null);
        id.setText(c == null ? "" : c.id());
        nome.setText(c == null ? "" : c.nome());
    }
    @Override void salvar(boolean nova) {
        selecionado = ctx.facade().salvarCategoria(ctx.perfil(), new Categoria(id.getText().trim().toUpperCase(), nome.getText()), nova);
    }
    @Override void excluir(Categoria c) { ctx.facade().excluirCategoria(ctx.perfil(), c.id()); }
}
