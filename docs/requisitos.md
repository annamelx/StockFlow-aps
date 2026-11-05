# Especificação de requisitos — Grupo 12

**UFPB – Campus IV | APS 2026.2 | Professor Dr. Dorgival Netto**  
**Tema atribuído:** Sistema de Gestão de Estoque e Vendas  
**Integrantes:** Anna Karolina, Awanna Costa, Cynthia Souza

> **Proveniência e revisão:** os requisitos obrigatórios vêm de *Orientações Projeto APS 2026.2*, seção 3 e anexo **Grupo 12** na página 9. O professor indica **Toda Chick Bolsas & Cia.** como documentação de referência, mas seu conteúdo não foi fornecido para esta versão. As escolhas detalhadas abaixo (políticas, limites, valores, modelo e interface) são **decisões propostas pela equipe**, a serem confrontadas com a referência quando estiver disponível. **Não se atribui à documentação Toda Chick nenhum requisito ainda não verificado.**

## 1. Problema, objetivo e fronteira

Uma loja precisa manter o saldo de produtos coerente com compras e devoluções. O sistema acadêmico oferece registro de produtos, reposição, abertura e confirmação de vendas, cancelamento de vendas pendentes, devolução vinculada à venda confirmada e consulta de histórico/relatório. Requisitos críticos: não vender além do saldo, não baixar estoque duas vezes, não devolver acima do vendido, manter centavos e estoque consistentes mesmo em falhas.

**Fora do escopo atual:** autenticação por senha, pagamento real, nota fiscal, múltiplas lojas, emissão fiscal, integração bancária, interface gráfica ou web, controle de concorrência distribuída entre diferentes máquinas e documentos não fornecidos. A seleção do perfil é acadêmica, sem autenticação real.

## 2. Perfis e autorização

| Ação | GERENTE | VENDEDOR |
|---|:---:|:---:|
| Consultar produtos, histórico, vendas e receita | ✓ | ✓ |
| Abrir, confirmar ou cancelar venda pendente | ✓ | ✓ |
| Cadastrar produto e repor estoque | ✓ | — |
| Registrar devolução | ✓ | — |

As permissões são verificadas em `ServicoLoja.autorizar()`, não apenas no console.

## 3. Casos de uso e critérios de aceitação

### UC01 — Gerenciar produtos e estoque

**Ator:** gerente (cadastro/reposição); gerente ou vendedor (consulta).  
**Pré-condições:** perfil autorizado; produto com ID único, nome, preço positivo, saldo e mínimo não negativos.  
**Principal:** (1) gerente informa dados; (2) serviço valida e persiste produto; (3) gerente registra reposição com ID único; (4) serviço aumenta saldo e grava movimentação na mesma transação; (5) observadores recebem evento após a gravação.  
**Alternativos/erros:** produto duplicado; saldo inválido; quantidade de reposição ≤0; repetição do mesmo ID de reposição; vendedor tenta operação restrita.  
**Pós-condições:** produto e eventuais movimentações recuperáveis ao reabrir.  
**Aceitação:** incremento correto, rejeição de quantidade inválida e de ID repetido, bloqueio por perfil, persistência real.

### UC02 — Abrir, confirmar e cancelar venda

**Ator:** gerente ou vendedor.  
**Pré-condições:** produtos existentes, quantidades positivas, ID de venda único, política de desconto válida.  
**Principal:** (1) usuário informa itens e política; (2) serviço cria venda `PENDENTE` com preço congelado por item e rateio em centavos; (3) usuário confirma; (4) serviço verifica **todos** os saldos; (5) serviço atualiza todos os produtos, gera movimentos de saída e altera a venda para `CONFIRMADA` **na mesma transação**; (6) observa estoque após o commit.  
**Alternativos/erros:** não há estoque suficiente (mantém `PENDENTE`, sem baixa); venda `PENDENTE` pode ser `CANCELADA` sem baixar; `CANCELADA` não pode ser confirmada; confirmar `CONFIRMADA` novamente não gera novo efeito; desconto inválido é rejeitado.  
**Aceitação:** apenas uma baixa por venda; saldo nunca negativo; transição válida; `subtotal − desconto = total`; um único snapshot em caso de falha.

### UC03 — Registrar devolução da venda

