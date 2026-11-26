package br.ufpb.aps.grupo12.interfacegrafica;

import br.ufpb.aps.grupo12.dominio.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Visão geral: indicadores calculados pelos serviços (não pela tela). */
final class PaginaPainel extends Pagina {
    private final JLabel produtos = numero(), alertas = numero(), pendentes = numero(), receita = numero();
    private final ModeloTabela<Produto> modeloAlerta = new ModeloTabela<>(
        new ModeloTabela.Coluna<>("Código", Produto::id),
        new ModeloTabela.Coluna<>("Produto", Produto::nome),
        new ModeloTabela.Coluna<>("Saldo", Produto::estoque),
        new ModeloTabela.Coluna<>("Mínimo", Produto::estoqueMinimo));
    private final ModeloTabela<Venda> modeloVendas = new ModeloTabela<>(
        new ModeloTabela.Coluna<>("Venda", Venda::id),
        new ModeloTabela.Coluna<>("Situação", Venda::estado),
        new ModeloTabela.Coluna<>("Total", v -> Dinheiro.formatar(v.totalCentavos())));

    PaginaPainel(Contexto ctx, Consumer<String> navegar) {
        super(ctx, "Olá, " + ctx.usuario().nome(), "Resumo da loja — " + ctx.usuario().rotuloPerfil());
        JPanel cards = new JPanel(new GridLayout(1, 4, 14, 0));
        cards.setOpaque(false);
        cards.add(card("Produtos", produtos, Tema.AZUL_ESCURO));
        cards.add(card("Estoque baixo", alertas, Tema.ALERTA));
        cards.add(card("Vendas pendentes", pendentes, Tema.AZUL_ESCURO));
        cards.add(card("Receita líquida", receita, Tema.VERDE));
        corpo.add(cards, BorderLayout.NORTH);

        JPanel listas = new JPanel(new GridLayout(1, 2, 14, 0));
        listas.setOpaque(false);
        listas.add(bloco("Produtos no estoque mínimo", new JTable(modeloAlerta), "Ver estoque", () -> navegar.accept("Estoque")));
        listas.add(bloco("Últimas vendas", new JTable(modeloVendas), "Nova venda", () -> navegar.accept("Nova venda")));
        corpo.add(listas, BorderLayout.CENTER);
    }

    @Override void atualizar() {
        List<Produto> todos = ctx.facade().estoque(ctx.perfil());
        List<Produto> emAlerta = ctx.facade().emAlerta(ctx.perfil());
        List<Venda> vendas = ctx.facade().vendas(ctx.perfil());
        produtos.setText(String.valueOf(todos.size()));
        alertas.setText(String.valueOf(emAlerta.size()));
        pendentes.setText(String.valueOf(vendas.stream().filter(v -> v.estado() == EstadoVenda.PENDENTE).count()));
        receita.setText(Dinheiro.formatar(ctx.facade().receitaLiquida(ctx.perfil())));
        modeloAlerta.definir(emAlerta);
        modeloVendas.definir(vendas.reversed());
    }

    private static JLabel numero() {
        JLabel l = new JLabel("–");
        l.setFont(Tema.titulo(28));
        return l;
    }

    private static JPanel card(String titulo, JLabel valor, Color cor) {
        JPanel c = Tema.cartao(new BorderLayout(0, 6));
        c.setBorder(new javax.swing.border.CompoundBorder(
            new javax.swing.border.MatteBorder(3, 0, 0, 0, cor), c.getBorder()));
        valor.setForeground(cor);
        JLabel t = Tema.subtitulo(titulo);
        c.add(valor, BorderLayout.CENTER);
        c.add(t, BorderLayout.SOUTH);
        return c;
    }

    private static JPanel bloco(String titulo, JTable tabela, String acao, Runnable aoClicar) {
        JPanel b = Tema.cartao(new BorderLayout(0, 12));
        JPanel topo = new JPanel(new BorderLayout());
        topo.setOpaque(false);
        JLabel t = new JLabel(titulo);
        t.setFont(Tema.titulo(17));
        topo.add(t, BorderLayout.WEST);
        JButton botao = Tema.botaoSecundario(acao);
        botao.addActionListener(e -> aoClicar.run());
        topo.add(botao, BorderLayout.EAST);
        topo.setBorder(new EmptyBorder(0, 0, 4, 0));
        b.add(topo, BorderLayout.NORTH);
        b.add(Tema.tabela(tabela), BorderLayout.CENTER);
        return b;
    }
}
