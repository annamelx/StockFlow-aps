package br.ufpb.aps.grupo12.interfacegrafica;

import br.ufpb.aps.grupo12.dominio.Perfil;
import br.ufpb.aps.grupo12.dominio.Produto;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

/** Janela após o login: menu lateral (conforme o perfil), barra superior e páginas (CardLayout). */
final class JanelaPrincipal extends JFrame {
    private record Item(String chave, String rotulo, Icone.Tipo icone, boolean soGerente, boolean subitem) {}
    private static final List<Item> MENU = List.of(
        new Item("dashboard", "Dashboard", Icone.Tipo.PAINEL, true, false),
        new Item("pdv", "PDV / Caixa", Icone.Tipo.CARRINHO, false, false),
        new Item("historico", "Histórico de Vendas", Icone.Tipo.HISTORICO, false, false),
        new Item("contas", "Contas", Icone.Tipo.CARTEIRA, false, false),
        new Item("cadastrar", "Cadastrar", Icone.Tipo.PASTA, false, false),
        new Item("produtos", "Produtos", Icone.Tipo.CAIXA, true, true),
        new Item("clientes", "Clientes", Icone.Tipo.PESSOA, false, true),
        new Item("fornecedores", "Fornecedores", Icone.Tipo.CAMINHAO, true, true),
        new Item("atendentes", "Atendentes", Icone.Tipo.PESSOAS, true, true),
        new Item("categorias", "Categorias", Icone.Tipo.ETIQUETA, true, true),
        new Item("movimentacoes", "Movimentações", Icone.Tipo.MOVIMENTO, true, false),
        new Item("relatorios", "Relatórios", Icone.Tipo.GRAFICO, true, false));

    private final CardLayout cartoes = new CardLayout();
    private final JPanel paginas = new JPanel(cartoes);
    private final Map<String, Pagina> porChave = new LinkedHashMap<>();
    private final Map<String, JButton> botoes = new LinkedHashMap<>();
    private final JPanel subitens = new JPanel();
    private final JLabel titulo = new JLabel(), data = new JLabel();
    private final JLabel badge = new JLabel();
    private final Contexto ctx;
    private final Runnable aoSair;
    private String atual;

    JanelaPrincipal(Contexto ctx, Runnable aoSair) {
        super("StockFlow — Gestão de Estoque e Vendas");
        this.ctx = ctx;
        this.aoSair = aoSair;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(conteudo());
        setSize(1320, 840);
        setMinimumSize(new Dimension(1180, 720));
        setLocationRelativeTo(null);
    }

    private boolean permitido(Item i) { return !i.soGerente() || ctx.perfil() == Perfil.GERENTE; }

    JPanel conteudo() {
        boolean g = ctx.perfil() == Perfil.GERENTE;
        if (g) registrar("dashboard", new PaginaDashboard(ctx, this::mostrar));
        registrar("pdv", new PaginaPdv(ctx, () -> {}));
        registrar("historico", new PaginaHistorico(ctx));
        registrar("contas", new PaginaContas(ctx));
        registrar("clientes", new PaginaClientes(ctx));
        if (g) {
            registrar("produtos", new PaginaProdutos(ctx));
            registrar("fornecedores", new PaginaFornecedores(ctx));
            registrar("atendentes", new PaginaAtendentes(ctx));
            registrar("categorias", new PaginaCategorias(ctx));
            registrar("movimentacoes", new PaginaMovimentacoes(ctx));
            registrar("relatorios", new PaginaRelatorios(ctx));
        }
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(menu(), BorderLayout.WEST);
        JPanel direita = new JPanel(new BorderLayout());
        direita.add(barraSuperior(), BorderLayout.NORTH);
        paginas.setBackground(Tema.FUNDO);
        direita.add(paginas, BorderLayout.CENTER);
        raiz.add(direita, BorderLayout.CENTER);
        mostrar(g ? "dashboard" : "pdv");
        return raiz;
    }

    private void registrar(String chave, Pagina p) {
        porChave.put(chave, p);
        JComponent conteudo = (p instanceof PaginaDashboard || p instanceof PaginaRelatorios) ? Componentes.rolagem(p) : p;
        paginas.add(conteudo, chave);
    }

    void mostrar(String chave) {
        Pagina p = porChave.get(chave);
        if (p == null) return;
        atual = chave;
        Tema.executar(this, p::atualizar, null);
        cartoes.show(paginas, chave);
        titulo.setText(p.titulo);
        data.setText(Componentes.capitalizar(LocalDate.now().format(Componentes.EXTENSO))
            + (p.subtitulo.isEmpty() ? "" : "  ·  " + p.subtitulo));
        botoes.forEach((k, b) -> marcar(b, k.equals(chave)));
        boolean emCadastro = MENU.stream().anyMatch(i -> i.subitem() && i.chave().equals(chave));
        if (emCadastro) subitens.setVisible(true);
        atualizarBadge();
    }

