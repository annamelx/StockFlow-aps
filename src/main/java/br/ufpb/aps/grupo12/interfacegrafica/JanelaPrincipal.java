package br.ufpb.aps.grupo12.interfacegrafica;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.*;

/** Janela após o login: menu lateral e páginas (CardLayout). */
final class JanelaPrincipal extends JFrame {
    private final CardLayout cartoes = new CardLayout();
    private final JPanel paginas = new JPanel(cartoes);
    private final Map<String, Pagina> porNome = new LinkedHashMap<>();
    private final List<JButton> botoesMenu = new ArrayList<>();
    private final Contexto ctx;
    private final Runnable aoSair;

    JanelaPrincipal(Contexto ctx, Runnable aoSair) {
        super("StockFlow — Gestão de Estoque e Vendas");
        this.ctx = ctx;
        this.aoSair = aoSair;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(conteudo());
        setSize(1240, 780);
        setMinimumSize(new Dimension(1080, 680));
        setLocationRelativeTo(null);
    }

    JPanel conteudo() {
        registrar("Painel", new PaginaPainel(ctx, this::mostrar));
        registrar("Estoque", new PaginaEstoque(ctx));
        registrar("Nova venda", new PaginaNovaVenda(ctx, () -> mostrar("Vendas")));
        registrar("Vendas", new PaginaVendas(ctx));
        registrar("Devoluções", new PaginaDevolucoes(ctx));
        registrar("Movimentações", new PaginaMovimentacoes(ctx));
        registrar("Relatórios", new PaginaRelatorios(ctx));
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(menu(), BorderLayout.WEST);
        raiz.add(paginas, BorderLayout.CENTER);
        mostrar("Painel");
        return raiz;
    }

    private void registrar(String nome, Pagina p) { porNome.put(nome, p); paginas.add(p, nome); }

    void mostrar(String nome) {
        Pagina p = porNome.get(nome);
        if (p == null) return;
        Tema.executar(this, p::atualizar, null);
        cartoes.show(paginas, nome);
        for (JButton b : botoesMenu) marcar(b, b.getText().trim().equals(nome));
    }

    private JPanel menu() {
        FundoGradiente m = new FundoGradiente(new BorderLayout());
        m.setPreferredSize(new Dimension(240, 0));
        m.setBorder(new EmptyBorder(24, 16, 20, 16));
        m.add(Marca.criar(true), BorderLayout.NORTH);
        JPanel itens = new JPanel();
        itens.setOpaque(false);
        itens.setLayout(new BoxLayout(itens, BoxLayout.Y_AXIS));
        itens.add(Box.createVerticalStrut(28));
        for (String nome : porNome.keySet()) {
            JButton b = new JButton("  " + nome);
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setFont(Tema.sans(14, Font.PLAIN));
            b.setForeground(new Color(0xCBD5E1));
            b.setContentAreaFilled(false);
            b.setOpaque(false);
            b.setFocusPainted(false);
            b.setBorder(new EmptyBorder(10, 12, 10, 12));
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(e -> mostrar(nome));
            botoesMenu.add(b);
            itens.add(b);
            itens.add(Box.createVerticalStrut(4));
        }
        m.add(itens, BorderLayout.CENTER);

        JPanel usuario = new JPanel(new BorderLayout(0, 10));
        usuario.setOpaque(false);
        JLabel nome = new JLabel("<html><b>" + ctx.usuario().nome() + "</b><br><span style='font-size:9px'>"
            + ctx.usuario().rotuloPerfil() + "</span></html>");
        nome.setFont(Tema.sans(13, Font.PLAIN));
        nome.setForeground(Color.WHITE);
        JButton sair = new JButton("Sair");
        sair.setFont(Tema.sans(12, Font.BOLD));
        sair.setForeground(Color.WHITE);
        sair.setContentAreaFilled(false);
        sair.setFocusPainted(false);
        sair.setBorder(new CompoundBorder(new LineBorder(new Color(0x334155), 1, true),
            new EmptyBorder(6, 12, 6, 12)));
        sair.addActionListener(e -> { dispose(); aoSair.run(); });
        usuario.add(nome, BorderLayout.CENTER);
        usuario.add(sair, BorderLayout.SOUTH);
        m.add(usuario, BorderLayout.SOUTH);
        return m;
    }

    private static void marcar(JButton b, boolean ativo) {
        b.setOpaque(ativo);
        b.setContentAreaFilled(ativo);
        b.setBackground(ativo ? Tema.AZUL : null);
        b.setForeground(ativo ? Color.WHITE : new Color(0xCBD5E1));
        b.setFont(Tema.sans(14, ativo ? Font.BOLD : Font.PLAIN));
        b.repaint();
    }
}
