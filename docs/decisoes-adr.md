# Registros de decisão arquitetural (ADRs)

## ADR-001 — Arquitetura em camadas e injeção por construtor

- **Contexto:** aplicação deve permitir explicação do fluxo entre tela, serviços, domínio e persistência, sem regras no `Main`.
- **Opções:** um único `Main` com regras; MVC com banco; camadas com interface console.
- **Decisão:** separar `aplicacao`, `servicos`, `dominio`, `persistencia`, demais componentes; injetar `Repositorio` em `ServicoLoja` e expor `LojaFacade` à interface.
- **Consequências:** mais classes, porém regras testáveis sem abrir console e persistência substituível.
- **Evidência:** `App.java`, `LojaFacade.java`, `ServicoLoja.java`; teste `facadeEncaminhaFluxoCompletoParaOsServicos`.

## ADR-002 — Persistência CSV com snapshot e atomicidade

- **Contexto:** confirmar venda modifica estado e estoque, devolução modifica estoque, histórico e valores; não pode ficar dado parcial.
- **Opções:** uma lista em memória (inadequada); múltiplos CSVs independentes (risco de inconsistência); SQLite (transação nativa); snapshot em um único CSV.
- **Decisão:** snapshot único com cópia, lock local, temporário e `ATOMIC_MOVE`.
- **Consequências:** implementável em Java padrão e executável sem banco instalado, mas regrava o arquivo inteiro, depende do sistema de arquivos local e não é arquitetura de produção distribuída.
- **Atualização (v2):** o mesmo snapshot passou a guardar clientes, fornecedores, categorias, compras, contas, usuárias e configuração, com leitura compatível da versão 1. Optou-se por **não** trocar para banco a poucos dias da entrega: o mecanismo de tudo-ou-nada já testado cobre as novas operações sem código novo de transação.
- **Evidência:** `RepositorioCsvAtomico.gravarAtomico`, `RepositorioCsvAtomico.executar`; testes `reinicioRecuperaTodasInformacoesPersistidas`, `falhaAntesDoCommitNaoDeixaVendaParcial`, `falhaAntesDoCommitNaoDeixaDevolucaoParcial`.

## ADR-003 — Padrões GoF: Factory Method, Strategy, Observer, Facade, Adapter

- **Contexto:** variar política de desconto, emitir alertas e adaptar a apresentação sem acoplar domínio à interface ou ao legado simulado.
- **Opções:** múltiplos `if/else` nos serviços; herança indiscriminada; padrões adequados a mudanças concretas.
- **Decisão:** Strategy para desconto; Factory Method para criar a política escolhida na tela sem `if/else` (criação delegada a `fabricar()` sobrescrito); Observer para alertas pós-commit; Facade para simplificação da interface; Adapter para exportação legada simulada. Singleton foi descartado: as dependências são criadas uma vez no `App` e injetadas, o que mantém os testes isolados.
- **Consequências:** mais abstrações para um sistema pequeno, mas extensionabilidade demonstrável e testes isolados; não utilizar Singleton desnecessariamente.
- **Evidência:** classes nos pacotes `politicas`, `politicas/fabricas`, `observadores`, `servicos/LojaFacade`, `relatorios`; testes em `ServicoLojaTest` e `PadroesEExtensaoTest`; extensão `DescontoPorFaixa`.

## ADR-004 — Testes de regras, falhas e recuperação

- **Contexto:** o professor exige no mínimo 12 cenários, fronteiras, idempotência, falhas, duas mutações e validação de padrões.
- **Opções:** só teste manual da interface; só testar entidades triviais; JUnit com entradas determinísticas e diretório temporário por teste.
- **Decisão:** testes JUnit 5 em `@TempDir`; cenários de sucesso/falha por regras; simulação de falha antes do `ATOMIC_MOVE` por interceptor; documentação de duas mutações controladas.
- **Consequências:** testes mais extensos, porém reproduzíveis e independentes de contas/serviços pagos.
- **Evidência:** `src/test/java/.../ServicoLojaTest.java`; `docs/validacao.md`.

## ADR-005 — Interface gráfica Swing sobre a Facade

- **Contexto:** o professor sugeriu uma interface além do console; ela não pode concentrar regras nem acessar persistência.
- **Opções:** manter só console; aplicação web (exigiria servidor e mais dependências); JavaFX (dependência externa e configuração de módulos); **Swing**, que já vem no JDK.
- **Decisão:** Swing no pacote `interfacegrafica`, recebendo `LojaFacade`, `CatalogoDescontos` e `AlertaEstoqueMinimo` por construtor (via `Contexto`). O console continua disponível com `--console`, e é usado automaticamente quando não há ambiente gráfico.
- **Consequências:** nenhuma dependência nova no `pom.xml`; a mesma Facade atende às duas interfaces. A aparência depende do sistema operacional; telas não têm teste automatizado próprio — o fluxo View → Facade → Serviço é testado pelo `ConsoleView`.
- **Atualização:** a interface passou a cobrir todo o protótipo de referência (dashboard, PDV, histórico, contas, cadastros, relatórios com gráficos) com identidade visual própria. Os novos serviços (`ServicoCadastros`, `ServicoFinanceiro`, `ServicoRelatorios`) também são acessados só pela Facade.
- **Evidência:** `aplicacao/App.java`, `interfacegrafica/*`, teste `consoleFazLoginEAbreVendaUsandoFactoryMethodPelaFacade`, capturas em `docs/telas/`.