**Ator:** gerente.  
**Pré-condições:** venda `CONFIRMADA`, produto realmente presente na venda e quantidade positiva dentro do saldo ainda não devolvido para aquele item.  
**Principal:** (1) gerente informa ID único da devolução, venda, produto e quantidade; (2) serviço verifica o histórico de devoluções do item; (3) calcula reembolso proporcional ao **valor líquido do item**, incluindo o desconto da venda, distribuindo os centavos ao longo das devoluções parciais; (4) registra devolução, reembolso e movimentação, além de repor estoque, em uma única transação.  
**Alternativos/erros:** venda não confirmada; produto ausente na venda; quantidade acima do restante; perfil sem permissão; ID de devolução repetido com dados diferentes. Mesmo ID e dados da devolução repetidos devolvem o registro original **sem repor novamente**.  
**Aceitação:** `soma(qtd devolvida) ≤ qtd vendida`; soma das restituições totais nunca supera o líquido do item; processamento idempotente; sem estado parcial.

### UC04 — Consultar histórico, saldos e relatório

**Ator:** gerente ou vendedor.  
**Pré-condições:** perfil autorizado.  
**Principal:** serviço consulta produtos, vendas, devoluções e movimentações persistidas; a `LojaFacade` fornece um ponto de acesso; o `AdaptadorRelatorio` exporta produtos para API legada simulada em CSV com cabeçalho `CODIGO;PRODUTO;QUANTIDADE`. A receita líquida computada corresponde ao total de **vendas confirmadas menos reembolsos registrados**.  
**Aceitação:** informações persistidas sobrevivem a reinício; relatório corresponde ao saldo atual; demonstrar Facade e Adapter com testes.

## 4. Regras de negócio

| ID | Regra e limites implementados | Tipo |
|---|---|---|
| RN01 | Não confirmar venda se **qualquer** item não tiver estoque suficiente; saldo não pode ficar negativo. | Consistência/fronteira |
| RN02 | Confirmar venda já `CONFIRMADA` é **idempotente** (não desconta de novo). | Duplicidade |
| RN03 | Venda só pode mudar de `PENDENTE` para `CONFIRMADA` ou `CANCELADA`. | Estado |
| RN04 | Devolução exige venda `CONFIRMADA` e item vendido naquela venda. | Vínculo |
| RN05 | Soma das devoluções de cada item não ultrapassa sua quantidade vendida. | Fronteira |
| RN06 | Reprocessar mesma devolução pelo ID não gera segundo estoque ou segundo reembolso; ID reutilizado para dados diferentes é rejeitado. | Idempotência |
| RN07 | Política de desconto em duas opções (**percentual de 0 a 100%** ou **valor fixo de 0 ao subtotal**), total nunca negativo. | Política variável |
| RN08 | Venda, estoque e movimentações; e devolução, estoque, valores e movimentações são gravados **tudo ou nada**. | Integridade |
| RN09 | Gerente e vendedor têm permissões distintas, verificadas no serviço. | Autorização |
| RN10 | Reposição requer quantidade positiva e identificador de lançamento não repetido. | Integridade |
| RN11 | Produto tem preço positivo, saldos não negativos e ID único. | Cadastro |

### Política de arredondamento proposta

Armazenamento de valores em **centavos (`long`)**, evitando `double` para dinheiro. Cálculo percentual com `BigDecimal` arredondado para centavo (`HALF_UP`). Distribuição de desconto entre itens por **maior resto**, garantindo que a soma dos descontos por item seja o desconto total. Em devolução parcial de um item, a restituição incremental é a diferença entre dois valores cumulativos com divisão inteira; a soma de todas as devoluções equivale exatamente ao preço **líquido** do item.

## 5. Requisitos não funcionais

- RNF01: Java 21; Maven; testes com JUnit Jupiter.
- RNF02: arquitetura de domínio, serviços, contrato de persistência, adaptadores e console.
- RNF03: persistência em um arquivo CSV-snapshot, com troca atômica (dependente de suporte do sistema de arquivos a `ATOMIC_MOVE`).
- RNF04: os dados não se perdem ao reiniciar; `--reset-demo` recompõe dados fictícios conhecidos.
- RNF05: não exige conta pessoal, serviço pago nem API externa.
- RNF06: erros esperados produzem mensagens compreensíveis; falhas de armazenamento não são ocultadas.

## 6. Pontos de revisão com a equipe

1. Localizar e anexar a documentação **Toda Chick Bolsas & Cia.**; preencher a tabela "mantido/alterado/adicionado" comparando os requisitos reais.
2. Confirmar a decisão didática de permitir somente **gerente** efetuar devoluções.
3. Confirmar que produtos devolvidos **voltam ao estoque vendável** imediatamente. O professor não especifica triagem de qualidade.
4. Definir se haverá devolução de dinheiro simulado, crédito na loja ou estorno contábil; esta versão modela somente o **valor de reembolso registrado**, não um pagamento externo.
5. Confirmar se a interface em console atende à preferência da turma; ela é explicitamente permitida pelo professor.
