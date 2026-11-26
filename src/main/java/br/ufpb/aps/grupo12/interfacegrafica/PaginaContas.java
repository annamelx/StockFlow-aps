package br.ufpb.aps.grupo12.interfacegrafica;

import br.ufpb.aps.grupo12.dominio.*;
import br.ufpb.aps.grupo12.servicos.ServicoFinanceiro.Totais;
import java.awt.*;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;

/** Contas a pagar e a receber. Lançar, quitar e cancelar: só gerente (verificado no serviço). */
final class PaginaContas extends Pagina {
    private final CartaoIndicador pagar = new CartaoIndicador(Icone.Tipo.SETA_BAIXO, "A pagar", "em aberto", Tema.ALERTA);
    private final CartaoIndicador receber = new CartaoIndicador(Icone.Tipo.SETA_CIMA, "A receber", "em aberto", Tema.VERDE);
    private final CartaoIndicador vencido = new CartaoIndicador(Icone.Tipo.ALERTA, "Vencidas", "abertas e vencidas", new Color(0xB45309));
    private final JComboBox<String> filtro = new JComboBox<>(new String[]{"Todas", "A pagar", "A receber", "Em aberto"});
    private final ModeloTabela<ContaFinanceira> modelo;
    private final JTable tabela;
    private List<ContaFinanceira> todas = List.of();

    PaginaContas(Contexto ctx) {
        super(ctx, "Contas", "a pagar e a receber");
        LocalDate hoje = LocalDate.now();
        modelo = new ModeloTabela<>(
            new ModeloTabela.Coluna<>("ID", ContaFinanceira::id),
            new ModeloTabela.Coluna<>("Tipo", ContaFinanceira::tipo),
            new ModeloTabela.Coluna<>("Descrição", ContaFinanceira::descricao),
            new ModeloTabela.Coluna<>("Vencimento", c -> c.vencimento().format(Componentes.DATA)),
            new ModeloTabela.Coluna<>("Valor", c -> Dinheiro.formatar(c.valorCentavos())),
            new ModeloTabela.Coluna<>("Situação", c -> c.vencida(hoje) ? "VENCIDA" : c.situacao()
                + (c.quitadaEm() != null ? " em " + c.quitadaEm().format(Componentes.DATA) : "")));
        tabela = new JTable(modelo);
        Tema.estilizarCampo(filtro);
        filtro.addActionListener(e -> filtrar());
        JPanel topo = new JPanel(new BorderLayout(0, 16));
        topo.setOpaque(false);
        topo.add(grade(3, 16, pagar, receber, vencido), BorderLayout.NORTH);
        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);
        JPanel esq = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        esq.setOpaque(false);
        esq.add(Tema.rotulo("Mostrar")); esq.add(filtro);
        barra.add(esq, BorderLayout.WEST);
        JButton nova = Componentes.comIcone(Tema.botaoPrimario("Nova conta"), Icone.Tipo.MAIS);
        JButton quitar = Tema.botaoSecundario("Quitar"), cancelar = Tema.botaoPerigo("Cancelar");
        nova.addActionListener(e -> nova());
        quitar.addActionListener(e -> agir(true));
        cancelar.addActionListener(e -> agir(false));
        barra.add(linha(cancelar, quitar, nova), BorderLayout.EAST);
        topo.add(barra, BorderLayout.SOUTH);
        add(topo, BorderLayout.NORTH);
        add(Tema.tabela(tabela), BorderLayout.CENTER);
        if (ctx.perfil() != Perfil.GERENTE)
            add(Tema.subtitulo("Perfil atendente: somente consulta. Lançar, quitar e cancelar são permitidos apenas à gerente."), BorderLayout.SOUTH);
    }

    @Override void atualizar() {
        Totais t = ctx.facade().totaisContas(ctx.perfil());
        pagar.valor(Dinheiro.formatar(t.aPagar()), Tema.ALERTA);
        receber.valor(Dinheiro.formatar(t.aReceber()), Tema.VERDE);
        vencido.valor(Dinheiro.formatar(t.vencido()), new Color(0xB45309));
        pagar.rodape("Saldo previsto: " + Dinheiro.formatar(t.aReceber() - t.aPagar()));
        todas = ctx.facade().contas(ctx.perfil());
        filtrar();
    }

    private void filtrar() {
        String f = (String) filtro.getSelectedItem();
        modelo.definir(todas.stream().filter(c -> switch (f) {
            case "A pagar" -> c.tipo() == TipoConta.PAGAR;
            case "A receber" -> c.tipo() == TipoConta.RECEBER;
            case "Em aberto" -> c.situacao() == SituacaoConta.ABERTA;
            default -> true;
        }).sorted(Comparator.comparing(ContaFinanceira::vencimento)).toList());
    }

    private void nova() {
        JTextField id = Tema.campo(8), desc = Tema.campo(22), valor = Tema.campo(10), venc = Tema.campo(10);
        JComboBox<TipoConta> tipo = new JComboBox<>(TipoConta.values());
        id.setText(ctx.facade().proximoId(ctx.perfil(), "CT"));
        venc.setText(LocalDate.now().plusDays(30).format(Componentes.DATA));
        if (!Formulario.mostrar(this, "Nova conta", new String[]{"ID", "Tipo", "Descrição", "Valor (R$)", "Vencimento"},
            new JComponent[]{id, tipo, desc, valor, venc})) return;
        executar(() -> ctx.facade().lancarConta(ctx.perfil(), id.getText().trim(), (TipoConta) tipo.getSelectedItem(),
            desc.getText(), Dinheiro.lerReais(valor.getText()), Componentes.lerData(venc.getText(), "Vencimento")), "Conta lançada.");
    }

    private void agir(boolean quitar) {
        int i = selecionada(tabela);
        if (i < 0) { JOptionPane.showMessageDialog(this, "Selecione uma conta."); return; }
        String id = modelo.linha(i).id();
        executar(() -> { if (quitar) ctx.facade().quitarConta(ctx.perfil(), id); else ctx.facade().cancelarConta(ctx.perfil(), id); },
            quitar ? "Conta quitada." : "Conta cancelada.");
    }
}
