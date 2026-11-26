package br.ufpb.aps.grupo12.interfacegrafica;

import br.ufpb.aps.grupo12.dominio.*;
import br.ufpb.aps.grupo12.servicos.ServicoRelatorios.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Relatórios (UC04) com abas. Indicadores e agregações vêm de ServicoRelatorios. */
final class PaginaRelatorios extends Pagina {
    private final JTextField de = Tema.campo(8), ate = Tema.campo(8);
    private final CardLayout abas = new CardLayout();
    private final JPanel conteudo = new JPanel(abas);
    private final Map<String, JToggleButton> botoesAba = new LinkedHashMap<>();
    private String abaAtual = "Vendas";
    // Vendas
    private final CartaoIndicador receita = new CartaoIndicador(Icone.Tipo.CIFRAO, "Receita", "vendas − devoluções", Tema.AZUL);
    private final CartaoIndicador ticket = new CartaoIndicador(Icone.Tipo.SETA_CIMA, "Ticket médio", "por venda", new Color(0x6366F1));
    private final CartaoIndicador lucro = new CartaoIndicador(Icone.Tipo.GRAFICO, "Lucro", "do período", Tema.VERDE);
    private final CartaoIndicador pedidos = new CartaoIndicador(Icone.Tipo.CARRINHO, "Pedidos", "vendas confirmadas", new Color(0x0EA5E9));
    private final Graficos.Barras mensal = new Graficos.Barras();
    private final Graficos.Rosca categorias = new Graficos.Rosca();
    private final Graficos.BarrasHorizontais pagamento = new Graficos.BarrasHorizontais();
    private final JPanel ranking = new JPanel();
    private final JLabel anoMensal = Tema.subtitulo("");
    // Compras
    private final CartaoIndicador totalCompras = new CartaoIndicador(Icone.Tipo.CAMINHAO, "Total comprado", "no período", Tema.AZUL);
    private final CartaoIndicador itensComprados = new CartaoIndicador(Icone.Tipo.CAIXA, "Unidades", "compradas", new Color(0x6366F1));
    private final CartaoIndicador numCompras = new CartaoIndicador(Icone.Tipo.HISTORICO, "Lançamentos", "de compra", new Color(0x0EA5E9));
    private final Graficos.BarrasHorizontais porFornecedor = new Graficos.BarrasHorizontais();
    private final ModeloTabela<Compra> compras = new ModeloTabela<>(
        new ModeloTabela.Coluna<>("Data", c -> c.data().format(Componentes.DATA_HORA)),
        new ModeloTabela.Coluna<>("Lançamento", Compra::id),
        new ModeloTabela.Coluna<>("Fornecedor", Compra::fornecedorId),
        new ModeloTabela.Coluna<>("Produto", Compra::produtoId),
        new ModeloTabela.Coluna<>("Qtd.", Compra::quantidade),
        new ModeloTabela.Coluna<>("Custo un.", c -> Dinheiro.formatar(c.custoUnitarioCentavos())),
        new ModeloTabela.Coluna<>("Total", c -> Dinheiro.formatar(c.totalCentavos())));
    // Estoque
    private final CartaoIndicador prodCount = new CartaoIndicador(Icone.Tipo.CAIXA, "Produtos", "cadastrados", Tema.AZUL);
    private final CartaoIndicador unidades = new CartaoIndicador(Icone.Tipo.PAINEL, "Unidades", "em estoque", new Color(0x6366F1));
    private final CartaoIndicador valorCusto = new CartaoIndicador(Icone.Tipo.CIFRAO, "Valor a custo", "do estoque", new Color(0x0EA5E9));
    private final CartaoIndicador valorVenda = new CartaoIndicador(Icone.Tipo.SETA_CIMA, "Valor de venda", "potencial", Tema.VERDE);
    private final Graficos.BarrasHorizontais porCategoria = new Graficos.BarrasHorizontais();
    private final JTextArea legado = new JTextArea();
    // Financeiro
    private final CartaoIndicador fPagar = new CartaoIndicador(Icone.Tipo.SETA_BAIXO, "A pagar", "em aberto", Tema.ALERTA);
    private final CartaoIndicador fReceber = new CartaoIndicador(Icone.Tipo.SETA_CIMA, "A receber", "em aberto", Tema.VERDE);
    private final CartaoIndicador fVencido = new CartaoIndicador(Icone.Tipo.ALERTA, "Vencido", "contas abertas", new Color(0xB45309));
    private final CartaoIndicador fSaldo = new CartaoIndicador(Icone.Tipo.BANCO, "Saldo previsto", "receber − pagar", Tema.AZUL);
    private final Graficos.BarrasHorizontais fluxo = new Graficos.BarrasHorizontais();

