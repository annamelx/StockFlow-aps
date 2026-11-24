 <div align="center">

🛍️ Sistema de Gestão de Estoque e Vendas

💜 APS • Grupo 12 • UFPB Campus IV • 2026.2

<img alt="Java 21" src="https://img.shields.io/badge/Java-21-7C3AED?style=for-the-badge&logo=openjdk&logoColor=white" />
<img alt="Maven" src="https://img.shields.io/badge/Maven-Projeto-EC4899?style=for-the-badge&logo=apachemaven&logoColor=white" />
<img alt="JUnit 5" src="https://img.shields.io/badge/Testes-JUnit%205-A855F7?style=for-the-badge" />
<img alt="Situação: versão inicial funcional" src="https://img.shields.io/badge/Status-Vers%C3%A3o%20inicial-6D28D9?style=for-the-badge" />

Vendas, descontos, devoluções e estoque consistente — inclusive quando uma operação falha.

✨ Funcionalidades · 🏗️ Arquitetura · 🚀 Executar · 🧪 Testar · 👩‍💻 Equipe

</div>

⸻

💜 O projeto

Este projeto foi desenvolvido como base para a Unidade III de Análise e Projeto de Sistemas, sob orientação do Prof. Dr. Dorgival Netto, na Universidade Federal da Paraíba — Campus IV.

O desafio do Grupo 12 é implementar venda, baixa de estoque e devolução vinculada aos itens realmente vendidos, impedindo devoluções acima da quantidade vendida ou processadas duas vezes. Também é necessário variar uma política de desconto e manter produtos, valores e movimentos consistentes quando ocorrer falha de persistência.

O tema foi definido pelo professor e deve utilizar como referência a documentação Toda Chick Bolsas & Cia.. A especificação inicial foi construída a partir das orientações oficiais da disciplina e deve ser revisada pela equipe considerando a documentação de referência.

🎯 O que o sistema resolve?

* Evita vender mais unidades do que existem em estoque.
* Garante que confirmar a mesma venda duas vezes não gere duas baixas.
* Impede devolução de um produto não vendido ou acima da quantidade comprada.
* Calcula desconto percentual ou fixo, além de reembolsos em centavos inteiros, sem erros de ponto flutuante.
* Usa gravação atômica em CSV: venda, saldo e histórico são substituídos juntos, ou o arquivo anterior permanece intacto.
* Diferencia permissões de gerente e vendedor, verificadas no serviço.

✨ Funcionalidades

Caso de uso	Fluxos da versão inicial
UC01 · Produtos e estoque	Cadastrar, consultar e registrar reposição sem duplicidade de lançamento.
UC02 · Vendas	Abrir em PENDENTE, calcular descontos, confirmar com baixa atômica e cancelar pendentes.
UC03 · Devoluções	Validar venda confirmada, impedir excesso, calcular restituição e repor estoque uma única vez.
UC04 · Consultas e relatórios	Consultar estoque, movimentos, vendas e receita líquida simulada; exportar relatório legado simulado.

As funcionalidades têm regras de sucesso e falha. Não há emissão fiscal, pagamento real nem autenticação de usuários; o console permite selecionar um perfil apenas para a demonstração acadêmica.

🔄 Ciclo de uma venda

stateDiagram-v2
  [*] --> PENDENTE: abrir venda
  PENDENTE --> CONFIRMADA: confirmar e baixar estoque
  PENDENTE --> CANCELADA: cancelar antes da confirmação
  CONFIRMADA --> CONFIRMADA: confirmar novamente sem novo efeito
  CONFIRMADA --> [*]
  CANCELADA --> [*]

👥 Permissões

Operação	Gerente	Vendedor(a)
Consultar estoque, movimentações e relatório	✅	✅
Abrir/confirmar/cancelar venda pendente	✅	✅
Cadastrar produto e repor estoque	✅	❌
Registrar devolução	✅	❌

🏗️ Arquitetura

Estrutura implementada em Java 21, usando arquitetura em camadas, injeção de dependências por construtor e quatro padrões de projeto GoF em duas categorias.

flowchart TB
    A[Console - App] --> B[LojaFacade - Facade]
    B --> C[ServicoLoja - autorização e casos de uso]
    C --> D[Domínio: Produto / Venda / ItemVenda / Devolucao]
    C --> E[Repositorio - contrato]
    E --> F[RepositorioCsvAtomico - snapshot CSV]
    C --> G[PoliticaDesconto - Strategy]
    C --> H[ObservadorEstoque - Observer]
    B --> I[AdaptadorRelatorio - Adapter]

