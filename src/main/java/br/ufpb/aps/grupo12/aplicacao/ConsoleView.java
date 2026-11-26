package br.ufpb.aps.grupo12.aplicacao;

import br.ufpb.aps.grupo12.dominio.*;
import br.ufpb.aps.grupo12.observadores.AlertaEstoqueMinimo;
import br.ufpb.aps.grupo12.politicas.PoliticaDesconto;
import br.ufpb.aps.grupo12.politicas.fabricas.CatalogoDescontos;
import br.ufpb.aps.grupo12.politicas.fabricas.CriadorPoliticaDesconto;
import br.ufpb.aps.grupo12.seguranca.ServicoAutenticacao;
import br.ufpb.aps.grupo12.seguranca.Usuario;
import br.ufpb.aps.grupo12.servicos.LojaFacade;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.*;

/** View de texto: lê comandos e mostra resultados. Regras, permissões e persistência ficam nos serviços. */
public final class ConsoleView {
    private final LojaFacade facade;
    private final ServicoAutenticacao autenticacao;
    private final CatalogoDescontos descontos;
    private final AlertaEstoqueMinimo alerta;
    private Scanner in;
    private PrintStream out;

    public ConsoleView(LojaFacade facade, ServicoAutenticacao autenticacao,
                       CatalogoDescontos descontos, AlertaEstoqueMinimo alerta) {
        this.facade = Objects.requireNonNull(facade);
        this.autenticacao = Objects.requireNonNull(autenticacao);
        this.descontos = Objects.requireNonNull(descontos);
        this.alerta = Objects.requireNonNull(alerta);
    }

    public void executar(InputStream entrada, PrintStream saida) {
        in = new Scanner(entrada);
        out = saida;
        out.println("\nSTOCKFLOW | ESTOQUE & VENDAS | APS UFPB 2026.2 | GRUPO 12");
        out.println("Contas de demonstração: gerente@stockflow.demo / vendedora@stockflow.demo (senha stockflow2026)");
        Usuario usuario = null;
        while (usuario == null) {
            try {
                usuario = autenticacao.entrar(ler("E-mail: "), ler("Senha: "));
            } catch (ErroNegocio e) {
                out.println(e.getMessage());
                if (!in.hasNextLine()) return;
            }
        }
        Perfil perfil = usuario.perfil();
        out.println("Bem-vindo(a), " + usuario.nome() + " — " + usuario.rotuloPerfil());
        while (true) {
            out.println("\n1 Estoque | 2 Abrir venda | 3 Confirmar | 4 Devolver | 5 Repor | 6 Relatório"
                + " | 7 Histórico | 8 Cancelar pendente | 9 Alertas | 0 Sair");
            String opcao = ler("> ");
            try {
                switch (opcao) {
                    case "0" -> { return; }
                    case "1" -> facade.estoque(perfil).forEach(p -> out.println(p.id() + " | " + p.nome()
                        + " | " + Dinheiro.formatar(p.precoCentavos()) + " | saldo " + p.estoque()
                        + (p.emAlerta() ? "  ⚠ mínimo " + p.estoqueMinimo() : "")));
                    case "2" -> abrirVenda(perfil);
                    case "3" -> out.println(facade.confirmar(perfil, ler("ID da venda: ")));
                    case "4" -> {
                        String id = ler("ID da devolução: ");
                        String venda = ler("ID da venda: ");
                        String produto = ler("ID do produto: ");
                        int qtd = inteiro(ler("Quantidade: "));
                        Devolucao d = facade.devolver(perfil, id, venda, produto, qtd);
                        out.println(d + " → reembolso " + Dinheiro.formatar(d.reembolsoCentavos()));
                    }
                    case "5" -> {
                        String produto = ler("ID do produto: ");
                        int qtd = inteiro(ler("Quantidade: "));
                        out.println(facade.repor(perfil, produto, qtd, ler("ID único da reposição: ")));
                    }
                    case "6" -> {
                        out.println(facade.relatorio(perfil));
                        out.println("Receita líquida: " + Dinheiro.formatar(facade.receitaLiquida(perfil)));
                    }
                    case "7" -> facade.historico(perfil).forEach(out::println);
                    case "8" -> out.println(facade.cancelar(perfil, ler("ID da venda: ")));
                    case "9" -> alerta.alertas().forEach(out::println);
                    default -> out.println("Opção inexistente.");
                }
            } catch (RuntimeException e) {
                out.println("Operação não realizada: " + e.getMessage());
            }
        }
    }

    private void abrirVenda(Perfil perfil) {
        String id = ler("ID da venda: ");
        Map<String, Integer> itens = new LinkedHashMap<>();
        do {
            itens.merge(ler("ID do produto: "), inteiro(ler("Quantidade: ")), Integer::sum);
        } while (ler("Adicionar outro item? [s/N]: ").equalsIgnoreCase("s"));
        List<CriadorPoliticaDesconto> opcoes = descontos.criadores();
        for (int i = 0; i < opcoes.size(); i++) out.println((i + 1) + " = " + opcoes.get(i).descricao());
        int escolha = inteiro(ler("Desconto: ")) - 1;
        if (escolha < 0 || escolha >= opcoes.size()) throw new ErroNegocio("Política de desconto inexistente.");
        CriadorPoliticaDesconto criador = opcoes.get(escolha);
        String parametro = criador.exigeParametro() ? ler(criador.rotuloParametro() + ": ") : "";
        PoliticaDesconto politica = criador.criar(parametro); // Factory Method
        Venda v = facade.abrirVenda(perfil, id, itens, politica);
        out.println(v.id() + " " + v.estado() + " | subtotal " + Dinheiro.formatar(v.subtotalCentavos())
            + " | desconto " + Dinheiro.formatar(v.descontoCentavos()) + " | total " + Dinheiro.formatar(v.totalCentavos()));
    }

    private int inteiro(String texto) {
        try { return Integer.parseInt(texto); }
        catch (NumberFormatException e) { throw new ErroNegocio("Número inteiro inválido: " + texto); }
    }

    private String ler(String texto) {
        out.print(texto);
        return in.hasNextLine() ? in.nextLine().trim() : "0";
    }
}
