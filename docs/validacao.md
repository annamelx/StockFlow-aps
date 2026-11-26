# Validação, cenários e evidências

## Como executar os testes oficiais

Java 21, Maven 3.9 ou compatível; na raiz:

```bash
mvn clean test
```

Os testes estão em `src/test/java/br/ufpb/aps/grupo12/ServicoLojaTest.java`. Cada teste utiliza `@TempDir`, estado inicial determinístico e arquivos independentes. Não dependem de rede, credenciais ou serviços externos.

## Validação local realizada (26/11/2026)

- **Compilação:** todo o código (`src/main` e `src/test`) compilado com `javac` do JDK 21.
- **Execução dos 49 cenários:** **49 passaram, 0 falharam** (27 em `ServicoLojaTest`, 9 em `PadroesEExtensaoTest`, 13 em `GestaoCompletaTest`). O ambiente de preparação não tinha acesso à internet para o Maven baixar o JUnit; por isso a execução usou um *runner* local que reproduz `@Test`, `@BeforeEach`, `@TempDir` e as asserções usadas.
- **Pendente para a equipe:** rodar `mvn clean test` na máquina de vocês (com internet) e anexar `target/surefire-reports/` à entrega. Não afirmar que o Maven executou antes de rodar.
- **Interface gráfica:** todas as telas foram abertas com dados de demonstração e capturadas em `docs/telas/`.

## Casos para demonstração com dados fictícios

Os números da venda (V…) são sugeridos automaticamente pelo PDV; use os que aparecerem.

1. **Reiniciar:** `mvn exec:java -Dexec.args="--reset-demo"`; depois `mvn exec:java` e entrar como `gerente@stockflow.demo` / `stockflow2026`. O Dashboard já mostra vendas de hoje, do mês e a meta.
2. **Fluxo principal (PDV):** adicionar 2 × *Bolsa transversal* (BOLSA01, saldo 10), escolher cliente e Pix, desconto Percentual 10 → total R$ 270,00 → **Finalizar**. Em *Produtos* o saldo cai para 8 e o histórico do item mostra a saída.
3. **Violação de regra:** adicionar 7 × *Bolsa de ombro* (saldo 6) e clicar **Finalizar**: a confirmação é recusada por estoque insuficiente e a venda fica PENDENTE, sem baixa.
4. **Repetição:** no *Histórico*, selecionar a venda do passo 2 e clicar **Confirmar** de novo: nada muda (uma única saída em *Movimentações*).
5. **Devolução:** no *Histórico*, selecionar a venda do passo 2 → **Devolver** 1 unidade (ID sugerido D…): reembolso R$ 135,00, saldo volta para 9. Repetir o mesmo ID: sem novo efeito. Tentar devolver mais 2: recusado (resta 1).
6. **Persistência:** fechar a janela, abrir de novo e conferir saldos, vendas, devoluções e contas.
7. **Perfil:** *Sair* → entrar como `karol@stockflow.demo`. O menu mostra só PDV, Histórico, Contas e Clientes; ao tentar **Devolver** ou **Quitar** uma conta, aparece “Perfil não tem autorização…”, mensagem vinda do serviço.
8. **Strategy + Factory Method:** no PDV, com 2 × BOLSA01 (R$ 300,00), trocar só o combo de desconto: percentual 10% → R$ 270,00; valor fixo 23,50 → R$ 276,50; progressivo → R$ 285,00.
9. **Compra atômica:** em *Produtos → Registrar compra*, escolher fornecedor, quantidade e custo: estoque, custo do produto, movimentação e relatório de compras mudam juntos.
10. **Consistência de cadastros:** tentar excluir um cliente que já comprou → recusado. Tentar excluir uma categoria com produtos → recusado.
11. **Adapter:** *Relatórios → Estoque* mostra a exportação no formato do sistema legado.

## Falha de transação, sem estado parcial

O `RepositorioCsvAtomico` aceita `InterceptorCommit` para testes; ele pode lançar exceção **depois da gravação de `.tmp` e antes da troca atômica**. O snapshot anterior permanece válido. Testes: `falhaAntesDoCommitNaoDeixaVendaParcial` e `falhaAntesDoCommitNaoDeixaDevolucaoParcial`.

## Duas mutações intencionais exigidas pelo professor

Executadas em **cópia temporária** do projeto (a versão entregue não contém as mutações). Resultado completo em `evidencias-mutacoes-local.txt`.

### Mutação 1 — remover a idempotência da confirmação

`ServicoLoja.confirmarVenda`: linha `if (v.estado() == EstadoVenda.CONFIRMADA) return new Confirmacao(v, List.of());` comentada.
**Resultado:** 47 passaram, **2 falharam** — `confirmarDuasVezesNaoBaixaEstoqueNovamente` e `observerSoRecebeEventoAposCommitEAlertaEstoqueMinimo` (a segunda confirmação passa a cair em `comEstado` e é rejeitada).
**Restaurado:** 49/49 passando.

### Mutação 2 — permitir devolução de venda não confirmada

`ServicoLoja.devolver`: condição trocada para `if (false && v.estado() != EstadoVenda.CONFIRMADA)`.
**Resultado:** 48 passaram, **1 falhou** — `devolucaoDeVendaPendenteEhRejeitada` (“nenhuma exceção lançada”).
**Restaurado:** 49/49 passando.

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
