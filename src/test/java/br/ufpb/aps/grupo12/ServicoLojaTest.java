package br.ufpb.aps.grupo12;

import br.ufpb.aps.grupo12.dominio.*;
import br.ufpb.aps.grupo12.observadores.AlertaEstoqueMinimo;
import br.ufpb.aps.grupo12.persistencia.RepositorioCsvAtomico;
import br.ufpb.aps.grupo12.politicas.*;
import br.ufpb.aps.grupo12.relatorios.*;
import br.ufpb.aps.grupo12.servicos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ServicoLojaTest {
    @TempDir Path pasta;
    Path arquivo;
    RepositorioCsvAtomico repo;
    ServicoLoja loja;
    private final Perfil gerente = Perfil.GERENTE;
    private final Perfil vendedor = Perfil.VENDEDOR;
    private PoliticaDesconto semDesconto() { return new DescontoPercentual(BigDecimal.ZERO); }
    @BeforeEach void preparar() {
        arquivo = pasta.resolve("loja.csv");
        repo = new RepositorioCsvAtomico(arquivo);
        loja = new ServicoLoja(repo);
        loja.cadastrarProduto(gerente, new Produto("P1", "Bolsa", 10000, 5, 2));
        loja.cadastrarProduto(gerente, new Produto("P2", "Carteira", 2000, 1, 1));
    }
    int saldo(String id) {
        return loja.consultarProdutos(vendedor).stream().filter(p -> p.id().equals(id))
            .findFirst().orElseThrow().estoque();
    }
    void vendaConfirmada(String id, int qtd) {
        loja.abrirVenda(vendedor, id, Map.of("P1", qtd), semDesconto());
        loja.confirmarVenda(vendedor, id);
    }

    @Test void vendaValidaBaixaEstoqueEHistorico() {
        vendaConfirmada("V1", 2);
        assertEquals(3, saldo("P1"));
        assertEquals(1, loja.consultarMovimentacoes(gerente).size());
        assertEquals(20000, loja.consultarReceitaLiquidaCentavos(gerente));
    }
    @Test void impedeVendaComEstoqueInsuficienteSemEfeitoParcial() {
        loja.abrirVenda(vendedor, "V1", Map.of("P1", 2, "P2", 2), semDesconto());
        assertThrows(ErroNegocio.class, () -> loja.confirmarVenda(vendedor, "V1"));
        assertEquals(5, saldo("P1"));
        assertEquals(1, saldo("P2"));
        assertTrue(loja.consultarMovimentacoes(gerente).isEmpty());
        assertEquals(EstadoVenda.PENDENTE, loja.consultarVendas(gerente).getFirst().estado());
    }
    @Test void confirmarDuasVezesNaoBaixaEstoqueNovamente() {
        vendaConfirmada("V1", 2);
        loja.confirmarVenda(vendedor, "V1");
        assertEquals(3, saldo("P1"));
        assertEquals(1, loja.consultarMovimentacoes(gerente).size());
    }
    @Test void vendaCanceladaNaoPodeSerConfirmada() {
        loja.abrirVenda(vendedor, "V1", Map.of("P1", 1), semDesconto());
        loja.cancelarVendaPendente(vendedor, "V1");
        assertThrows(ErroNegocio.class, () -> loja.confirmarVenda(vendedor, "V1"));
        assertEquals(5, saldo("P1"));
    }
    @Test void vendaConfirmadaNaoPodeSerCanceladaComoPendente() {
        vendaConfirmada("V1", 1);
        assertThrows(ErroNegocio.class, () -> loja.cancelarVendaPendente(vendedor, "V1"));
    }
    @Test void devolucaoParcialVinculadaAVendaRepõeEstoqueECalculaReembolso() {
        vendaConfirmada("V1", 3);
        Devolucao d = loja.devolver(gerente, "D1", "V1", "P1", 2);
        assertEquals(20000, d.reembolsoCentavos());
        assertEquals(4, saldo("P1"));
        assertEquals(10000, loja.consultarReceitaLiquidaCentavos(gerente));
    }
    @Test void devolucaoExatamenteNoLimiteEhAceita() {
        vendaConfirmada("V1", 2);
        loja.devolver(gerente, "D1", "V1", "P1", 2);
        assertEquals(5, saldo("P1"));
        assertEquals(0, loja.consultarReceitaLiquidaCentavos(gerente));
    }
    @Test void devolucaoAcimaDaQuantidadeVendidaEhRejeitada() {
        vendaConfirmada("V1", 2);
        loja.devolver(gerente, "D1", "V1", "P1", 1);
        assertThrows(ErroNegocio.class, () -> loja.devolver(gerente, "D2", "V1", "P1", 2));
        assertEquals(4, saldo("P1"));
        assertEquals(2, loja.consultarMovimentacoes(gerente).size());
    }
    @Test void devolucaoDeProdutoNaoVendidoEhRejeitada() {
        vendaConfirmada("V1", 1);
        assertThrows(ErroNegocio.class, () -> loja.devolver(gerente, "D1", "V1", "P2", 1));
        assertEquals(1, saldo("P2"));
    }
    @Test void devolucaoDeVendaPendenteEhRejeitada() {
        loja.abrirVenda(vendedor, "V1", Map.of("P1", 1), semDesconto());
        assertThrows(ErroNegocio.class, () -> loja.devolver(gerente, "D1", "V1", "P1", 1));
    }
    @Test void devolverDuasVezesComMesmoIdNaoDuplicaReembolsoNemEstoque() {
        vendaConfirmada("V1", 2);
        loja.devolver(gerente, "D1", "V1", "P1", 1);
        loja.devolver(gerente, "D1", "V1", "P1", 1);
        assertEquals(4, saldo("P1"));
        assertEquals(10000, loja.consultarReceitaLiquidaCentavos(gerente));
        assertEquals(2, loja.consultarMovimentacoes(gerente).size());
    }
    @Test void idDeDevolucaoNaoPodeSerReutilizadoComOutrosDados() {
        vendaConfirmada("V1", 2);
        loja.devolver(gerente, "D1", "V1", "P1", 1);
        assertThrows(ErroNegocio.class, () -> loja.devolver(gerente, "D1", "V1", "P1", 2));
    }
    @Test void descontoPercentualCalculaValorCorreto() {
        Venda v = loja.abrirVenda(vendedor, "V1", Map.of("P1", 1),
            new DescontoPercentual(new BigDecimal("10")));
        assertEquals(1000, v.descontoCentavos());
        assertEquals(9000, v.totalCentavos());
    }
    @Test void strategyFixoEhExtensaoSemModificarServico() {
        Venda v = loja.abrirVenda(vendedor, "V1", Map.of("P1", 1), new DescontoFixo(2350));
        assertEquals(2350, v.descontoCentavos());
        assertEquals(7650, v.totalCentavos());
    }
    @Test void naoPermiteDescontoNegativoOuMaiorQueSubtotal() {
        assertThrows(ErroNegocio.class, () -> new DescontoPercentual(new BigDecimal("101")));
        assertThrows(ErroNegocio.class, () -> loja.abrirVenda(vendedor, "V1", Map.of("P1", 1), new DescontoFixo(10001)));
        assertEquals(0, loja.consultarVendas(gerente).size());
    }
    @Test void fronteiraQuantidadeZeroNaoCriaVenda() {
        assertThrows(ErroNegocio.class, () -> loja.abrirVenda(vendedor, "V1", Map.of("P1", 0), semDesconto()));
    }
    @Test void fronteiraQuantidadeExatamenteEstoquePodeSerConfirmada() {
        vendaConfirmada("V1", 5);
        assertEquals(0, saldo("P1"));
    }
    @Test void vendedorNaoPodeDevolverNemReporNemCadastrar() {
        vendaConfirmada("V1", 1);
        assertThrows(ErroNegocio.class, () -> loja.devolver(vendedor, "D1", "V1", "P1", 1));
        assertThrows(ErroNegocio.class, () -> loja.reporEstoque(vendedor, "P1", 1, "R1"));
        assertThrows(ErroNegocio.class, () -> loja.cadastrarProduto(vendedor,
            new Produto("NOVO", "Bolsa 2", 100, 1, 0)));
    }
    @Test void reinicioRecuperaTodasInformacoesPersistidas() {
        vendaConfirmada("V1", 1);
        loja.devolver(gerente, "D1", "V1", "P1", 1);
        ServicoLoja novaInstancia = new ServicoLoja(new RepositorioCsvAtomico(arquivo));
        assertEquals(5, novaInstancia.consultarProdutos(vendedor).stream()
            .filter(p -> p.id().equals("P1")).findFirst().orElseThrow().estoque());
        assertEquals(1, novaInstancia.consultarDevolucoes(gerente).size());
        assertEquals(2, novaInstancia.consultarMovimentacoes(gerente).size());
    }
    @Test void falhaAntesDoCommitNaoDeixaVendaParcial() {
        loja.abrirVenda(vendedor, "V1", Map.of("P1", 2), semDesconto());
        ServicoLoja falha = new ServicoLoja(new RepositorioCsvAtomico(arquivo, () -> {
            throw new IllegalStateException("Falha simulada antes de substituir snapshot");
        }));
        assertThrows(IllegalStateException.class, () -> falha.confirmarVenda(vendedor, "V1"));
        assertEquals(5, saldo("P1"));
        assertEquals(EstadoVenda.PENDENTE, loja.consultarVendas(gerente).getFirst().estado());
        assertEquals(0, loja.consultarMovimentacoes(gerente).size());
    }
    @Test void falhaAntesDoCommitNaoDeixaDevolucaoParcial() {
        vendaConfirmada("V1", 2);
        ServicoLoja falha = new ServicoLoja(new RepositorioCsvAtomico(arquivo, () -> {
            throw new IllegalStateException("Falha simulada antes de substituir snapshot");
        }));
        assertThrows(IllegalStateException.class, () -> falha.devolver(gerente, "D1", "V1", "P1", 1));
        assertEquals(3, saldo("P1"));
        assertEquals(0, loja.consultarDevolucoes(gerente).size());
        assertEquals(20000, loja.consultarReceitaLiquidaCentavos(gerente));
    }
    @Test void observerSoRecebeEventoAposCommitEAlertaEstoqueMinimo() {
        AlertaEstoqueMinimo alerta = new AlertaEstoqueMinimo();
        loja.observar(alerta);
        loja.abrirVenda(vendedor, "V1", Map.of("P1", 3), semDesconto());
        assertTrue(alerta.alertas().isEmpty());
        loja.confirmarVenda(vendedor, "V1");
        assertEquals(1, alerta.alertas().size());
        loja.confirmarVenda(vendedor, "V1");
        assertEquals(1, alerta.alertas().size());
    }
    @Test void facadeEncaminhaFluxoCompletoParaOsServicos() {
        LojaFacade facade = new LojaFacade(loja, new AdaptadorRelatorio(new ExportadorAntigo()));
        facade.abrirVenda(vendedor, "V1", Map.of("P1", 1), semDesconto());
        facade.confirmar(vendedor, "V1");
        assertEquals(4, facade.estoque(vendedor).stream().filter(p -> p.id().equals("P1"))
            .findFirst().orElseThrow().estoque());
        assertEquals(1, facade.historico(vendedor).size());
    }
    @Test void adapterExportaRelatorioParaFormatoLegado() {
        LojaFacade facade = new LojaFacade(loja, new AdaptadorRelatorio(new ExportadorAntigo()));
        String csv = facade.relatorio(gerente);
        assertTrue(csv.startsWith("CODIGO;PRODUTO;QUANTIDADE"));
        assertTrue(csv.contains("\"P1\";\"Bolsa\";\"5\""));
    }
    @Test void reposicaoDuplicadaEhRejeitadaEEstoquePreservado() {
        loja.reporEstoque(gerente, "P1", 2, "R1");
        assertThrows(ErroNegocio.class, () -> loja.reporEstoque(gerente, "P1", 2, "R1"));
        assertEquals(7, saldo("P1"));
        assertEquals(1, loja.consultarMovimentacoes(gerente).size());
    }
    @Test void produtoExigeIdUnicoPrecoPositivoESaldoNaoNegativo() {
        assertThrows(ErroNegocio.class, () -> new Produto("P0", "Bolsa inválida", 0, 1, 0));
        assertThrows(ErroNegocio.class, () -> new Produto("P0", "Bolsa inválida", 100, -1, 0));
        assertThrows(ErroNegocio.class, () -> new Produto("ID ERRADO", "Bolsa inválida", 100, 1, 0));
        assertThrows(ErroNegocio.class, () -> loja.cadastrarProduto(gerente,
            new Produto("P1", "Duplicada", 100, 1, 0)));
        assertEquals(2, loja.consultarProdutos(gerente).size());
    }
    @Test void rateioDeDescontoEReembolsoConservamCentavos() {
        // Três itens de um centavo, desconto de dois centavos: soma dos descontos não excede preço.
        loja.cadastrarProduto(gerente, new Produto("C1", "1 centavo", 1, 2, 0));
        loja.cadastrarProduto(gerente, new Produto("C2", "1 centavo", 1, 2, 0));
        loja.cadastrarProduto(gerente, new Produto("C3", "1 centavo", 1, 2, 0));
        Venda v = loja.abrirVenda(vendedor, "V1", Map.of("C1", 1,"C2", 1,"C3", 1), new DescontoFixo(2));
        assertEquals(2, v.itens().stream().mapToLong(ItemVenda::descontoCentavos).sum());
        assertEquals(1, v.totalCentavos());
        loja.confirmarVenda(vendedor, "V1");
        for (int i = 1; i <= 3; i++) loja.devolver(gerente, "D" + i, "V1", "C" + i, 1);
        assertEquals(0, loja.consultarReceitaLiquidaCentavos(gerente));
    }
}
