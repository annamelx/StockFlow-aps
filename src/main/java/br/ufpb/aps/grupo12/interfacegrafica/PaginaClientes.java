package br.ufpb.aps.grupo12.interfacegrafica;

import br.ufpb.aps.grupo12.dominio.*;
import java.util.List;
import javax.swing.*;

final class PaginaClientes extends PaginaListaDetalhe<Cliente> {
    private final JTextField id = Tema.campo(10), nome = Tema.campo(24), telefone = Tema.campo(16), email = Tema.campo(24);
    private final JLabel resumo = Tema.subtitulo(" ");

    PaginaClientes(Contexto ctx) {
        super(ctx, "Clientes", "cliente", "Novo cliente");
        extras.add(resumo, java.awt.BorderLayout.NORTH);
        montarFormulario();
    }
    @Override List<Cliente> carregar() { return ctx.facade().clientes(ctx.perfil()); }
    @Override String nome(Cliente c) { return c.nome(); }
    @Override String detalhe(Cliente c) { return c.id(); }
    @Override String[] rotulos() { return new String[]{"Código", "Nome", "Telefone", "E-mail"}; }
    @Override JComponent[] campos() { return new JComponent[]{id, nome, telefone, email}; }
    @Override void preencher(Cliente c) {
        id.setEditable(c == null);
        id.setText(c == null ? ctx.facade().proximoId(ctx.perfil(), "CLI") : c.id());
        nome.setText(c == null ? "" : c.nome());
        telefone.setText(c == null ? "" : c.telefone());
        email.setText(c == null ? "" : c.email());
        if (c == null) { resumo.setText(" "); return; }
        var compras = ctx.facade().vendas(ctx.perfil()).stream()
            .filter(v -> v.clienteId().equals(c.id()) && v.estado() == EstadoVenda.CONFIRMADA).toList();
        resumo.setText("<html><br><b>" + compras.size() + " compra(s) confirmada(s)</b> · total "
            + Dinheiro.formatar(compras.stream().mapToLong(Venda::totalCentavos).sum())
            + (compras.isEmpty() ? "" : " · última em " + compras.getLast().criadaEm().format(Componentes.DATA)) + "</html>");
    }
    @Override void salvar(boolean novo) {
        Cliente c = ctx.facade().salvarCliente(ctx.perfil(), new Cliente(id.getText().trim(), nome.getText(), telefone.getText(), email.getText()), novo);
        selecionado = c;
    }
    @Override void excluir(Cliente c) { ctx.facade().excluirCliente(ctx.perfil(), c.id()); }
}
