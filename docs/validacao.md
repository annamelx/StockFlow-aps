# Validação, cenários e evidências

## Como executar os testes oficiais

Java 21, Maven 3.9 ou compatível; na raiz:

```bash
mvn clean test
```

Os testes estão em `src/test/java/br/ufpb/aps/grupo12/ServicoLojaTest.java`. Cada teste utiliza `@TempDir`, estado inicial determinístico e arquivos independentes. Não dependem de rede, credenciais ou serviços externos.

## Validação local realizada na preparação desta versão

- **Compilação da implementação Java:** executada com `javac -encoding UTF-8 ...` e concluída.
- **Verificação funcional dos testes:** 27 métodos de testes foram executados em um **harness temporário de compatibilidade** que reproduziu asserções e ciclo `@BeforeEach` / `@TempDir`: **27 passaram, 0 falharam**.
- **Importante:** este harness **não é a execução oficial pelo JUnit/Maven**; Maven não estava instalado no ambiente de preparação. A equipe precisa executar `mvn clean test` em seu computador e anexar os relatórios de `target/surefire-reports` à entrega final. Não substituir este registro pela afirmação de que o Maven já executou.

## Casos para demonstração com dados fictícios

1. **Reiniciar:** `mvn exec:java -Dexec.args="--reset-demo"`; depois `mvn exec:java` e selecionar gerente (`1`).
2. **Sucesso:** abrir venda `V1`, produto `BOLSA01`, quantidade `2`, política percentual `10`; confirmar `V1`; estoque de `BOLSA01` cai de `10` para `8`, e o total cai de R$ 300,00 para R$ 270,00.
3. **Violação de regra:** tentar abrir `V2` com `BOLSA02`, quantidade `7` e desconto zero; ao confirmar `V2`, rejeitar falta de estoque (há 6 inicialmente). A venda permanece pendente e o estoque não muda.
4. **Repetição:** confirmar `V1` novamente; consultar histórico, mostrando só uma saída para a venda `V1`.
5. **Devolução:** registrar `D1` de 1 unidade `BOLSA01` vinculada à `V1`; estoque sobe de 8 para 9; reembolso proporcional líquido R$ 135,00. Repetir `D1`: saldo, receita e histórico não mudam. Tentar outra devolução `D2` de 2 unidades da mesma venda/produto: rejeitar (resta só 1).
6. **Persistência:** fechar com `0`; reabrir o programa e consultar o saldo e histórico; recuperar `9` para `BOLSA01` e as movimentações.
7. **Perfil:** reiniciar programa, selecionar vendedor (`2`), tentar reposição ou devolução: acesso negado pelo serviço.
8. **Strategy:** depois de reset, abrir venda com percentual e outra com valor fixo (`centavos`), comparar totais; mostrar `DescontoFixo` como extensão.
9. **Adapter/Facade:** abrir opção de relatório para exportação legado simulado e demonstrar a chamada pela facade.

## Falha de transação, sem estado parcial

O `RepositorioCsvAtomico` aceita `InterceptorCommit` para testes; ele pode lançar exceção **depois da gravação de `.tmp` e antes da troca atômica**. O snapshot anterior permanece válido. Testes: `falhaAntesDoCommitNaoDeixaVendaParcial` e `falhaAntesDoCommitNaoDeixaDevolucaoParcial`.

## Duas mutações intencionais exigidas pelo professor

Execute em **cópia descartável ou branch temporária**, nunca na entrega final. Registre screenshot/saída de cada execução e o diff.

### Mutação 1 — violar idempotência da confirmação

Arquivo `ServicoLoja.java` em `confirmarVenda`: remova **temporariamente** o retorno antecipado da condição `if (v.estado() == EstadoVenda.CONFIRMADA) ...`.  
**Teste-alvo:** `confirmarDuasVezesNaoBaixaEstoqueNovamente` (deve falhar porque o serviço passa a rejeitar indevidamente repetição de confirmação).  
**Restauração:** reponha o retorno; o teste volta a passar.

### Mutação 2 — permitir devolução de venda não confirmada

Arquivo `ServicoLoja.java` em `devolver`: desabilite **temporariamente** a checagem `if (v.estado() != EstadoVenda.CONFIRMADA)` colocando `false &&` na condição, em **cópia temporária**.  
**Teste-alvo:** `devolucaoDeVendaPendenteEhRejeitada` (deve falhar porque a venda PENDENTE passa a aceitar devolução).  
**Restauração:** remova `false &&`; o teste volta a passar.

Com Maven instalado, rodar individualmente, por exemplo:

```bash
mvn -Dtest=ServicoLojaTest#confirmarDuasVezesNaoBaixaEstoqueNovamente test
mvn -Dtest=ServicoLojaTest#devolucaoDeVendaPendenteEhRejeitada test
```

**Registrar evidências finais:** data, nome do teste, alteração de uma linha, mensagem da falha, código restaurado, execução completa sem falhas. A versão de entrega não conterá as mutações.

## Revisão crítica — exemplo para explicar na defesa

**Problema da proposta inicial:** calcular o desconto total sem distribuí-lo por item não permite restituir corretamente centavos quando há devoluções parciais e diferentes produtos.  
**Melhoria implementada:** `ServicoLoja.distribuirDesconto` rateia o desconto por **maior resto** preservando soma exata; `ItemVenda.restituiAte` calcula restituição cumulativa para impedir centavos duplicados/perdidos.  
**Evidência:** `rateioDeDescontoEReembolsoConservamCentavos` testa três itens de um centavo com desconto total de dois centavos; após devolver todos, líquido é exatamente zero.  
**Complemento necessário:** no relatório final, registrar o diff do código antes/depois se a equipe desenvolver uma versão anterior concreta.

## Limitações e pendências reais

- Executar `mvn clean test` **na máquina da equipe** (não executado neste ambiente).
- Registrar resultados reais das duas mutações intencionais no relatório final.
- Comparar com a documentação anterior *Toda Chick Bolsas & Cia.*, ainda não enviada.
- Verificar comportamento de `ATOMIC_MOVE` no sistema operacional e volume escolhidos para apresentação.