    PaginaRelatorios(Contexto ctx) {
        super(ctx, "Relatórios", "indicadores por período");
        LocalDate hoje = LocalDate.now();
        de.setText(hoje.withDayOfMonth(1).format(Componentes.DATA));
        ate.setText(hoje.format(Componentes.DATA));

        JPanel topo = new JPanel(new BorderLayout(0, 14));
        topo.setOpaque(false);
        JPanel linhaAbas = new JPanel(new BorderLayout());
        linhaAbas.setOpaque(false);
        Cartao abasCartao = new Cartao(new FlowLayout(FlowLayout.LEFT, 4, 0), 6);
        ButtonGroup g = new ButtonGroup();
        Object[][] defs = {{"Vendas", Icone.Tipo.SETA_CIMA}, {"Compras", Icone.Tipo.CAMINHAO}, {"Estoque", Icone.Tipo.CAIXA}, {"Financeiro", Icone.Tipo.BANCO}};
        for (Object[] d : defs) {
            String nome = (String) d[0];
            JToggleButton b = Componentes.alternavel(nome, Tema.AZUL, Color.WHITE);
            b.setBorder(new EmptyBorder(9, 16, 9, 16));
            b.putClientProperty("icone", d[1]);
            b.addActionListener(e -> { abaAtual = nome; estiloAbas(); abas.show(conteudo, nome); Tema.executar(this, this::atualizar, null); });
            g.add(b);
            abasCartao.add(b);
            botoesAba.put(nome, b);
        }
        botoesAba.get("Vendas").setSelected(true);
        estiloAbas();
        linhaAbas.add(abasCartao, BorderLayout.WEST);
        JButton imprimir = Componentes.comIcone(Tema.botaoPrimario("Imprimir / PDF"), Icone.Tipo.IMPRESSORA);
        imprimir.addActionListener(e -> imprimir());
        JPanel bi = new JPanel(new GridBagLayout());
        bi.setOpaque(false);
        bi.add(imprimir);
        linhaAbas.add(bi, BorderLayout.EAST);
        topo.add(linhaAbas, BorderLayout.NORTH);
        Cartao filtro = new Cartao(new FlowLayout(FlowLayout.LEFT, 10, 0), 12);
        filtro.add(Tema.rotulo("Período")); filtro.add(de); filtro.add(Tema.subtitulo("até")); filtro.add(ate);
        JButton filtrar = Componentes.comIcone(Tema.botaoSecundario("Filtrar"), Icone.Tipo.BUSCA);
        filtrar.addActionListener(e -> Tema.executar(this, this::atualizar, null));
        JButton mes = Tema.botaoSecundario("Este mês"), trinta = Tema.botaoSecundario("Últimos 30 dias"), ano = Tema.botaoSecundario("Este ano");
        mes.addActionListener(e -> periodo(hoje.withDayOfMonth(1), hoje));
        trinta.addActionListener(e -> periodo(hoje.minusDays(29), hoje));
        ano.addActionListener(e -> periodo(hoje.withDayOfYear(1), hoje));
        filtro.add(filtrar); filtro.add(mes); filtro.add(trinta); filtro.add(ano);
        topo.add(filtro, BorderLayout.SOUTH);
        add(topo, BorderLayout.NORTH);

        conteudo.setOpaque(false);
        conteudo.add(abaVendas(), "Vendas");
        conteudo.add(abaCompras(), "Compras");
        conteudo.add(abaEstoque(), "Estoque");
        conteudo.add(abaFinanceiro(), "Financeiro");
        add(conteudo, BorderLayout.CENTER);
    }

    private void estiloAbas() {
        botoesAba.values().forEach(b -> {
            boolean s = b.isSelected();
            b.setForeground(s ? Color.WHITE : Tema.TEXTO);
            b.setIcon(new Icone((Icone.Tipo) b.getClientProperty("icone"), 16, s ? Color.WHITE : Tema.TEXTO_SUAVE));
        });
    }

    private void periodo(LocalDate i, LocalDate f) {
        de.setText(i.format(Componentes.DATA));
        ate.setText(f.format(Componentes.DATA));
        Tema.executar(this, this::atualizar, null);
    }

