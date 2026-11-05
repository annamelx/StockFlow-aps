package br.ufpb.aps.grupo12.aplicacao;

import br.ufpb.aps.grupo12.dominio.*;
import br.ufpb.aps.grupo12.observadores.AlertaEstoqueMinimo;
import br.ufpb.aps.grupo12.persistencia.RepositorioCsvAtomico;
import br.ufpb.aps.grupo12.politicas.*;
import br.ufpb.aps.grupo12.relatorios.*;
import br.ufpb.aps.grupo12.servicos.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;

/** Console simples. Regras, permissão e persistência ficam nas camadas apropriadas. */
public final class App {
    private App() {}
    public static void main(String[] args) {
        Path caminho = Path.of("dados", "loja.csv");
        RepositorioCsvAtomico repo = new RepositorioCsvAtomico(caminho);
        if (Arrays.asList(args).contains("--reset-demo")) {
            repo.restaurarDemo();
            System.out.println("Dados fictícios restaurados. Abra o sistema novamente.");
            return;
        }
        if (!Files.exists(caminho)) repo.restaurarDemo();
        ServicoLoja servico = new ServicoLoja(repo);
        AlertaEstoqueMinimo alerta = new AlertaEstoqueMinimo();
        servico.observar(alerta);
        LojaFacade facade = new LojaFacade(servico, new AdaptadorRelatorio(new ExportadorAntigo()));
        Scanner in = new Scanner(System.in);
        System.out.println("\nAPS UFPB 2026.2 | GRUPO 12 | GESTÃO DE ESTOQUE E VENDAS");
        System.out.println("Demonstração acadêmica com seleção de perfil. Não há autenticação real.");
        Perfil perfil = ler(in, "Escolha perfil [1=GERENTE, 2=VENDEDOR]: ").equals("1")
            ? Perfil.GERENTE : Perfil.VENDEDOR;
        while (true) {
            System.out.println("\n1 Estoque | 2 Abrir venda | 3 Confirmar | 4 Devolver | 5 Repor | 6 Relatório | 7 Histórico | 8 Cancelar pendente | 9 Alertas | 0 Sair");
            String opcao = ler(in, "> ");
            try {
                switch (opcao) {
                    case "0" -> { return; }
                    case "1" -> facade.estoque(perfil).forEach(System.out::println);
                    case "2" -> {
                        String id = ler(in, "ID da venda: ");
                        String produto = ler(in, "ID do produto: ");
                        int qtd = Integer.parseInt(ler(in, "Quantidade: "));
                        String tipo = ler(in, "Desconto [1=percentual, 2=fixo]: ");
                        PoliticaDesconto desc = tipo.equals("1")
                            ? new DescontoPercentual(new BigDecimal(ler(in, "Percentual: ")))
                            : new DescontoFixo(Long.parseLong(ler(in, "Valor fixo em centavos: ")));
                        System.out.println(facade.abrirVenda(perfil, id, Map.of(produto, qtd), desc));
                    }
                    case "3" -> System.out.println(facade.confirmar(perfil, ler(in, "ID da venda: ")));
                    case "4" -> {
                        String id = ler(in, "ID da devolução: ");
                        String venda = ler(in, "ID da venda: ");
                        String produto = ler(in, "ID do produto: ");
                        int qtd = Integer.parseInt(ler(in, "Quantidade: "));
                        System.out.println(facade.devolver(perfil, id, venda, produto, qtd));
                    }
                    case "5" -> {
                        String produto = ler(in, "ID do produto: ");
                        int qtd = Integer.parseInt(ler(in, "Quantidade: "));
                        String id = ler(in, "ID único da reposição: ");
                        System.out.println(facade.repor(perfil, produto, qtd, id));
                    }
                    case "6" -> { System.out.println(facade.relatorio(perfil));
                        System.out.println("Receita líquida (centavos): " + facade.receitaLiquida(perfil)); }
                    case "7" -> facade.historico(perfil).forEach(System.out::println);
                    case "8" -> System.out.println(facade.cancelar(perfil, ler(in, "ID da venda: ")));
                    case "9" -> alerta.alertas().forEach(System.out::println);
                    default -> System.out.println("Opção inexistente.");
                }
            } catch (RuntimeException e) {
                System.out.println("Operação não realizada: " + e.getMessage());
            }
        }
    }
    private static String ler(Scanner in, String texto) {
        System.out.print(texto);
        return in.hasNextLine() ? in.nextLine().trim() : "0";
    }
}
