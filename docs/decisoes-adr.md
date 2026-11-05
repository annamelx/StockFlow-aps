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
- **Evidência:** `RepositorioCsvAtomico.gravarAtomico`, `RepositorioCsvAtomico.executar`; testes `reinicioRecuperaTodasInformacoesPersistidas`, `falhaAntesDoCommitNaoDeixaVendaParcial`, `falhaAntesDoCommitNaoDeixaDevolucaoParcial`.

## ADR-003 — Quatro padrões GoF: Strategy, Observer, Facade, Adapter

- **Contexto:** variar política de desconto, emitir alertas e adaptar a apresentação sem acoplar domínio à interface ou ao legado simulado.
- **Opções:** múltiplos `if/else` nos serviços; herança indiscriminada; padrões adequados a mudanças concretas.
- **Decisão:** Strategy para desconto; Observer para alertas pós-commit; Facade para simplificação da interface; Adapter para exportação legada simulada.
- **Consequências:** mais abstrações para um sistema pequeno, mas extensionabilidade demonstrável e testes isolados; não utilizar Singleton desnecessariamente.
- **Evidência:** classes nos pacotes `politicas`, `observadores`, `servicos/LojaFacade`, `relatorios` e testes relacionados no `ServicoLojaTest`.

## ADR-004 — Testes de regras, falhas e recuperação

- **Contexto:** o professor exige no mínimo 12 cenários, fronteiras, idempotência, falhas, duas mutações e validação de padrões.
- **Opções:** só teste manual da interface; só testar entidades triviais; JUnit com entradas determinísticas e diretório temporário por teste.
- **Decisão:** testes JUnit 5 em `@TempDir`; cenários de sucesso/falha por regras; simulação de falha antes do `ATOMIC_MOVE` por interceptor; documentação de duas mutações controladas.
- **Consequências:** testes mais extensos, porém reproduzíveis e independentes de contas/serviços pagos.
- **Evidência:** `src/test/java/.../ServicoLojaTest.java`; `docs/validacao.md`.
