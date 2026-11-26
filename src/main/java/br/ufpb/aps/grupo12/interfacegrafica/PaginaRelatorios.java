package br.ufpb.aps.grupo12.interfacegrafica;

import br.ufpb.aps.grupo12.dominio.Dinheiro;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** UC04 — relatório legado (Adapter), receita líquida e alertas do Observer. */
final class PaginaRelatorios extends Pagina {
    private final JTextArea csv = new JTextArea();
    private final JTextArea alertas = new JTextArea();
    private final JLabel receita = new JLabel("–");

    PaginaRelatorios(Contexto ctx) {
        super(ctx, "Relatórios", "Exportação no formato do sistema legado (Adapter) e alertas de estoque (Observer)");
        receita.setFont(Tema.titulo(26));
        receita.setForeground(Tema.VERDE);
        JPanel topo = Tema.cartao(new BorderLayout());
        topo.add(receita, BorderLayout.CENTER);
        topo.add(Tema.subtitulo("Receita líquida = vendas confirmadas − reembolsos de devoluções"), BorderLayout.SOUTH);
        corpo.add(topo, BorderLayout.NORTH);
        for (JTextArea a : new JTextArea[]{csv, alertas}) {
            a.setEditable(false);
            a.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            a.setBorder(new EmptyBorder(10, 10, 10, 10));
        }
        JPanel blocos = new JPanel(new GridLayout(1, 2, 14, 0));
        blocos.setOpaque(false);
        JButton copiar = Tema.botaoSecundario("Copiar CSV");
        copiar.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard()
            .setContents(new StringSelection(csv.getText()), null));
        blocos.add(bloco("Exportação p/ sistema legado", csv, copiar));
        blocos.add(bloco("Alertas de estoque mínimo desta sessão", alertas, null));
        corpo.add(blocos, BorderLayout.CENTER);
    }

    private static JPanel bloco(String titulo, JTextArea area, JButton acao) {
        JPanel p = Tema.cartao(new BorderLayout(0, 10));
        JPanel cab = new JPanel(new BorderLayout());
        cab.setOpaque(false);
        JLabel t = new JLabel(titulo);
        t.setFont(Tema.titulo(15));
        cab.add(t, BorderLayout.WEST);
        if (acao != null) cab.add(acao, BorderLayout.EAST);
        p.add(cab, BorderLayout.NORTH);
        JScrollPane s = new JScrollPane(area);
        s.setBorder(BorderFactory.createLineBorder(Tema.BORDA));
        p.add(s, BorderLayout.CENTER);
        return p;
    }

    @Override void atualizar() {
        receita.setText(Dinheiro.formatar(ctx.facade().receitaLiquida(ctx.perfil())));
        csv.setText(ctx.facade().relatorio(ctx.perfil()));
        var lista = ctx.alerta().alertas();
        alertas.setText(lista.isEmpty() ? "Nenhum alerta gerado nesta sessão.\n(O Observer avisa após vendas/devoluções/reposições"
            + "\nque deixam o saldo no mínimo ou abaixo.)" : String.join("\n", lista));
    }
}
