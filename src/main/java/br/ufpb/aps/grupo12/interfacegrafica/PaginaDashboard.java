package br.ufpb.aps.grupo12.interfacegrafica;

import br.ufpb.aps.grupo12.dominio.*;
import br.ufpb.aps.grupo12.servicos.ServicoRelatorios.Painel;
import java.awt.*;
import java.awt.geom.Point2D;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.*;

/** Dashboard da gerente. Todos os números vêm de ServicoRelatorios.painel(). */
final class PaginaDashboard extends Pagina {
    private final CartaoIndicador fatHoje = new CartaoIndicador(Icone.Tipo.CIFRAO, "Hoje", "Faturamento", Tema.AZUL);
    private final CartaoIndicador lucroHoje = new CartaoIndicador(Icone.Tipo.SETA_CIMA, "Hoje", "Lucro", Tema.VERDE);
    private final CartaoIndicador lucroMes = new CartaoIndicador(Icone.Tipo.GRAFICO, "Mês atual", "Lucro", new Color(0x6366F1));
    private final JLabel fatMes = new JLabel(), mesNome = new JLabel(), pct = new JLabel(), metaTxt = new JLabel();
    private final Graficos.Progresso barraMeta = new Graficos.Progresso(new Color(255, 255, 255, 40), Tema.CIANO);
    private final Graficos.Linha semana = new Graficos.Linha();
    private final JPanel listaAlerta = new JPanel();
    private final ModeloTabela<Movimentacao> movs = new ModeloTabela<>(
        new ModeloTabela.Coluna<>("Data", m -> m.data().format(Componentes.DATA_HORA)),
        new ModeloTabela.Coluna<>("Tipo", m -> switch (m.tipo()) {
            case ENTRADA_REPOSICAO -> "↑ Compra/reposição";
            case SAIDA_VENDA -> "↓ Venda";
            case ENTRADA_DEVOLUCAO -> "↑ Devolução";
        }),
        new ModeloTabela.Coluna<>("Produto", Movimentacao::produtoId),
        new ModeloTabela.Coluna<>("Qtd.", Movimentacao::quantidade),
        new ModeloTabela.Coluna<>("Operação", Movimentacao::operacaoId));

    PaginaDashboard(Contexto ctx, Consumer<String> navegar) {
        super(ctx, "Dashboard", "visão geral da loja");
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        col.add(alinhar(Componentes.secao("Atual")));
        col.add(Box.createVerticalStrut(10));
        lucroHoje.rodape("Lucro líquido do dia corrente");
        lucroMes.rodape("Lucro acumulado do mês atual");
        col.add(alinhar(grade(3, 16, fatHoje, lucroHoje, lucroMes)));
        col.add(Box.createVerticalStrut(16));
        col.add(alinhar(cartaoMes()));
        col.add(Box.createVerticalStrut(16));

        Cartao graf = new Cartao(new BorderLayout());
        graf.add(Componentes.cabecalho("Vendas da semana", Tema.subtitulo("últimos 7 dias · vendas confirmadas")), BorderLayout.NORTH);
        graf.add(semana, BorderLayout.CENTER);
        Cartao alerta = new Cartao(new BorderLayout());
        JLabel ta = Componentes.tituloSecao("Estoque baixo");
        ta.setIcon(new Icone(Icone.Tipo.ALERTA, 18, Tema.ALERTA));
        ta.setIconTextGap(8);
        JPanel cab = new JPanel(new BorderLayout());
        cab.setOpaque(false);
        cab.setBorder(new EmptyBorder(0, 0, 10, 0));
        cab.add(ta, BorderLayout.WEST);
        alerta.add(cab, BorderLayout.NORTH);
        listaAlerta.setOpaque(false);
        listaAlerta.setLayout(new BoxLayout(listaAlerta, BoxLayout.Y_AXIS));
        alerta.add(listaAlerta, BorderLayout.CENTER);
        alerta.add(Componentes.link("Ver produtos →", () -> navegar.accept("produtos")), BorderLayout.SOUTH);
        JPanel meio = new JPanel(new GridBagLayout());
        meio.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH; c.weighty = 1;
        c.weightx = 0.64; c.insets = new Insets(0, 0, 0, 16); meio.add(graf, c);
        c.weightx = 0.36; c.insets = new Insets(0, 0, 0, 0); meio.add(alerta, c);
        meio.setPreferredSize(new Dimension(800, 290));
        col.add(alinhar(meio));
        col.add(Box.createVerticalStrut(16));

        Cartao mov = new Cartao(new BorderLayout());
        mov.add(Componentes.cabecalho("Movimentações recentes", Componentes.link("Ver todas →", () -> navegar.accept("movimentacoes"))), BorderLayout.NORTH);
        JTable t = new JTable(movs);
        JScrollPane st = Tema.tabela(t);
        st.setPreferredSize(new Dimension(400, 8 * 32 + 36));
        mov.add(st, BorderLayout.CENTER);
        col.add(alinhar(mov));
        add(col, BorderLayout.CENTER);
    }

    private static JComponent alinhar(JComponent c) { c.setAlignmentX(Component.LEFT_ALIGNMENT); return c; }

