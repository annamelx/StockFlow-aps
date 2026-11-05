# Como abrir no IntelliJ IDEA e enviar para o GitHub

## 1. Preparar o Java e o Maven

1. Instale/seleciona o **JDK 21** no IntelliJ.
2. Extraia o ZIP do projeto para uma pasta do Windows, por exemplo `Documentos/APS-Grupo12/`.
3. No IntelliJ IDEA, escolha **File → Open** e selecione a pasta que contém `pom.xml`.
4. Aguarde a importação Maven. Verifique **Project Structure → Project SDK = 21** e, se necessário, **Settings → Build Tools → Maven → Runner → JRE**.
5. Em **Terminal** na pasta raiz do projeto, execute `mvn clean test`. Caso `mvn` não seja reconhecido, use a janela Maven do IntelliJ com a fase `test`, ou instale Maven/adicione ao PATH.

## 2. Executar

**Compilar e abrir:**

```powershell
mvn clean compile
mvn exec:java
```

O console pergunta primeiro o perfil `1` (gerente) ou `2` (vendedor) e depois as operações. Um arquivo `dados/loja.csv` é criado com produtos de exemplo. Para apagar alterações da demonstração e restaurar os saldos iniciais:

```powershell
mvn compile exec:java -Dexec.args="--reset-demo"
```

O arquivo de dados é ignorado pelo Git (`.gitignore`) para não misturar registros locais de teste com código.

## 3. Se o repositório GitHub já foi criado (com README inicial)

Abra o terminal no local onde deseja clonar seu repositório, depois:

```powershell
git clone URL_DO_SEU_REPOSITORIO
cd NOME_DA_PASTA_CLONADA
```

Copie **todo o conteúdo extraído do ZIP** para dentro da pasta clonada, aceitando substituir o README inicial. **Não mova a pasta `.git`** (fica oculta): o repositório clonado precisa mantê-la. Em seguida:

```powershell
git status
git add .
git commit -m "feat: estrutura inicial APS grupo 12 com casos de uso e testes"
git push origin main
```

Caso a branch padrão não seja `main`, veja o nome com `git branch --show-current` e faça `git push` usando esse nome. Se o commit falhar por falta de nome/e-mail configurados, configure **apenas no seu computador** com suas informações reais:

```powershell
git config --global user.name "SEU NOME NO GIT"
git config --global user.email "SEU EMAIL DE COMMITS"
```

Se preferir interface gráfica, use **GitHub Desktop** ou a aba **Git** do IntelliJ para fazer commit e push depois de clonar o repositório.

## 4. Colaboração das três integrantes

- No GitHub, abra **Settings → Collaborators → Add people** e convide Awanna e Cynthia por seus usuários reais.
- Cada pessoa clona o repositório na própria máquina, cria uma branch para sua tarefa e faz commits claros com sua conta. Recomendações de branches: `feat/testes-vendas`, `docs/uml-requisitos`, `feat/relatorios`.
- Para integrar as contribuições, abram Pull Requests, revisem alterações e executem os testes antes do merge.
- Evitem inserir senhas, tokens, documentos pessoais ou arquivos `dados/*.csv` reais no Git.

## 5. Checklist antes do push final

- [ ] `mvn clean test` terminou com todos os testes passando.
- [ ] Diagrama de classes revisado com base no código definitivo.
- [ ] Documento Toda Chick Bolsas & Cia. revisado e linkado no README.
- [ ] Diferenças entre documentação anterior e requisitos novos registradas.
- [ ] Dois testes de mutação demonstrados e restaurados; logs/screenshot salvos.
- [ ] Todas as três integrantes conseguem executar o projeto e explicar um caso de uso.
- [ ] Fotos/contatos acadêmicos autorizados por cada integrante adicionados ao README.
- [ ] Slides e demais evidências finais criados e referenciados.
