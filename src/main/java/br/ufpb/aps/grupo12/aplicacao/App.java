package br.ufpb.aps.grupo12.aplicacao;

import br.ufpb.aps.grupo12.interfacegrafica.JanelaLogin;
import br.ufpb.aps.grupo12.observadores.AlertaEstoqueMinimo;
import br.ufpb.aps.grupo12.persistencia.RepositorioCsvAtomico;
import br.ufpb.aps.grupo12.politicas.fabricas.CatalogoDescontos;
import br.ufpb.aps.grupo12.relatorios.AdaptadorRelatorio;
import br.ufpb.aps.grupo12.relatorios.ExportadorAntigo;
import br.ufpb.aps.grupo12.seguranca.ContasPersistidas;
import br.ufpb.aps.grupo12.seguranca.ServicoAutenticacao;
import br.ufpb.aps.grupo12.servicos.LojaFacade;
import br.ufpb.aps.grupo12.servicos.ServicoCadastros;
import br.ufpb.aps.grupo12.servicos.ServicoFinanceiro;
import br.ufpb.aps.grupo12.servicos.ServicoLoja;
import br.ufpb.aps.grupo12.servicos.ServicoRelatorios;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Ponto de entrada: SOMENTE cria e conecta os componentes (injeção por construtor).
 * Nenhuma regra de negócio fica aqui.
 *
 * Argumentos: --reset-demo  restaura os dados fictícios
 *             --console     usa a interface de texto em vez da janela gráfica
 */
public final class App {
    private App() {}

    public static void main(String[] args) {
        List<String> opcoes = List.of(args);
        Path caminho = Path.of("dados", "loja.csv");
        Clock relogio = Clock.systemDefaultZone();
        RepositorioCsvAtomico repo = new RepositorioCsvAtomico(caminho);
        if (opcoes.contains("--reset-demo")) {
            DadosDemo.restaurar(repo, LocalDate.now(relogio));
            System.out.println("Dados fictícios restaurados. Abra o sistema novamente.");
            return;
        }
        if (!Files.exists(caminho)) DadosDemo.restaurar(repo, LocalDate.now(relogio));
        else DadosDemo.garantirUsuarias(repo);

        ServicoLoja servico = new ServicoLoja(repo, relogio);
        AlertaEstoqueMinimo alerta = new AlertaEstoqueMinimo();
        servico.observar(alerta);
        LojaFacade facade = new LojaFacade(servico, new AdaptadorRelatorio(new ExportadorAntigo()),
            new ServicoCadastros(repo), new ServicoFinanceiro(repo, relogio), new ServicoRelatorios(repo, relogio));
        ServicoAutenticacao autenticacao = new ServicoAutenticacao(new ContasPersistidas(repo));
        CatalogoDescontos descontos = CatalogoDescontos.padrao();

        if (opcoes.contains("--console") || GraphicsEnvironment.isHeadless()) {
            new ConsoleView(facade, autenticacao, descontos, alerta).executar(System.in, System.out);
        } else {
            JanelaLogin.abrir(facade, autenticacao, descontos, alerta);
        }
    }
}
