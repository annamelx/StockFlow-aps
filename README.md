<div align="center">

# 📦 StockFlow — Sistema de Gestão de Estoque e Vendas

### 💜 APS • Grupo 12 • UFPB Campus IV • 2026.2

<img alt="Java 21" src="https://img.shields.io/badge/Java-21-7C3AED?style=for-the-badge&logo=openjdk&logoColor=white" />
<img alt="Maven" src="https://img.shields.io/badge/Maven-Projeto-EC4899?style=for-the-badge&logo=apachemaven&logoColor=white" />
<img alt="JUnit 5" src="https://img.shields.io/badge/Testes-JUnit%205-A855F7?style=for-the-badge" />
<img alt="Situação: versão final" src="https://img.shields.io/badge/Status-Vers%C3%A3o%20final-6D28D9?style=for-the-badge" />

**Vendas, descontos, devoluções e estoque consistente — inclusive quando uma operação falha.**

[✨ Funcionalidades](#-funcionalidades) · [🏗️ Arquitetura](#%EF%B8%8F-arquitetura) · [🚀 Executar](#-executando-o-projeto) · [🧪 Testar](#-testes) · [👩‍💻 Equipe](#-equipe)

</div>

---

## 💜 O projeto

Este projeto foi desenvolvido como base para a **Unidade III de Análise e Projeto de Sistemas**, com orientação do **Prof. Dr. Dorgival Netto**, da **Universidade Federal da Paraíba — Campus IV**.

O desafio do **Grupo 12** é implementar **venda, baixa de estoque e devolução vinculada aos itens realmente vendidos**, impedindo devoluções acima da quantidade vendida ou processadas duas vezes. Também é necessário variar uma política de desconto e manter produtos, valores e movimentos consistentes quando ocorrer falha de persistência.

O tema foi definido pelo professor e utiliza como referência a documentação **Toda Chick Bolsas & Cia.** (semestre anterior), composta pela [modelagem de dados no Notion](https://app.notion.com/p/Toda-Chick-Bolsas-Cia-Modelagem-de-Dados-eed393f29c174fe2bb4fe00f53f3147d) e pelo [protótipo de interface no Figma](https://www.figma.com/make/1C28Lge3yXl9R2z64MKwFL/PROJETO-MODELAGEM-DE-DADOS).

**O que foi reaproveitado e o que foi adaptado:**

| Da referência Toda Chick | Neste projeto |
|---|---|
| Protótipo de telas (login, painel com total de produtos e alertas, estoque, vendas, relatórios) | Usado como **base funcional** das telas; a interface Swing tem **identidade visual própria** (StockFlow, azul-marinho/preto e azul) e layout diferente |
| Dois tipos de usuária: proprietária e atendente | Perfis `GERENTE` e `VENDEDOR`; login em `ServicoAutenticacao` com contas fictícias próprias; permissões verificadas no `ServicoLoja` |
| Catálogo de bolsas e acessórios, alerta de estoque baixo | 8 produtos fictícios (2 em alerta), `Produto.emAlerta()` e Observer `AlertaEstoqueMinimo` |
| Registro de vendas e relatórios | **Adaptado ao desafio do Grupo 12**: venda com estados, baixa atômica, devolução vinculada aos itens vendidos, políticas de desconto |

> As regras de devolução, idempotência e integridade **não vêm da documentação anterior**: são exigências do desafio do Grupo 12 nas orientações do professor e foram especificadas pela equipe em [requisitos](docs/requisitos.md).

### 🎯 O que o sistema resolve?

- Evita vender mais unidades do que existem em estoque.
- Garante que confirmar a mesma venda **duas vezes não dê duas baixas**.
- Impede devolução de um produto não vendido ou acima da quantidade comprada.
- Calcula desconto **percentual, fixo ou progressivo por faixa de valor**, além de reembolsos em **centavos inteiros**, sem erros de ponto flutuante.
- Usa **gravação atômica em CSV**: venda, saldo e histórico são substituídos juntos, ou o arquivo anterior permanece intacto.
- Diferencia permissões de **gerente (proprietária) e vendedor (atendente)**, verificadas no serviço.
- Possui **interface gráfica própria (StockFlow)** e, como alternativa, um modo console.

## ✨ Funcionalidades

| Caso de uso | Fluxos da versão inicial |
|:--|:--|
| **UC01 · Produtos e estoque** | Cadastrar, consultar e registrar reposição sem duplicidade de lançamento. |
| **UC02 · Vendas** | Abrir em `PENDENTE`, calcular desconto (percentual, fixo ou progressivo), confirmar com baixa atômica e cancelar pendentes. |
| **UC03 · Devoluções** | Validar venda confirmada, impedir excesso, calcular restituição e repor estoque uma única vez. |
| **UC04 · Consultas e relatórios** | Consultar estoque, movimentos, vendas e receita líquida simulada; exportar relatório legado simulado. |

> As funcionalidades têm regras de sucesso e falha. Não há emissão fiscal nem pagamento real. O login usa **duas contas fictícias de demonstração** (sem cadastro de usuários).

### 🔄 Ciclo de uma venda

<div>
  <img src="./plantuml-diagram.png" alt="">
</div>

### 👥 Permissões

| Operação | Gerente | Vendedor(a) |
|---|:---:|:---:|
| Consultar estoque, movimentações e relatório | ✅ | ✅ |
| Abrir/confirmar/cancelar venda pendente | ✅ | ✅ |
| Cadastrar produto e repor estoque | ✅ | ❌ |
| Registrar devolução | ✅ | ❌ |

**Contas de demonstração** (senha `stockflow2026` para as duas): `gerente@stockflow.demo` (gerente) e `vendedora@stockflow.demo` (vendedora). A tela apenas identifica a usuária; quem **bloqueia** uma operação não permitida é o `ServicoLoja.autorizar()`, inclusive no console.

### 🖥️ Interface gráfica

| Login | Painel |
|---|---|
| <img src="docs/telas/login.png" width="420"> | <img src="docs/telas/painel.png" width="420"> |
| **Nova venda** (políticas via Factory Method) | **Devoluções** |
| <img src="docs/telas/nova-venda.png" width="420"> | <img src="docs/telas/devolucoes.png" width="420"> |

Outras telas: [estoque](docs/telas/estoque.png), [vendas](docs/telas/vendas.png), [movimentações](docs/telas/movimentacoes.png), [relatórios](docs/telas/relatorios.png).

## 🏗️ Arquitetura

Estrutura implementada em **Java 21**, usando arquitetura em camadas (MVC na interface), injeção de dependências por construtor e **cinco padrões GoF nas três categorias**.

<div>
  <img src="./diagrama arquitetura.png" alt="">
</div>

| GoF | Categoria | Uso no sistema |
|---|---|---|
| **Factory Method** | Criação | `CriadorPoliticaDesconto.fabricar()` é sobrescrito por cada criador concreto; as telas escolhem um criador sem `if/else`. |
| **Strategy** | Comportamental | Política de desconto intercambiável: percentual, fixo e **progressivo (extensão)**. |
| **Observer** | Comportamental | Registra alerta quando estoque atinge o mínimo, somente após confirmação da operação. |
| **Facade** | Estrutural | `LojaFacade` é o único ponto de acesso das telas (gráfica e console) aos serviços. |
| **Adapter** | Estrutural | Adapta dados atuais ao exportador legado **simulado**. |

**Extensão demonstrável:** `DescontoPorFaixa` (≥ R$ 300 → 5%; ≥ R$ 500 → 10%) implementa `PoliticaDesconto` e ganhou o criador `CriadorDescontoPorFaixa`. Código **acrescentado**: essas duas classes + testes. Código **alterado**: uma linha em `CatalogoDescontos.padrao()` para registrar o criador. Código **reutilizado sem mudança**: `ServicoLoja`, `Venda`, `ItemVenda`, `PoliticaDesconto`, telas. Detalhes em [arquitetura](docs/arquitetura.md#extensão-do-strategy).

### 🗂️ Organização do código

```text
.
├── pom.xml
├── README.md
├── src/
│   ├── main/java/br/ufpb/aps/grupo12/
│   │   ├── aplicacao/      # App (montagem das dependências) e ConsoleView
│   │   ├── interfacegrafica/ # Telas Swing (View/Controller) do StockFlow
│   │   ├── seguranca/      # Usuario, ContasDemo, ServicoAutenticacao
│   │   ├── dominio/        # Produto, Venda, ItemVenda, Devolucao, Movimentacao
│   │   ├── servicos/       # Casos de uso e Facade
│   │   ├── persistencia/   # Contrato Repositorio e CSV atômico
│   │   ├── politicas/      # Strategy: percentual, fixo, progressivo
│   │   │   └── fabricas/   # Factory Method: criadores de políticas
│   │   ├── observadores/   # Observer de estoque mínimo
│   │   └── relatorios/     # Adapter e exportação legada simulada
│   └── test/java/br/ufpb/aps/grupo12/   # ServicoLojaTest, PadroesEExtensaoTest
├── dados/                 # CSV gerado em execução, ignorado pelo Git
└── docs/                  # UML, regras, rastreabilidade, ADRs, testes, apresentação
```

## 🚀 Executando o projeto

**Pré-requisitos:** JDK 21 e Maven 3.9+ instalados. Não há dependência de banco externo nem de serviços pagos.

No terminal **dentro da pasta do projeto**, execute:

```bash
mvn clean compile
mvn exec:java
```

Abre a **janela gráfica** do StockFlow (tela de login). Para usar o menu de texto:

```bash
mvn exec:java -Dexec.args="--console"
```

Na primeira execução, o programa carrega **8 produtos fictícios** (2 já em estoque baixo). No Windows também há atalhos em `scripts/` (`iniciar-windows.bat`, `iniciar-console-windows.bat`, `reset-demo-windows.bat`, `testar-windows.bat`).

**Resetar para repetir os testes demonstrativos:**

```bash
mvn compile exec:java -Dexec.args="--reset-demo"
mvn exec:java
```

O reset **substitui os dados do arquivo de demonstração** — não execute em um arquivo que contenha dados que deseja preservar. Para comandos de PowerShell ou problemas no IntelliJ IDEA, consulte [validação](docs/validacao.md).

### 🧪 Testes

```bash
mvn clean test
```

Existem **36 cenários JUnit** em duas classes: `ServicoLojaTest` (regras de negócio, fronteiras, perfis, estados, idempotência, persistência real, falhas injetadas de commit, Strategy, Observer, Facade, Adapter) e `PadroesEExtensaoTest` (Factory Method, extensão progressiva com fronteiras R$ 299,99/300,00/499,99/500,00, autenticação, integração console → Facade → serviço). As duas mutações exigidas foram executadas e detectadas — ver [validação](docs/validacao.md).

Os resultados oficiais deverão ser capturados em `target/surefire-reports/`. Para dois defeitos introduzidos **temporariamente**, há um procedimento documentado em [validação e mutações](docs/validacao.md). O código entregue deve permanecer sem alterações intencionalmente incorretas.

## 📚 Documentação para o professor

| Documento | Conteúdo |
|---|---|
| [Especificação de requisitos](docs/requisitos.md) | UC01–UC04, RN01–RN11, perfis, fluxos, políticas, limites e critérios de aceitação. |
| [Arquitetura e padrões GoF](docs/arquitetura.md) | Responsabilidades, transação, alternativas, classes dos cinco padrões e extensão. |
| [Diagrama de classes UML (PlantUML)](docs/diagramas/classes.puml) | Atributos, visibilidades, métodos e relacionamentos. |
| [Fluxos e estados (Mermaid)](docs/diagramas/fluxos.md) | Estados da venda e fluxos de venda e devolução. |
| [Matriz de rastreabilidade](docs/rastreabilidade.md) | Cada regra ligada a métodos, testes e evidências de demonstração. |
| [Decisões arquiteturais (ADR)](docs/decisoes-adr.md) | Contexto, opções, decisão, consequências e evidências. |
| [Validação](docs/validacao.md) | Cenários reproduzíveis, falhas de commit e roteiro de duas mutações. |
| [Uso de IA](docs/uso-de-ia.md) | Declaração de auxílio e tabela de interações significativas a revisar. |
| [Roteiro da 1ª parcial](docs/apresentacao_parcial_12-11.md) | Apresentação de 12/11, com distribuição sugerida de 5 minutos. |
| [Roteiro final](docs/apresentacao_final_08-12.md) | Apresentação de 08/12, com demonstração e possíveis perguntas. |
| [Slides da 1ª parcial](apresentacao_1a_parcial_12-11.pptx) | Apresentação editável (6 slides, roxo e rosa), acompanhada de roteiro. |
| [Telas da interface](docs/telas/) | Capturas da interface gráfica com dados de demonstração. |
| [Guia IntelliJ/GitHub](docs/guia-intellij-github.md) | Importação no Windows, testes, colaboração e push. |
| [Evidência de mutações](docs/evidencias-mutacoes-local.txt) | Dois defeitos introduzidos em cópia temporária, detectados pelos testes e restaurados. |

## ⚠️ Limitações conhecidas

- Login apenas com **duas contas fictícias** fixas (`ContasDemo`); não há cadastro de usuários nem recuperação de senha.
- Os alertas do Observer ficam em memória durante a sessão; o estado do estoque, esse sim, é persistido.
- O CSV atômico depende de `ATOMIC_MOVE` no sistema de arquivos local; não é solução para várias máquinas.
- Reembolso e receita são valores simulados: não há pagamento, estorno real nem nota fiscal.
- Devolução repõe o produto direto ao estoque vendável (sem triagem de avaria).

## 👩‍💻 Equipe

<div align="center">

| Integrante | GitHub | E-mail acadêmico | Foto |
|---|---|---|---|
| **Anna Karolina** | *Adicionar perfil* | *Adicionar* | *Adicionar* |
| **Awanna Costa** | *Adicionar perfil* | *Adicionar* | *Adicionar* |
| **Cynthia Souza** | *Adicionar perfil* | *Adicionar* | *Adicionar* |
| **Caio** | *Adicionar perfil* | *Adicionar* | *Adicionar* |


**Disciplina:** Análise e Projeto de Sistemas · **Professor:** Dr. Dorgival Netto  
**Instituição:** Universidade Federal da Paraíba (UFPB), Campus IV

</div>
---

💜 *Feito por Equipe 12.*

</div>
