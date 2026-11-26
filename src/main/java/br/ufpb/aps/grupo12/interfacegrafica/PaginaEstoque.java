package br.ufpb.aps.grupo12.interfacegrafica;

import br.ufpb.aps.grupo12.dominio.*;
import java.awt.*;
import javax.swing.*;

/** UC01 — produtos e reposição. A permissão (só gerente) é verificada no serviço. */
final class PaginaEstoque extends Pagina {
    private final ModeloTabela<Produto> modelo = new ModeloTabela<>(
        new ModeloTabela.Coluna<>("Código", Produto::id),
        new ModeloTabela.Coluna<>("Produto", Produto::nome),
        new ModeloTabela.Coluna<>("Preço", p -> Dinheiro.formatar(p.precoCentavos())),
        new ModeloTabela.Coluna<>("Saldo", Produto::estoque),
        new ModeloTabela.Coluna<>("Mínimo", Produto::estoqueMinimo),
        new ModeloTabela.Coluna<>("Situação", p -> p.emAlerta() ? "⚠ Estoque baixo" : "OK"));
    private final JTable tabela = new JTable(modelo);

    PaginaEstoque(Contexto ctx) {
        super(ctx, "Estoque", "Produtos cadastrados, saldos e reposição");
        corpo.add(Tema.tabela(tabela), BorderLayout.CENTER);
    }

    @Override protected JComponent acoesCabecalho() {
        JButton repor = Tema.botaoSecundario("Repor estoque");
        JButton novo = Tema.botaoPrimario("+ Cadastrar produto");
        repor.addActionListener(e -> repor());
        novo.addActionListener(e -> cadastrar());
        return linha(repor, novo);
    }

    @Override void atualizar() { modelo.definir(ctx.facade().estoque(ctx.perfil())); }

    private void cadastrar() {
        JTextField codigo = Tema.campo(14), nome = Tema.campo(20), preco = Tema.campo(10),
            saldo = Tema.campo(6), minimo = Tema.campo(6);
        if (!Formulario.mostrar(this, "Cadastrar produto",
            new String[]{"Código", "Nome", "Preço (R$)", "Estoque inicial", "Estoque mínimo"},
            new JComponent[]{codigo, nome, preco, saldo, minimo})) return;
        Tema.executar(this, () -> ctx.facade().cadastrarProduto(ctx.perfil(), new Produto(
            codigo.getText().trim(), nome.getText().trim(), Dinheiro.lerReais(preco.getText()),
            Tema.inteiro(saldo.getText(), "Estoque inicial"), Tema.inteiro(minimo.getText(), "Estoque mínimo"))),
            "Produto cadastrado.");
        atualizar();
    }

    private void repor() {
        int i = selecionada(tabela);
        JComboBox<String> produto = new JComboBox<>(ctx.facade().estoque(ctx.perfil()).stream()
            .map(Produto::id).toArray(String[]::new));
        Tema.estilizarCampo(produto);
        if (i >= 0) produto.setSelectedItem(modelo.linha(i).id());
        JTextField qtd = Tema.campo(6), lancamento = Tema.campo(14);
        if (!Formulario.mostrar(this, "Repor estoque",
            new String[]{"Produto", "Quantidade", "ID único do lançamento"},
            new JComponent[]{produto, qtd, lancamento})) return;
        Tema.executar(this, () -> {
            Produto p = ctx.facade().repor(ctx.perfil(), (String) produto.getSelectedItem(),
                Tema.inteiro(qtd.getText(), "Quantidade"), lancamento.getText().trim());
            JOptionPane.showMessageDialog(this, p.nome() + ": novo saldo " + p.estoque());
        }, null);
        atualizar();
    }
}
