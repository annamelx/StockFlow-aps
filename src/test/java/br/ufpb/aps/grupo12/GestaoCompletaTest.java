package br.ufpb.aps.grupo12;

import br.ufpb.aps.grupo12.aplicacao.DadosDemo;
import br.ufpb.aps.grupo12.aplicacao.RelogioAjustavel;
import br.ufpb.aps.grupo12.dominio.*;
import br.ufpb.aps.grupo12.persistencia.RepositorioCsvAtomico;
import br.ufpb.aps.grupo12.politicas.DescontoPercentual;
import br.ufpb.aps.grupo12.seguranca.ContasPersistidas;
import br.ufpb.aps.grupo12.seguranca.ServicoAutenticacao;
import br.ufpb.aps.grupo12.servicos.*;
import br.ufpb.aps.grupo12.servicos.ServicoRelatorios.Periodo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Cadastros, compras, contas, relatórios, usuárias e persistência da versão completa. */
class GestaoCompletaTest {
    @TempDir Path pasta;
    Path arquivo;
    RepositorioCsvAtomico repo;
    RelogioAjustavel relogio;
    ServicoLoja loja;
    ServicoCadastros cad;
    ServicoFinanceiro fin;
    ServicoRelatorios rel;
    final Perfil g = Perfil.GERENTE, v = Perfil.VENDEDOR;
    static final LocalDate DIA = LocalDate.of(2026, 11, 10);

    @BeforeEach void preparar() {
        arquivo = pasta.resolve("loja.csv");
        repo = new RepositorioCsvAtomico(arquivo);
        relogio = new RelogioAjustavel(DIA.atTime(9, 0), ZoneId.of("America/Fortaleza"));
        loja = new ServicoLoja(repo, relogio);
        cad = new ServicoCadastros(repo);
        fin = new ServicoFinanceiro(repo, relogio);
        rel = new ServicoRelatorios(repo, relogio);
        cad.salvarCategoria(g, new Categoria("BOLSAS", "Bolsas"), true);
        cad.salvarFornecedor(g, new Fornecedor("FOR-001", "Fornecedor A", "", ""), true);
        cad.salvarCliente(g, new Cliente("CLI-001", "Caio", "", ""), true);
        loja.cadastrarProduto(g, new Produto("B1", "Bolsa", 10000, 10, 2, "BOLSAS", 4000));
    }

    private Venda vender(String id, int qtd) {
        loja.abrirVenda(g, id, Map.of("B1", qtd), new DescontoPercentual(BigDecimal.ZERO),
            new ServicoLoja.DadosVenda("CLI-001", FormaPagamento.PIX, "Karol"));
        return loja.confirmarVenda(g, id);
    }

    @Test void vendaGuardaClientePagamentoEDataDeConfirmacao() {
        relogio.ajustar(DIA.atTime(15, 30));
        Venda venda = vender("V1", 2);
        assertEquals("CLI-001", venda.clienteId());
        assertEquals(FormaPagamento.PIX, venda.formaPagamento());
        assertEquals(DIA.atTime(15, 30), venda.confirmadaEm());
        assertEquals(4000, venda.itens().getFirst().custoUnitarioCentavos(), "custo congelado no item");
    }

    @Test void vendaComClienteInexistenteEhRejeitada() {
        assertThrows(ErroNegocio.class, () -> loja.abrirVenda(g, "V1", Map.of("B1", 1), new DescontoPercentual(BigDecimal.ZERO),
            new ServicoLoja.DadosVenda("CLI-999", FormaPagamento.PIX, "")));
        assertTrue(loja.consultarVendas(g).isEmpty());
    }

    @Test void simularVendaCalculaSemGravarNada() {
        Venda previa = loja.simularVenda(v, Map.of("B1", 3), new DescontoPercentual(new BigDecimal("10")));
        assertEquals(27000, previa.totalCentavos());
        assertTrue(loja.consultarVendas(g).isEmpty());
        assertEquals(10, loja.consultarProdutos(g).getFirst().estoque());
    }

    @Test void compraAtualizaEstoqueCustoEMovimentoNaMesmaTransacao() {
        loja.reporEstoque(g, "B1", 5, "REP-1", new ServicoLoja.DadosCompra("FOR-001", 4500));
        Produto p = loja.consultarProdutos(g).getFirst();
        assertEquals(15, p.estoque());
        assertEquals(4500, p.custoCentavos());
        assertEquals(22500, rel.compras(g, Periodo.dia(DIA)).total());
        assertEquals(1, loja.consultarMovimentacoes(g).size());
    }

    @Test void compraComFornecedorInexistenteNaoAlteraNada() {
        assertThrows(ErroNegocio.class, () -> loja.reporEstoque(g, "B1", 5, "REP-1", new ServicoLoja.DadosCompra("FOR-999", 100)));
        assertEquals(10, loja.consultarProdutos(g).getFirst().estoque());
        assertTrue(loja.consultarMovimentacoes(g).isEmpty());
        assertEquals(0, rel.compras(g, Periodo.dia(DIA)).total());
    }

    @Test void clienteComVendasNaoPodeSerExcluidoECategoriaEmUsoTambemNao() {
        vender("V1", 1);
        assertThrows(ErroNegocio.class, () -> cad.excluirCliente(g, "CLI-001"));
        assertThrows(ErroNegocio.class, () -> cad.excluirCategoria(g, "BOLSAS"));
        assertThrows(ErroNegocio.class, () -> cad.salvarCliente(v, new Cliente("CLI-001", "Outro", "", ""), true), "duplicado");
        assertEquals(1, cad.clientes(v).size());
    }