    private static JPanel coluna(JComponent... partes) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        for (JComponent c : partes) { c.setAlignmentX(Component.LEFT_ALIGNMENT); p.add(c); p.add(Box.createVerticalStrut(16)); }
        return p;
    }
    private static Cartao bloco(String titulo, JComponent direita, JComponent corpo, int altura) {
        Cartao c = new Cartao(new BorderLayout());
        c.add(Componentes.cabecalho(titulo, direita), BorderLayout.NORTH);
        c.add(corpo, BorderLayout.CENTER);
        c.setPreferredSize(new Dimension(400, altura));
        return c;
    }
    private static JPanel par(JComponent a, JComponent b) { return grade(2, 16, a, b); }

    private JComponent abaVendas() {
        JButton meta = Componentes.comIcone(Tema.botaoSecundario("Definir meta"), Icone.Tipo.SETA_CIMA);
        meta.addActionListener(e -> definirMeta());
        JPanel cabMensal = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        cabMensal.setOpaque(false);
        cabMensal.add(anoMensal); cabMensal.add(meta);
        ranking.setOpaque(false);
        ranking.setLayout(new BoxLayout(ranking, BoxLayout.Y_AXIS));
        return coluna(grade(4, 16, receita, ticket, lucro, pedidos),
            par(bloco("Vendas mensais vs. meta", cabMensal, mensal, 330), bloco("Vendas por categoria", null, categorias, 330)),
            par(bloco("Vendas por forma de pagamento", null, pagamento, 280), bloco("Produtos mais vendidos", null, ranking, 280)));
    }
    private JComponent abaCompras() {
        JScrollPane t = Tema.tabela(new JTable(compras));
        return coluna(grade(3, 16, totalCompras, itensComprados, numCompras),
            bloco("Compras por fornecedor", null, porFornecedor, 170), bloco("Lançamentos de compra", null, t, 360));
    }
    private JComponent abaEstoque() {
        legado.setEditable(false);
        legado.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        legado.setBorder(new EmptyBorder(8, 8, 8, 8));
        JButton copiar = Tema.botaoSecundario("Copiar CSV");
        copiar.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(legado.getText()), null));
        JScrollPane sl = new JScrollPane(legado);
        sl.setBorder(BorderFactory.createLineBorder(Tema.BORDA));
        return coluna(grade(4, 16, prodCount, unidades, valorCusto, valorVenda),
            par(bloco("Unidades por categoria", null, porCategoria, 300),
                bloco("Exportação p/ sistema legado (Adapter)", copiar, sl, 300)));
    }
    private JComponent abaFinanceiro() {
        return coluna(grade(4, 16, fPagar, fReceber, fVencido, fSaldo), bloco("Fluxo no período (contas quitadas)", null, fluxo, 160));
    }

    private Periodo periodo() {
        return new Periodo(Componentes.lerData(de.getText(), "Data inicial"), Componentes.lerData(ate.getText(), "Data final"));
    }

    @Override void atualizar() {
        Periodo p = periodo();
        switch (abaAtual) {
            case "Compras" -> {
                ResumoCompras r = ctx.facade().compras(ctx.perfil(), p);
                totalCompras.valor(Dinheiro.formatar(r.total()));
                itensComprados.valor(String.valueOf(r.quantidadeItens()));
                numCompras.valor(String.valueOf(r.compras().size()));
                porFornecedor.definir(r.porFornecedor(), true);
                compras.definir(r.compras());
            }
            case "Estoque" -> {
                ResumoEstoque r = ctx.facade().resumoEstoque(ctx.perfil());
                prodCount.valor(String.valueOf(r.produtos()));
                prodCount.rodape(r.emAlerta().size() + " em estoque baixo");
                unidades.valor(String.valueOf(r.unidades()));
                valorCusto.valor(Dinheiro.formatar(r.valorCusto()));
                valorVenda.valor(Dinheiro.formatar(r.valorVenda()), Tema.VERDE);
                Map<String, Long> m = new LinkedHashMap<>();
                r.unidadesPorCategoria().forEach((k, v) -> m.put(k, (long) v));
                porCategoria.definir(m, false);
                legado.setText(ctx.facade().relatorio(ctx.perfil()));
            }
            case "Financeiro" -> {
                ResumoFinanceiro r = ctx.facade().resumoFinanceiro(ctx.perfil(), p);
                fPagar.valor(Dinheiro.formatar(r.aPagarAberto()), Tema.ALERTA);
                fReceber.valor(Dinheiro.formatar(r.aReceberAberto()), Tema.VERDE);
                fVencido.valor(Dinheiro.formatar(r.vencido()));
                fSaldo.valor(Dinheiro.formatar(r.saldoPrevisto()), r.saldoPrevisto() < 0 ? Tema.ALERTA : Tema.AZUL_ESCURO);
                Map<String, Long> m = new LinkedHashMap<>();
                m.put("Recebido", r.recebidoNoPeriodo());
                m.put("Pago", r.pagoNoPeriodo());
                fluxo.definir(m, true);
            }
            default -> atualizarVendas(p);
        }
    }

    private void atualizarVendas(Periodo p) {
        Resumo r = ctx.facade().resumo(ctx.perfil(), p);
        receita.valor(Dinheiro.formatar(r.faturamento()));
        receita.rodape(r.reembolsos() > 0 ? "inclui −" + Dinheiro.formatar(r.reembolsos()) + " em devoluções" : "sem devoluções");
        ticket.valor(Dinheiro.formatar(r.ticketMedio()));
        lucro.valor(Dinheiro.formatar(r.lucro()), Tema.VERDE);
        lucro.rodape(r.margemPercentual().toPlainString().replace('.', ',') + "% de margem");
        pedidos.valor(String.valueOf(r.pedidos()));
        int ano = p.fim().getYear();
        anoMensal.setText(String.valueOf(ano));
        long[] meses = ctx.facade().vendasPorMes(ctx.perfil(), ano);
        List<String> rot = new ArrayList<>();
        List<Long> val = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            rot.add(Componentes.capitalizar(Month.of(i + 1).getDisplayName(TextStyle.SHORT, Componentes.BR).replace(".", "")));
            val.add(meses[i]);
        }
        mensal.definir(rot, val, ctx.facade().metaMensal(ctx.perfil()));
        categorias.definir(ctx.facade().vendasPorCategoria(ctx.perfil(), p));
        Map<String, Long> pg = new LinkedHashMap<>();
        ctx.facade().vendasPorFormaPagamento(ctx.perfil(), p).forEach((k, v) -> pg.put(k.rotulo(), v));
        pagamento.definir(pg, true);
        ranking.removeAll();
        List<ProdutoVendido> top = ctx.facade().maisVendidos(ctx.perfil(), p, 5);
        if (top.isEmpty()) ranking.add(Tema.subtitulo("Nenhuma venda registrada no período."));
        int pos = 1;
        for (ProdutoVendido pv : top) {
            JPanel l = new JPanel(new BorderLayout(10, 0));
            l.setOpaque(false);
            l.setBorder(new EmptyBorder(6, 0, 6, 0));
            l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            JLabel n = new JLabel(String.valueOf(pos++), SwingConstants.CENTER);
            n.setFont(Tema.titulo(14));
            n.setForeground(Color.WHITE);
            n.setOpaque(true);
            n.setBackground(pos == 2 ? Tema.AZUL : new Color(0x94A3B8));
            n.setPreferredSize(new Dimension(28, 28));
            l.add(n, BorderLayout.WEST);
            JLabel nome = new JLabel("<html><b>" + pv.nome() + "</b><br><font color='#64748B'>" + pv.quantidade() + " unidades</font></html>");
            nome.setFont(Tema.sans(12, Font.PLAIN));
            l.add(nome, BorderLayout.CENTER);
            JLabel v = new JLabel(Dinheiro.formatar(pv.receita()));
            v.setFont(Tema.sans(13, Font.BOLD));
            l.add(v, BorderLayout.EAST);
            ranking.add(l);
        }
        ranking.revalidate();
        ranking.repaint();
    }

    private void definirMeta() {
        JTextField v = Tema.campo(10);
        v.setText(Dinheiro.formatar(ctx.facade().metaMensal(ctx.perfil())).replace("R$ ", ""));
        if (!Formulario.mostrar(this, "Meta mensal de faturamento", new String[]{"Meta (R$)"}, new JComponent[]{v})) return;
        executar(() -> ctx.facade().definirMeta(ctx.perfil(), Dinheiro.lerReais(v.getText())), "Meta atualizada.");
    }

    /** Imprime a aba atual (no Windows, escolha "Microsoft Print to PDF" para salvar em PDF). */
    private void imprimir() {
        Component alvo = conteudo;
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("StockFlow — Relatório de " + abaAtual);
        job.setPrintable((Graphics g, PageFormat pf, int pagina) -> {
            if (pagina > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) g;
            g2.translate(pf.getImageableX(), pf.getImageableY());
            double esc = Math.min(pf.getImageableWidth() / alvo.getWidth(), pf.getImageableHeight() / (alvo.getHeight() + 40.0));
            g2.setFont(Tema.titulo(14));
            g2.drawString("StockFlow — Relatório de " + abaAtual + " (" + de.getText() + " a " + ate.getText() + ")", 0, 14);
            g2.translate(0, 26);
            g2.scale(esc, esc);
            alvo.printAll(g2);
            return Printable.PAGE_EXISTS;
        });
        if (!job.printDialog()) return;
        try {
            job.print();
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(this, "Não foi possível imprimir: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