    private void atualizarBadge() {
        int n = ctx.facade().emAlerta(ctx.perfil()).size();
        badge.setText(n > 0 ? String.valueOf(n) : "");
        badge.setVisible(n > 0);
    }

    // ---------------- menu lateral ----------------
    private JPanel menu() {
        JPanel m = new JPanel(new BorderLayout());
        m.setBackground(Tema.NOITE);
        m.setPreferredSize(new Dimension(250, 0));
        m.setBorder(new MatteBorder(0, 0, 0, 1, new Color(0x1E293B)));
        JPanel topo = new JPanel(new BorderLayout(0, 16));
        topo.setOpaque(false);
        topo.setBorder(new EmptyBorder(22, 18, 14, 18));
        topo.add(Marca.criar(true), BorderLayout.NORTH);
        JLabel chip = Componentes.pilula("●  Perfil " + ctx.usuario().rotuloPerfil(),
            new Color(0x1E293B), new Color(0x93C5FD));
        JPanel chipLinha = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        chipLinha.setOpaque(false);
        chipLinha.add(chip);
        topo.add(chipLinha, BorderLayout.SOUTH);
        m.add(topo, BorderLayout.NORTH);

        JPanel itens = new JPanel();
        itens.setOpaque(false);
        itens.setLayout(new BoxLayout(itens, BoxLayout.Y_AXIS));
        itens.setBorder(new EmptyBorder(6, 12, 6, 12));
        subitens.setOpaque(false);
        subitens.setLayout(new BoxLayout(subitens, BoxLayout.Y_AXIS));
        subitens.setBorder(new CompoundBorder(new EmptyBorder(0, 18, 0, 0), new MatteBorder(0, 1, 0, 0, new Color(0x334155))));
        subitens.setAlignmentX(Component.LEFT_ALIGNMENT);
        subitens.setVisible(false);
        for (Item i : MENU) {
            if (!permitido(i)) continue;
            if (i.subitem()) {
                subitens.add(botaoMenu(i, true));
                continue;
            }
            JButton b = botaoMenu(i, false);
            itens.add(b);
            itens.add(Box.createVerticalStrut(2));
            if (i.chave().equals("cadastrar")) {
                b.addActionListener(e -> { subitens.setVisible(!subitens.isVisible()); itens.revalidate(); b.repaint(); });
                itens.add(subitens);
            }
        }
        m.add(itens, BorderLayout.CENTER);

        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setOpaque(false);
        rodape.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, new Color(0x1E293B)), new EmptyBorder(12, 18, 16, 18)));
        JLabel versao = new JLabel("<html>StockFlow · APS 2026.2<br>UFPB Campus IV · Grupo 12</html>");
        versao.setFont(Tema.sans(10, Font.PLAIN));
        versao.setForeground(new Color(0x64748B));
        rodape.add(versao, BorderLayout.CENTER);
        m.add(rodape, BorderLayout.SOUTH);
        return m;
    }

    private JButton botaoMenu(Item i, boolean sub) {
        Color normal = new Color(0xCBD5E1);
        JButton b = new JButton(i.rotulo()) {
            @Override protected void paintComponent(Graphics g) {
                if (Boolean.TRUE.equals(getClientProperty("ativo"))) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setPaint(new GradientPaint(0, 0, Tema.AZUL, getWidth(), 0, new Color(0x1E40AF)));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                    g2.dispose();
                } else if (getModel().isRollover()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(new Color(0x111C33));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                    g2.dispose();
                }
                super.paintComponent(g);
                if (Boolean.TRUE.equals(getClientProperty("expansivel"))) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(normal);
                    g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int x = getWidth() - 22, y = getHeight() / 2;
                    if (subitens.isVisible()) { g2.drawLine(x, y - 2, x + 4, y + 2); g2.drawLine(x + 4, y + 2, x + 8, y - 2); }
                    else { g2.drawLine(x + 2, y - 4, x + 6, y); g2.drawLine(x + 6, y, x + 2, y + 4); }
                    g2.dispose();
                }
            }
        };
        b.setIcon(new Icone(i.icone(), sub ? 16 : 18, normal));
        b.setIconTextGap(12);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFont(Tema.sans(sub ? 13 : 14, Font.PLAIN));
        b.setForeground(normal);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setRolloverEnabled(true);
        b.setBorder(new EmptyBorder(sub ? 8 : 11, 12, sub ? 8 : 11, 12));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, sub ? 36 : 44));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (!i.chave().equals("cadastrar")) {
            b.addActionListener(e -> mostrar(i.chave()));
            botoes.put(i.chave(), b);
        } else {
            b.putClientProperty("expansivel", true);
        }
        b.putClientProperty("icone", i.icone());
        b.putClientProperty("sub", sub);
        return b;
    }

    private static void marcar(JButton b, boolean ativo) {
        b.putClientProperty("ativo", ativo);
        Color cor = ativo ? Color.WHITE : new Color(0xCBD5E1);
        b.setForeground(cor);
        boolean sub = Boolean.TRUE.equals(b.getClientProperty("sub"));
        b.setIcon(new Icone((Icone.Tipo) b.getClientProperty("icone"), sub ? 16 : 18, cor));
        b.setFont(Tema.sans(sub ? 13 : 14, ativo ? Font.BOLD : Font.PLAIN));
        b.repaint();
    }

    // ---------------- barra superior ----------------
    private JPanel barraSuperior() {
        JPanel b = new JPanel(new BorderLayout());
        b.setBackground(Color.WHITE);
        b.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Tema.BORDA), new EmptyBorder(12, 28, 12, 24)));
        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 2));
        textos.setOpaque(false);
        titulo.setFont(Tema.titulo(19));
        titulo.setForeground(Tema.TEXTO);
        data.setFont(Tema.sans(12, Font.PLAIN));
        data.setForeground(Tema.TEXTO_SUAVE);
        textos.add(titulo); textos.add(data);
        b.add(textos, BorderLayout.WEST);

        JPanel direita = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        direita.setOpaque(false);
        JLayeredPane sino = new JLayeredPane();
        sino.setPreferredSize(new Dimension(34, 34));
        JButton botaoSino = new JButton(new Icone(Icone.Tipo.SINO, 20, Tema.TEXTO));
        botaoSino.setContentAreaFilled(false); botaoSino.setBorderPainted(false); botaoSino.setFocusPainted(false);
        botaoSino.setBounds(0, 4, 30, 30);
        botaoSino.setToolTipText("Alertas de estoque");
        botaoSino.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        botaoSino.addActionListener(e -> mostrarAlertas(botaoSino));
        badge.setOpaque(true);
        badge.setBackground(Tema.ALERTA);
        badge.setForeground(Color.WHITE);
        badge.setFont(Tema.sans(9, Font.BOLD));
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        badge.setBounds(18, 2, 16, 14);
        sino.add(botaoSino, JLayeredPane.DEFAULT_LAYER);
        sino.add(badge, JLayeredPane.PALETTE_LAYER);
        direita.add(sino);
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 30));
        direita.add(sep);

        JPanel usuario = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        usuario.setOpaque(false);
        usuario.add(Componentes.avatar(ctx.usuario().nome(), 34));
        JLabel nome = new JLabel("<html><b>" + ctx.usuario().nome() + "</b><br><font color='#64748B'>"
            + ctx.usuario().rotuloPerfil() + "</font></html>");
        nome.setFont(Tema.sans(13, Font.PLAIN));
        usuario.add(nome);
        usuario.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JPopupMenu menuUsuario = new JPopupMenu();
        JMenuItem sair = new JMenuItem("Sair", new Icone(Icone.Tipo.SAIR, 16, Tema.TEXTO));
        sair.addActionListener(e -> { dispose(); aoSair.run(); });
        menuUsuario.add(sair);
        usuario.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { menuUsuario.show(usuario, 0, usuario.getHeight()); }
        });
        direita.add(usuario);
        b.add(direita, BorderLayout.EAST);
        return b;
    }

    /** Lista de produtos no mínimo + alertas emitidos pelo Observer nesta sessão. */
    private void mostrarAlertas(Component origem) {
        JPopupMenu p = new JPopupMenu();
        p.setBorder(new CompoundBorder(new LineBorder(Tema.BORDA), new EmptyBorder(8, 8, 8, 8)));
        JLabel t = new JLabel("Estoque baixo");
        t.setFont(Tema.titulo(14));
        t.setBorder(new EmptyBorder(2, 6, 8, 6));
        p.add(t);
        List<Produto> lista = ctx.facade().emAlerta(ctx.perfil());
        if (lista.isEmpty()) p.add(new JLabel("  Nenhum produto no estoque mínimo."));
        for (Produto pr : lista) {
            JMenuItem it = new JMenuItem(pr.nome() + " — " + (pr.estoque() == 0 ? "esgotado" : pr.estoque() + " un (mín. " + pr.estoqueMinimo() + ")"),
                new Icone(Icone.Tipo.ALERTA, 16, Tema.ALERTA));
            it.addActionListener(e -> { if (ctx.perfil() == Perfil.GERENTE) mostrar("produtos"); });
            p.add(it);
        }
        List<String> sessao = ctx.alerta().alertas();
        if (!sessao.isEmpty()) {
            p.addSeparator();
            JLabel s = new JLabel("  Avisos desta sessão (Observer)");
            s.setFont(Tema.sans(11, Font.BOLD));
            s.setForeground(Tema.TEXTO_SUAVE);
            p.add(s);
            sessao.stream().skip(Math.max(0, sessao.size() - 5)).forEach(a -> p.add(new JLabel("  " + a)));
        }
        p.show(origem, origem.getWidth() - 320, origem.getHeight());
    }

    String paginaAtual() { return atual; }
}