    @Test void atendentePodeCadastrarClienteMasNaoExcluirNemVerFornecedores() {
        cad.salvarCliente(v, new Cliente("CLI-002", "Awanna Costa", "", ""), true);
        assertThrows(ErroNegocio.class, () -> cad.excluirCliente(v, "CLI-002"));
        assertThrows(ErroNegocio.class, () -> cad.fornecedores(v));
        assertThrows(ErroNegocio.class, () -> rel.painel(v));
    }

    @Test void quitarContaDuasVezesNaoMudaDataECanceladaNaoPodeSerQuitada() {
        fin.lancarConta(g, "CT-1", TipoConta.PAGAR, "Aluguel", 100000, DIA.plusDays(5));
        relogio.ajustar(DIA.plusDays(1).atTime(10, 0));
        ContaFinanceira q1 = fin.quitar(g, "CT-1");
        relogio.ajustar(DIA.plusDays(3).atTime(10, 0));
        ContaFinanceira q2 = fin.quitar(g, "CT-1");
        assertEquals(DIA.plusDays(1), q2.quitadaEm());
        assertEquals(q1, q2);
        fin.lancarConta(g, "CT-2", TipoConta.RECEBER, "Encomenda", 5000, DIA);
        fin.cancelar(g, "CT-2");
        assertThrows(ErroNegocio.class, () -> fin.quitar(g, "CT-2"), "cancelada não pode ser quitada");
        assertEquals(SituacaoConta.CANCELADA, fin.contas(g).get(1).situacao());
        assertThrows(ErroNegocio.class, () -> fin.lancarConta(v, "CT-3", TipoConta.PAGAR, "X", 100, DIA));
    }

    @Test void lucroEFaturamentoDescontamDevolucaoPeloValorLiquidoECusto() {
        vender("V1", 3);                    // 3 × (100 − 40) = 180,00 de lucro
        loja.devolver(g, "D1", "V1", "B1", 1); // devolve 100,00, custo 40,00 volta → lucro −60,00
        ServicoRelatorios.Resumo r = rel.resumo(g, Periodo.dia(DIA));
        assertEquals(20000, r.faturamento());
        assertEquals(12000, r.lucro());
        assertEquals(1, r.pedidos());
        assertEquals(30000, r.ticketMedio());
    }

    @Test void metaFronteiraEProgressoLimitadoA100() {
        assertThrows(ErroNegocio.class, () -> fin.definirMeta(g, 0));
        fin.definirMeta(g, 10000);
        vender("V1", 2); // 200,00 > meta de 100,00
        assertEquals(0, new BigDecimal("100").compareTo(rel.painel(g).progressoMeta()));
    }

    @Test void usuariaDesativadaNaoEntraENaoSeDesativaAUnicaGerente() {
        DadosDemo.garantirUsuarias(repo);
        ServicoAutenticacao auth = new ServicoAutenticacao(new ContasPersistidas(repo));
        cad.cadastrarUsuario(g, "caio@stockflow.demo", "Caio", Perfil.VENDEDOR, "senha123");
        assertEquals(Perfil.VENDEDOR, auth.entrar("caio@stockflow.demo", "senha123").perfil());
        cad.alterarAtivo(g, "caio@stockflow.demo", false);
        assertThrows(ErroNegocio.class, () -> auth.entrar("caio@stockflow.demo", "senha123"));
        assertThrows(ErroNegocio.class, () -> cad.alterarAtivo(g, "gerente@stockflow.demo", false));
        assertThrows(ErroNegocio.class, () -> cad.cadastrarUsuario(g, "x@stockflow.demo", "X", Perfil.VENDEDOR, "123"));
    }

    @Test void todosOsCadastrosSobrevivemAoReinicioEArquivoVersao1AindaEhLido() throws Exception {
        vender("V1", 1);
        fin.lancarConta(g, "CT-1", TipoConta.PAGAR, "Luz, água", 5000, DIA);
        ServicoCadastros outro = new ServicoCadastros(new RepositorioCsvAtomico(arquivo));
        assertEquals("Caio", outro.clientes(g).getFirst().nome());
        assertEquals("Luz, água", new ServicoFinanceiro(new RepositorioCsvAtomico(arquivo), relogio).contas(g).getFirst().descricao());
        Path antigo = pasta.resolve("v1.csv");
        Files.writeString(antigo, "APS_CSV,1\nP,P1,QmFsc2E,10000,5,2\nV,V1,CONFIRMADA,10000,0,10000,WA\nI,V1,P1,1,10000,0\n",
            StandardCharsets.UTF_8);
        ServicoLoja legado = new ServicoLoja(new RepositorioCsvAtomico(antigo));
        assertEquals("Balsa", legado.consultarProdutos(g).getFirst().nome());
        assertEquals(EstadoVenda.CONFIRMADA, legado.consultarVendas(g).getFirst().estado());
    }

    @Test void dadosDeDemonstracaoSaoGeradosPorOperacoesReaisESaldosDocumentados() {
        DadosDemo.restaurar(repo, DIA);
        loja.consultarProdutos(g).forEach(p -> assertEquals((int) DadosDemo.SALDO_FINAL.get(p.id()), p.estoque(), p.id()));
        assertTrue(loja.consultarVendas(g).size() > 50);
        assertTrue(rel.painel(g).hojeResumo().pedidos() > 0, "há vendas no dia da restauração");
        assertEquals(2, loja.consultarProdutosEmAlerta(g).size());
    }
}