    /** Cartão escuro grande: faturamento do mês e progresso da meta. */
    private JComponent cartaoMes() {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, Tema.NOITE, getWidth(), getHeight(), new Color(0x1E3A8A)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setPaint(new RadialGradientPaint(new Point2D.Float(getWidth() * 0.82f, 0), getWidth() * 0.4f,
                    new float[]{0, 1}, new Color[]{new Color(56, 189, 248, 70), new Color(56, 189, 248, 0)}));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(new Color(255, 255, 255, 14));
                g2.fillOval(getWidth() / 2 - 40, getHeight() - 70, 160, 160);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(22, 26, 22, 26));
        p.setPreferredSize(new Dimension(800, 150));
        JPanel esq = new JPanel(new BorderLayout(0, 8));
        esq.setOpaque(false);
        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topo.setOpaque(false);
        topo.add(Componentes.selo(Icone.Tipo.BANCO, new Color(255, 255, 255, 30)));
        mesNome.setFont(Tema.sans(12, Font.PLAIN));
        mesNome.setForeground(new Color(0xBFDBFE));
        topo.add(mesNome);
        esq.add(topo, BorderLayout.NORTH);
        fatMes.setFont(Tema.titulo(38));
        fatMes.setForeground(Color.WHITE);
        esq.add(fatMes, BorderLayout.CENTER);
        p.add(esq, BorderLayout.WEST);
        JPanel dir = new JPanel();
        dir.setOpaque(false);
        dir.setLayout(new BoxLayout(dir, BoxLayout.Y_AXIS));
        JLabel r = new JLabel("PROGRESSO DA META");
        r.setFont(Tema.sans(11, Font.BOLD));
        r.setForeground(new Color(0x93C5FD));
        pct.setFont(Tema.titulo(28));
        pct.setForeground(Color.WHITE);
        metaTxt.setFont(Tema.sans(11, Font.PLAIN));
        metaTxt.setForeground(new Color(0xBFDBFE));
        barraMeta.setPreferredSize(new Dimension(240, 8));
        barraMeta.setMaximumSize(new Dimension(240, 8));
        for (JComponent c : new JComponent[]{r, pct, barraMeta, metaTxt}) c.setAlignmentX(Component.RIGHT_ALIGNMENT);
        dir.add(r); dir.add(Box.createVerticalStrut(6)); dir.add(pct); dir.add(Box.createVerticalStrut(8));
        dir.add(barraMeta); dir.add(Box.createVerticalStrut(6)); dir.add(metaTxt);
        p.add(dir, BorderLayout.EAST);
        return p;
    }

    @Override void atualizar() {
        Painel p = ctx.facade().painel(ctx.perfil());
        fatHoje.valor(Dinheiro.formatar(p.hojeResumo().faturamento()));
        fatHoje.rodape(p.hojeResumo().pedidos() + " venda(s) confirmada(s) hoje");
        lucroHoje.valor(Dinheiro.formatar(p.hojeResumo().lucro()), Tema.VERDE);
        lucroMes.valor(Dinheiro.formatar(p.mesResumo().lucro()), new Color(0x4F46E5));
        mesNome.setText("<html><b style='color:#93C5FD'>MÊS ATUAL</b><br>Faturamento — "
            + p.hoje().getMonth().getDisplayName(TextStyle.FULL, Componentes.BR) + " de " + p.hoje().getYear() + "</html>");
        fatMes.setText(Dinheiro.formatar(p.mesResumo().faturamento()));
        pct.setText(p.progressoMeta().stripTrailingZeros().toPlainString().replace('.', ',') + "%");
        barraMeta.definir(p.progressoMeta().doubleValue());
        metaTxt.setText("Meta: " + Dinheiro.formatar(p.meta()) + "  ·  " + p.mesResumo().pedidos() + " vendas no mês");
        List<String> dias = new ArrayList<>();
        p.vendasSemana().keySet().forEach(d -> dias.add(Componentes.capitalizar(
            d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Componentes.BR).replace(".", ""))));
        semana.definir(dias, new ArrayList<>(p.vendasSemana().values()));
        listaAlerta.removeAll();
        if (p.estoqueBaixo().isEmpty()) listaAlerta.add(Tema.subtitulo("Nenhum produto no mínimo. 🎉"));
        for (Produto pr : p.estoqueBaixo()) {
            JPanel l = new JPanel(new BorderLayout(10, 0));
            l.setOpaque(false);
            l.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Tema.BORDA), new EmptyBorder(8, 0, 8, 0)));
            l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
            l.add(Componentes.avatar(pr.nome(), 34), BorderLayout.WEST);
            JLabel nome = new JLabel("<html><b>" + pr.nome() + "</b><br><font color='#64748B'>" + pr.id()
                + " · mín. " + pr.estoqueMinimo() + "</font></html>");
            nome.setFont(Tema.sans(12, Font.PLAIN));
            l.add(nome, BorderLayout.CENTER);
            l.add(pr.estoque() == 0 ? Componentes.pilula("Esgotado", new Color(0xFEE2E2), Tema.ALERTA)
                : Componentes.pilula(pr.estoque() + " un", new Color(0xFEF3C7), new Color(0xB45309)), BorderLayout.EAST);
            listaAlerta.add(l);
        }
        listaAlerta.revalidate();
        listaAlerta.repaint();
        movs.definir(p.recentes());
    }
}
