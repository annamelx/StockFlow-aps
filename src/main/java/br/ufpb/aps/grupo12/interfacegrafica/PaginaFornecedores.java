package br.ufpb.aps.grupo12.interfacegrafica;

import br.ufpb.aps.grupo12.dominio.Fornecedor;
import java.util.List;
import javax.swing.*;

final class PaginaFornecedores extends PaginaListaDetalhe<Fornecedor> {
    private final JTextField id = Tema.campo(10), nome = Tema.campo(24), documento = Tema.campo(18), telefone = Tema.campo(16);
    PaginaFornecedores(Contexto ctx) { super(ctx, "Fornecedores", "fornecedor", "Novo fornecedor", Icone.Tipo.CAMINHAO); montarFormulario(); }
    @Override List<Fornecedor> carregar() { return ctx.facade().fornecedores(ctx.perfil()); }
    @Override String nome(Fornecedor f) { return f.nome(); }
    @Override String detalhe(Fornecedor f) { return f.id(); }
    @Override String[] rotulos() { return new String[]{"Código", "Razão social", "CNPJ/CPF", "Telefone"}; }
    @Override JComponent[] campos() { return new JComponent[]{id, nome, documento, telefone}; }
    @Override void preencher(Fornecedor f) {
        id.setEditable(f == null);
        id.setText(f == null ? ctx.facade().proximoId(ctx.perfil(), "FOR") : f.id());
        nome.setText(f == null ? "" : f.nome());
        documento.setText(f == null ? "" : f.documento());
        telefone.setText(f == null ? "" : f.telefone());
    }
    @Override void salvar(boolean novo) {
        selecionado = ctx.facade().salvarFornecedor(ctx.perfil(), new Fornecedor(id.getText().trim(), nome.getText(), documento.getText(), telefone.getText()), novo);
    }
    @Override void excluir(Fornecedor f) { ctx.facade().excluirFornecedor(ctx.perfil(), f.id()); }
}
