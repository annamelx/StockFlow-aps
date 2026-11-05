# Primeira apresentação parcial — 12/11/2026 (até 5 min)

> O professor exige: visão geral, funcionalidades, classes, arquitetura preliminar e justificativas. Este é o **roteiro de apresentação**, não uma afirmação de que os slides já foram entregues.

## Slide 1 — Abertura (20 s) — Anna Karolina

**Título:** Sistema de Gestão de Estoque e Vendas  
**Subtítulo:** Projeto APS 2026.2 — Grupo 12 — UFPB Campus IV  
**Fala sugerida:** "Nosso projeto resolve um problema comum em uma loja: garantir que o estoque, as vendas e as devoluções permaneçam coerentes, inclusive em tentativas repetidas ou falhas durante uma operação."

## Slide 2 — Problema e objetivos (45 s) — Anna Karolina

- Uma venda precisa baixar o estoque exatamente uma vez.
- Uma devolução só pode referir-se ao que foi vendido.
- Descontos e reembolsos precisam ser consistentes em centavos.
- Perfil gerente e vendedor possuem funções diferentes.

**Fala:** "O foco é garantir a correção das regras de negócio, não apenas criar telas de cadastro."

## Slide 3 — Quatro casos de uso (65 s) — Awanna Costa

1. Gerenciar produtos, saldo e reposições.
2. Abrir e confirmar vendas; cancelar apenas as pendentes.
3. Registrar devolução vinculada à venda confirmada.
4. Consultar histórico e gerar relatório.

**Detalhe didático:** destacar **UC02 e UC03** como fluxos que vão além de CRUD.

## Slide 4 — Classes e estados (75 s) — Awanna Costa

**Classes do domínio:** `Produto`, `Venda`, `ItemVenda`, `Devolucao`, `Movimentacao`, `EstadoVenda`, `Perfil`.  
**Estados:** `PENDENTE` → `CONFIRMADA` / `CANCELADA`.  
**Fala:** "As entidades guardam os dados; o serviço garante que a venda não saia de um estado inválido e que as quantidades sejam respeitadas."  
**Visual:** exportar `docs/diagramas/classes.puml` (simplificar para slide) e usar estado de `docs/diagramas/fluxos.md`.

## Slide 5 — Arquitetura e decisões (65 s) — Cynthia Souza

**Fluxo:** Console → Facade → Serviço → Contrato Repository → CSV atômico.  
**Padrões previstos/implementados no projeto-base:** Strategy (descontos), Observer (alerta), Facade (interface), Adapter (relatório legado simulado).  
**Fala:** "Se a confirmação falhar antes do salvamento, a transação não deixa o estoque diferente da venda. Temos também uma política de desconto intercambiável sem alterar o serviço."

## Slide 6 — Próximas etapas e encerramento (30 s) — Cynthia Souza

- Revalidar com a documentação **Toda Chick Bolsas & Cia.**
- Revisar UML, executar os testes JUnit com Maven, simular defeitos.
- Preparar a segunda parcial e demonstrar persistência e repetição.

**Fechamento:** "Na próxima etapa, demonstraremos não apenas o fluxo bem-sucedido, mas também como o sistema impede operações inválidas e preserva os dados em caso de falha."

## Preparação e responsabilidades

- **Anna:** explicar problema, regras e validar dados da loja-referência.
- **Awanna:** mostrar casos de uso, estados e diagrama de classes.
- **Cynthia:** arquitetura, persistência, padrões e próximos testes.

Esta é somente uma **sugestão de divisão de apresentação**; vocês podem redistribuir tarefas. Cada uma deve saber explicar um fluxo completo, persistência e teste correspondente, pois a defesa final será individual.
