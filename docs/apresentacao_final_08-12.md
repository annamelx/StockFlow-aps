# Roteiro da apresentação final — Grupo 12 — 08/12/2026

**15 min de apresentação e 10 min de perguntas** (orientações do professor).

| Tempo | Conteúdo | Evidência |
|---|---|---|
| 0:00–1:30 | Problema, escopo e regras mais críticas | Tabela de casos e regras |
| 1:30–3:30 | Arquitetura, classes UML, fluxo por camadas | `docs/diagramas/classes.puml` |
| 3:30–5:30 | Quatro padrões GoF, alternativa e extensão Strategy | Código das políticas, Observer, Facade, Adapter |
| 5:30–10:30 | Demo: venda confirmada, falha estoque, repetição, devolução, perfis, reinício | Console com `--reset-demo` e dados fictícios |
| 10:30–12:30 | Testes JUnit e simulação dos dois defeitos | Relatório Maven, comparação de mutações |
| 12:30–14:00 | Revisão crítica antes/depois e uso de IA | Diff, teste de centavos, três interações revisadas |
| 14:00–15:00 | Limitações, conclusão e fonte de requisitos anterior | README + requisitos + limitações |

**Perguntas individuais para treinar:** Onde a autorização é verificada? Como impede confirmar duas vezes? Qual é a fronteira de devolução? O que acontece com venda e estoque se o arquivo falhar? Que código muda para novo desconto? O que o Adapter adapta? O que muda quando o produto tem quantidade zero? Como testar uma nova regra? Por que não usar `double` para dinheiro?

**Obrigação adicional:** formular pelo menos uma pergunta relevante a outra equipe, por exemplo: "Como vocês garantem que uma falha entre atualizar dois dados relacionados não deixa o sistema inconsistente? Qual teste demonstra isso?"