GoF	Categoria	Uso no sistema
Strategy	Comportamental	Troca política de desconto percentual por fixo.
Observer	Comportamental	Registra alerta quando estoque atinge mínimo, após confirmação da operação.
Facade	Estrutural	Simplifica o conjunto de serviços acessados pela interface.
Adapter	Estrutural	Adapta dados atuais ao exportador legado simulado.

Extensão demonstrável: DescontoFixo implementa PoliticaDesconto; o serviço ServicoLoja.abrirVenda() continua usando o contrato comum, sem alteração. Consulte arquitetura e decisão de extensão.

🗂️ Organização do código

.
├── pom.xml
├── README.md
├── src/
│   ├── main/java/br/ufpb/aps/grupo12/
│   │   ├── aplicacao/      # Console e montagem de dependências
│   │   ├── dominio/        # Produto, Venda, ItemVenda, Devolucao, Movimentacao
│   │   ├── servicos/       # Casos de uso e Facade
│   │   ├── persistencia/   # Contrato Repositorio e CSV atômico
│   │   ├── politicas/      # Strategy percentual e fixo
│   │   ├── observadores/   # Observer de estoque mínimo
│   │   └── relatorios/     # Adapter e exportação legada simulada
│   └── test/java/br/ufpb/aps/grupo12/ServicoLojaTest.java
├── dados/                 # CSV gerado em execução, ignorado pelo Git
└── docs/                  # UML, regras, rastreabilidade, ADRs, testes, apresentação

🚀 Executando o projeto

Pré-requisitos: JDK 21 e Maven 3.9+ instalados. Não há dependência de banco externo nem de serviços pagos.

No terminal, dentro da pasta do projeto, execute:

mvn clean compile
mvn exec:java

Na primeira execução, o programa carrega três produtos fictícios. É possível escolher o perfil e executar as operações em um menu.

Resetar para repetir os testes demonstrativos:

mvn compile exec:java -Dexec.args="--reset-demo"
mvn exec:java

O reset substitui os dados do arquivo de demonstração — não execute em um arquivo que contenha dados que deseja preservar. Para comandos de PowerShell ou problemas no IntelliJ IDEA, consulte validação.

🧪 Testes

mvn clean test

Existem 27 cenários JUnit que cobrem regras de negócio, valores de fronteira, perfis, estados, idempotência, persistência real, falhas injetadas de commit e os quatro padrões. A execução oficial com Maven/JUnit ainda precisa ser confirmada pela equipe. Não confundir verificações preliminares com o relatório final do Maven.

Os resultados oficiais deverão ser capturados em target/surefire-reports/. Para dois defeitos introduzidos temporariamente, há um procedimento documentado em validação e mutações. O código entregue deve permanecer sem alterações intencionalmente incorretas.

📚 Documentação para o professor

Documento	Conteúdo
Especificação de requisitos	UC01–UC04, RN01–RN11, perfis, fluxos, políticas, limites e critérios de aceitação.
Arquitetura e padrões GoF	Responsabilidades, transação, alternativas, classes dos quatro padrões e extensão.
Diagrama de classes UML (PlantUML)	Atributos, visibilidades, métodos e relacionamentos.
Fluxos e estados (Mermaid)	Estados da venda e fluxos de venda e devolução.
Matriz de rastreabilidade	Cada regra ligada a métodos, testes e evidências de demonstração.
Decisões arquiteturais	Contexto, opções, decisão, consequências e evidências.
Validação	Cenários reproduzíveis, falhas de commit e roteiro de duas mutações.
Uso de IA	Declaração de auxílio e tabela de interações significativas a revisar.
Roteiro da 1ª parcial	Apresentação parcial, com distribuição sugerida de cinco minutos.
Roteiro final	Apresentação final, com demonstração e possíveis perguntas.
Slides da 1ª parcial	Apresentação editável acompanhada de roteiro.
Guia IntelliJ/GitHub	Importação no Windows, testes, colaboração e push.
Evidência técnica preliminar de mutações	Dois defeitos detectados em cópias temporárias, sem execução oficial Maven.

👩‍💻 Equipe

Integrante	GitHub	E-mail acadêmico
Anna Karolina	annamelx	anna.gomes@dcx.ufpb.br
Awanna Costa	A adicionar	A adicionar
Cynthia Souza	A adicionar	A adicionar
Caio	A adicionar	A adicionar

Disciplina: Análise e Projeto de Sistemas
Professor: Prof. Dr. Dorgival Netto
Instituição: Universidade Federal da Paraíba (UFPB), Campus IV.

⸻

💜 Desenvolvido pela Equipe 12 — 2026.2.