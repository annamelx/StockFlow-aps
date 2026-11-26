# Matriz de rastreabilidade — requisitos → código → teste → demonstração

**Os identificadores RN01 a RN11 foram definidos em `requisitos.md`.** Todos os métodos são relativos a `src/main/java/br/ufpb/aps/grupo12/`. Os testes pertencem a `src/test/java/br/ufpb/aps/grupo12/ServicoLojaTest.java` (ST) e `PadroesEExtensaoTest.java` (PE).

| Caso / regra | Implementação responsável | Teste automatizado | Evidência na apresentação |
|---|---|---|---|
| UC02 / RN01 estoque suficiente | `servicos/ServicoLoja.confirmarVenda`, `dominio/Produto` | `impedeVendaComEstoqueInsuficienteSemEfeitoParcial`, `fronteiraQuantidadeExatamenteEstoquePodeSerConfirmada` | Tentar 2 itens quando um tem saldo 1; saldo intacto |
| UC02 / RN02 confirmação idempotente | `ServicoLoja.confirmarVenda`, `persistencia/EstadoDados.mesmoConteudo` | `confirmarDuasVezesNaoBaixaEstoqueNovamente` | Confirmar V1 duas vezes; só uma saída |
| UC02 / RN03 estados | `dominio/Venda.comEstado`, `ServicoLoja.cancelarVendaPendente` | `vendaCanceladaNaoPodeSerConfirmada`, `vendaConfirmadaNaoPodeSerCanceladaComoPendente` | Cancelar pendente e tentar confirmar |
| UC03 / RN04 devolução válida | `ServicoLoja.devolver`, `dominio/Venda.item` | `devolucaoDeProdutoNaoVendidoEhRejeitada`, `devolucaoDeVendaPendenteEhRejeitada` | Tentar devolver produto que não aparece em V1 |
| UC03 / RN05 limite de devolução | `ServicoLoja.devolver`, `dominio/ItemVenda.restituiAte` | `devolucaoExatamenteNoLimiteEhAceita`, `devolucaoAcimaDaQuantidadeVendidaEhRejeitada` | Devolver 1/2 e tentar devolver mais 2 |
| UC03 / RN06 idempotência de devolução | `ServicoLoja.devolver`, `dominio/Devolucao` | `devolverDuasVezesComMesmoIdNaoDuplicaReembolsoNemEstoque`, `idDeDevolucaoNaoPodeSerReutilizadoComOutrosDados` | Repetir D1 e comparar histórico |
| UC02 / RN07 alternativas de desconto | `politicas/PoliticaDesconto`, `DescontoPercentual`, `DescontoFixo`, `ServicoLoja.distribuirDesconto` | `descontoPercentualCalculaValorCorreto`, `strategyFixoEhExtensaoSemModificarServico`, `rateioDeDescontoEReembolsoConservamCentavos` | Calcular duas políticas para mesmo produto |
| UC02/03 / RN08 transação atômica | `persistencia/RepositorioCsvAtomico.gravarAtomico`, `EstadoDados.copiar` | `falhaAntesDoCommitNaoDeixaVendaParcial`, `falhaAntesDoCommitNaoDeixaDevolucaoParcial`, `reinicioRecuperaTodasInformacoesPersistidas` | Simular falha de commit e abrir novamente |
| UC01/03 / RN09 perfis | `ServicoLoja.autorizar` | `vendedorNaoPodeDevolverNemReporNemCadastrar` | Vendedor tenta devolver; gerente consegue |
| UC01 / RN10 reposição | `ServicoLoja.reporEstoque`, `dominio/Movimentacao` | `reposicaoDuplicadaEhRejeitadaEEstoquePreservado` | Repetir ID de reposição |
| UC01 / RN11 produto válido | `dominio/Produto` e `ServicoLoja.cadastrarProduto` | `produtoExigeIdUnicoPrecoPositivoESaldoNaoNegativo` | Tentar produto com preço negativo |
| UC01 / Observer | `observadores/*`, `ServicoLoja.publicarDepoisDoCommit` | `observerSoRecebeEventoAposCommitEAlertaEstoqueMinimo` | Baixar saldo até mínimo e mostrar alerta |
| UC02 / Strategy | `politicas/*` | `strategyFixoEhExtensaoSemModificarServico` | Mostrar desconto percentual x fixo |
| UC04 / Facade | `servicos/LojaFacade` | `facadeEncaminhaFluxoCompletoParaOsServicos` | Executar fluxo a partir de App/Facade |
| UC02 / RN07 extensão progressiva | `politicas/DescontoPorFaixa`, `politicas/fabricas/CriadorDescontoPorFaixa` | PE `descontoProgressivoRespeitaFronteirasDasFaixas`, PE `extensaoProgressivaFuncionaNoServicoSemAlterarSeuContrato` | Tela Nova venda → “Progressivo”: R$ 300,00 dá R$ 15,00 |
| UC02 / Factory Method | `politicas/fabricas/CriadorPoliticaDesconto.criar/fabricar`, `PaginaNovaVenda.abrirVenda`, `ConsoleView.abrirVenda` | PE `factoryMethodCadaCriadorConcretoProduzSuaPolitica`, PE `factoryMethodPermiteNovoCriadorSobrescrevendoApenasFabricar`, PE `factoryMethodRejeitaParametroInvalidoSemCriarVenda` | Trocar política no combo sem mudar a tela |
| RN09 / login dos perfis | `seguranca/ServicoAutenticacao.entrar`, `ContasDemo` | PE `autenticacaoDevolvePerfilCorretoERejeitaSenhaErrada`, PE `consoleFazLoginEAbreVendaUsandoFactoryMethodPelaFacade` | Entrar como vendedora e tentar repor estoque |
| UC01/UC04 / alerta de estoque | `dominio/Produto.emAlerta`, `ServicoLoja.consultarProdutosEmAlerta` | PE `produtosEmAlertaIncluemSaldoIgualAoMinimo` | Painel mostra “Estoque baixo” |
| UC04 / Adapter | `relatorios/AdaptadorRelatorio`, `ExportadorAntigo` | `adapterExportaRelatorioParaFormatoLegado` | Mostrar CSV exportado |

> As regras RN01 a RN11 têm cobertura direta no conjunto de testes. A equipe deve verificar novamente a matriz após qualquer alteração no código.
