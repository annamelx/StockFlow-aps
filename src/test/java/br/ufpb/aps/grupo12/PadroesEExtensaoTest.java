package br.ufpb.aps.grupo12;

import br.ufpb.aps.grupo12.aplicacao.ConsoleView;
import br.ufpb.aps.grupo12.dominio.*;
import br.ufpb.aps.grupo12.observadores.AlertaEstoqueMinimo;
import br.ufpb.aps.grupo12.persistencia.RepositorioCsvAtomico;
import br.ufpb.aps.grupo12.politicas.*;
import br.ufpb.aps.grupo12.politicas.fabricas.*;
import br.ufpb.aps.grupo12.relatorios.*;
import br.ufpb.aps.grupo12.seguranca.*;
import br.ufpb.aps.grupo12.servicos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes do Factory Method, da nova extensão do Strategy (desconto progressivo),
 * da autenticação dos perfis e da integração da interface com a Facade.
 */
class PadroesEExtensaoTest {
    @TempDir Path pasta;
    ServicoLoja loja;
    final Perfil gerente = Perfil.GERENTE;
    final Perfil vendedor = Perfil.VENDEDOR;

    @BeforeEach void preparar() {
        loja = new ServicoLoja(new RepositorioCsvAtomico(pasta.resolve("loja.csv")));
        loja.cadastrarProduto(gerente, new Produto("BOLSA", "Bolsa", 10000, 20, 2));
        loja.cadastrarProduto(gerente, new Produto("CHAVEIRO", "Chaveiro", 1, 5, 0));
    }

    // ---------- Factory Method ----------
    @Test void factoryMethodCadaCriadorConcretoProduzSuaPolitica() {
        assertInstanceOf(DescontoPercentual.class, new CriadorDescontoPercentual().criar("10"));
        assertInstanceOf(DescontoFixo.class, new CriadorDescontoFixo().criar("23,50"));
        assertInstanceOf(DescontoPorFaixa.class, new CriadorDescontoPorFaixa().criar(""));
        assertEquals(2350, new CriadorDescontoFixo().criar("23,50").descontoEmCentavos(10000));
        assertEquals(1000, new CriadorDescontoPercentual().criar("10%").descontoEmCentavos(10000));
    }

    @Test void factoryMethodPermiteNovoCriadorSobrescrevendoApenasFabricar() {
        // Um criador anônimo novo: só sobrescreve a operação de criação; o fluxo de criar() é herdado.
        CriadorPoliticaDesconto natal = new CriadorPoliticaDesconto() {
            @Override protected PoliticaDesconto fabricar(String p) { return new DescontoFixo(500); }
            @Override public String descricao() { return "Natal"; }
        };
        Venda v = loja.abrirVenda(vendedor, "V1", Map.of("BOLSA", 1), natal.criar("qualquer"));
        assertEquals(500, v.descontoCentavos());
        assertThrows(ErroNegocio.class, () -> natal.criar("  "), "parâmetro obrigatório validado no Creator");
    }

    @Test void factoryMethodRejeitaParametroInvalidoSemCriarVenda() {
        assertThrows(ErroNegocio.class, () -> new CriadorDescontoPercentual().criar("dez"));
        assertThrows(ErroNegocio.class, () -> new CriadorDescontoPercentual().criar("150"));
        assertThrows(ErroNegocio.class, () -> new CriadorDescontoFixo().criar("1,999"));
        assertTrue(loja.consultarVendas(gerente).isEmpty());
    }

    // ---------- Extensão do Strategy: desconto progressivo ----------
    @Test void descontoProgressivoRespeitaFronteirasDasFaixas() {
        PoliticaDesconto faixa = DescontoPorFaixa.padraoDaLoja();
        assertEquals(0, faixa.descontoEmCentavos(29999));      // R$ 299,99 → sem desconto
        assertEquals(1500, faixa.descontoEmCentavos(30000));   // R$ 300,00 → 5%
        assertEquals(2500, faixa.descontoEmCentavos(49999));   // R$ 499,99 → 5% (2499,95 → 2500)
        assertEquals(5000, faixa.descontoEmCentavos(50000));   // R$ 500,00 → 10%
    }

