# Validação, cenários e evidências

## Como executar os testes oficiais

Java 21, Maven 3.9 ou compatível; na raiz:

```bash
mvn clean test
```

Os testes estão em `src/test/java/br/ufpb/aps/grupo12/ServicoLojaTest.java`. Cada teste utiliza `@TempDir`, estado inicial determinístico e arquivos independentes. Não dependem de rede, credenciais ou serviços externos.

## Validação local realizada (26/11/2026)

- **Compilação:** todo o código (`src/main` e `src/test`) compilado com `javac` do JDK 21.
- **Execução dos 36 cenários:** **36 passaram, 0 falharam** (27 em `ServicoLojaTest` + 9 em `PadroesEExtensaoTest`). O ambiente de preparação não tinha acesso à internet para o Maven baixar o JUnit; por isso a execução usou um *runner* local que reproduz `@Test`, `@BeforeEach`, `@TempDir` e as asserções usadas.
- **Pendente para a equipe:** rodar `mvn clean test` na máquina de vocês (com internet) e anexar `target/surefire-reports/` à entrega. Não afirmar que o Maven executou antes de rodar.
- **Interface gráfica:** todas as telas foram abertas com dados de demonstração e capturadas em `docs/telas/`.

## Casos para demonstração com dados fictícios

1. **Reiniciar:** `mvn exec:java -Dexec.args="--reset-demo"`; depois `mvn exec:java` e entrar como `gerente@stockflow.demo` / `stockflow2026` (no console: `--console`).
2. **Sucesso:** abrir venda `V1`, produto `BOLSA01`, quantidade `2`, política percentual `10`; confirmar `V1`; estoque de `BOLSA01` cai de `10` para `8`, e o total cai de R$ 300,00 para R$ 270,00.
3. **Violação de regra:** tentar abrir `V2` com `BOLSA02`, quantidade `7` e desconto zero; ao confirmar `V2`, rejeitar falta de estoque (há 6 inicialmente). A venda permanece pendente e o estoque não muda.
4. **Repetição:** confirmar `V1` novamente; consultar histórico, mostrando só uma saída para a venda `V1`.
5. **Devolução:** registrar `D1` de 1 unidade `BOLSA01` vinculada à `V1`; estoque sobe de 8 para 9; reembolso proporcional líquido R$ 135,00. Repetir `D1`: saldo, receita e histórico não mudam. Tentar outra devolução `D2` de 2 unidades da mesma venda/produto: rejeitar (resta só 1).
6. **Persistência:** fechar com `0`; reabrir o programa e consultar o saldo e histórico; recuperar `9` para `BOLSA01` e as movimentações.
7. **Perfil:** clicar em *Sair*, entrar como `karol@stockflow.demo` e tentar *Repor estoque* ou *Registrar devolução*: a tela mostra “Perfil não tem autorização…”, mensagem vinda do `ServicoLoja`.
8. **Strategy + Factory Method:** abrir três vendas de 2 × BOLSA01 (R$ 300,00) trocando só o combo de política: percentual 10% → R$ 270,00; fixo R$ 23,50 → R$ 276,50; progressivo → R$ 285,00. Mostrar que `DescontoPorFaixa` + `CriadorDescontoPorFaixa` foram acrescentados sem mudar serviço nem tela.
9. **Adapter/Facade:** abrir opção de relatório para exportação legado simulado e demonstrar a chamada pela facade.

## Falha de transação, sem estado parcial

O `RepositorioCsvAtomico` aceita `InterceptorCommit` para testes; ele pode lançar exceção **depois da gravação de `.tmp` e antes da troca atômica**. O snapshot anterior permanece válido. Testes: `falhaAntesDoCommitNaoDeixaVendaParcial` e `falhaAntesDoCommitNaoDeixaDevolucaoParcial`.

## Duas mutações intencionais exigidas pelo professor

Executadas em **cópia temporária** do projeto (a versão entregue não contém as mutações). Resultado completo em `evidencias-mutacoes-local.txt`.

### Mutação 1 — remover a idempotência da confirmação

`ServicoLoja.confirmarVenda`: linha `if (v.estado() == EstadoVenda.CONFIRMADA) return new Confirmacao(v, List.of());` comentada.
**Resultado:** 34 passaram, **2 falharam** — `confirmarDuasVezesNaoBaixaEstoqueNovamente` e `observerSoRecebeEventoAposCommitEAlertaEstoqueMinimo` (a segunda confirmação passa a cair em `comEstado` e é rejeitada).
**Restaurado:** 36/36 passando.

### Mutação 2 — permitir devolução de venda não confirmada

`ServicoLoja.devolver`: condição trocada para `if (false && v.estado() != EstadoVenda.CONFIRMADA)`.
**Resultado:** 35 passaram, **1 falhou** — `devolucaoDeVendaPendenteEhRejeitada` (“nenhuma exceção lançada”).
**Restaurado:** 36/36 passando.

Para repetir com Maven, na cópia temporária:

```bash
mvn -Dtest=ServicoLojaTest#confirmarDuasVezesNaoBaixaEstoqueNovamente test
mvn -Dtest=ServicoLojaTest#devolucaoDeVendaPendenteEhRejeitada test
```

## Revisão crítica — exemplo para explicar na defesa

**Problema da proposta inicial:** calcular o desconto total sem distribuí-lo por item não permite restituir corretamente centavos quando há devoluções parciais e diferentes produtos.  
**Melhoria implementada:** `ServicoLoja.distribuirDesconto` rateia o desconto por **maior resto** preservando soma exata; `ItemVenda.restituiAte` calcula restituição cumulativa para impedir centavos duplicados/perdidos.  
**Evidência:** `rateioDeDescontoEReembolsoConservamCentavos` testa três itens de um centavo com desconto total de dois centavos; após devolver todos, líquido é exatamente zero.  
**Complemento necessário:** no relatório final, registrar o diff do código antes/depois se a equipe desenvolver uma versão anterior concreta.

## Limitações e pendências reais

- Executar `mvn clean test` **na máquina da equipe** e anexar os relatórios Surefire.
- (Opcional) Repetir as duas mutações com Maven e tirar print para os slides.
- Completar a comparação com a modelagem de dados do Notion (Toda Chick).
- Verificar comportamento de `ATOMIC_MOVE` no sistema operacional e volume escolhidos para apresentação.