    @Test void extensaoProgressivaFuncionaNoServicoSemAlterarSeuContrato() {
        Venda pequena = loja.abrirVenda(vendedor, "V1", Map.of("BOLSA", 2), DescontoPorFaixa.padraoDaLoja());
        Venda grande = loja.abrirVenda(vendedor, "V2", Map.of("BOLSA", 5), DescontoPorFaixa.padraoDaLoja());
        assertEquals(0, pequena.descontoCentavos());
        assertEquals(5000, grande.descontoCentavos());
        assertEquals(45000, grande.totalCentavos());
        loja.confirmarVenda(vendedor, "V2");
        Devolucao d = loja.devolver(gerente, "D1", "V2", "BOLSA", 1);
        assertEquals(9000, d.reembolsoCentavos(), "reembolso usa o valor líquido (com desconto)");
    }

    // ---------- Perfis / autenticação ----------
    @Test void autenticacaoDevolvePerfilCorretoERejeitaSenhaErrada() {
        ServicoAutenticacao auth = new ServicoAutenticacao(new ContasDemo());
        assertEquals(Perfil.GERENTE, auth.entrar("GERENTE@stockflow.demo", ContasDemo.SENHA_DEMO).perfil());
        assertEquals(Perfil.VENDEDOR, auth.entrar("vendedora@stockflow.demo", ContasDemo.SENHA_DEMO).perfil());
        assertThrows(ErroNegocio.class, () -> auth.entrar("vendedora@stockflow.demo", "errada"));
        assertThrows(ErroNegocio.class, () -> auth.entrar("ninguem@stockflow.demo", ContasDemo.SENHA_DEMO));
    }

    @Test void produtosEmAlertaIncluemSaldoIgualAoMinimo() {
        loja.cadastrarProduto(gerente, new Produto("LIMITE", "No limite", 100, 2, 2));
        loja.cadastrarProduto(gerente, new Produto("ACIMA", "Acima", 100, 3, 2));
        var ids = loja.consultarProdutosEmAlerta(vendedor).stream().map(Produto::id).toList();
        assertTrue(ids.contains("LIMITE"));
        assertFalse(ids.contains("ACIMA"));
    }

    // ---------- Integração: View → Facade → Serviço ----------
    @Test void consoleFazLoginEAbreVendaUsandoFactoryMethodPelaFacade() {
        LojaFacade facade = new LojaFacade(loja, new AdaptadorRelatorio(new ExportadorAntigo()));
        String roteiro = String.join("\n", "vendedora@stockflow.demo", "stockflow2026",
            "2", "V9", "BOLSA", "3", "n", "3", "",   // abrir venda, desconto progressivo (3ª opção)
            "3", "V9",                               // confirmar
            "5", "BOLSA", "1", "R1",                 // vendedora tenta repor → negado no serviço
            "0") + "\n";
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        new ConsoleView(facade, new ServicoAutenticacao(new ContasDemo()), CatalogoDescontos.padrao(),
            new AlertaEstoqueMinimo()).executar(new ByteArrayInputStream(roteiro.getBytes(StandardCharsets.UTF_8)),
            new PrintStream(saida, true, StandardCharsets.UTF_8));
        String texto = saida.toString(StandardCharsets.UTF_8);
        Venda v9 = facade.vendas(gerente).getFirst();
        assertEquals(EstadoVenda.CONFIRMADA, v9.estado());
        assertEquals(1500, v9.descontoCentavos());
        assertEquals(17, facade.estoque(gerente).getFirst().estoque());
        assertTrue(texto.contains("Perfil não tem autorização"));
    }

    @Test void dinheiroConverteReaisECentavosSemPontoFlutuante() {
        assertEquals(123456, Dinheiro.lerReais("R$ 1.234,56"));
        assertEquals(2350, Dinheiro.lerReais("23.50"));
        assertEquals("R$ 1.234,56", Dinheiro.formatar(123456));
        assertThrows(ErroNegocio.class, () -> Dinheiro.lerReais("abc"));
    }
}
