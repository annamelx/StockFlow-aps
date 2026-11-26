package br.ufpb.aps.grupo12.persistencia;

import br.ufpb.aps.grupo12.dominio.*;
import br.ufpb.aps.grupo12.seguranca.Usuario;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

/**
 * Uma única fotografia CSV por transação. O snapshot é gravado em arquivo temporário,
 * fsync e renomeação atômica; o arquivo anterior permanece intacto em caso de falha.
 *
 * O arquivo .lock serializa múltiplos processos que usem este repositório.
 * É necessário um sistema de arquivos que suporte ATOMIC_MOVE; caso contrário a
 * operação falha explicitamente e nenhum "fallback" não atômico é realizado.
 */
public final class RepositorioCsvAtomico implements Repositorio {
    @FunctionalInterface public interface InterceptorCommit { void antesDoCommit() throws RuntimeException; }
    private final Path arquivo;
    private final Path trava;
    private final InterceptorCommit interceptor;

    public RepositorioCsvAtomico(Path arquivo) { this(arquivo, () -> {}); }
    public RepositorioCsvAtomico(Path arquivo, InterceptorCommit interceptor) {
        this.arquivo = Objects.requireNonNull(arquivo).toAbsolutePath();
        this.trava = this.arquivo.resolveSibling(this.arquivo.getFileName() + ".lock");
        this.interceptor = Objects.requireNonNull(interceptor);
    }
    @Override public <T> T consultar(Function<EstadoDados, T> fn) {
        return executar(fn, false);
    }
    @Override public <T> T transacionar(Function<EstadoDados, T> fn) {
        return executar(fn, true);
    }
    private synchronized <T> T executar(Function<EstadoDados,T> fn, boolean escrita) {
        try {
            Path pasta = arquivo.getParent();
            Files.createDirectories(pasta);
            try (FileChannel canalTrava = FileChannel.open(trava,
                     StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock bloqueio = canalTrava.lock()) {
                EstadoDados original = ler();
                EstadoDados trabalho = original.copiar();
                T resultado = fn.apply(trabalho);
                if (escrita && !original.mesmoConteudo(trabalho)) gravarAtomico(trabalho);
                return resultado;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Erro de persistência CSV: " + e.getMessage(), e);
        }
    }
    /** Data usada ao migrar arquivos da versão 1, que não registravam datas. */
    private static final LocalDateTime DATA_MIGRACAO_V1 = LocalDateTime.of(2026, 1, 1, 0, 0);

    private EstadoDados ler() throws IOException {
        EstadoDados estado = new EstadoDados();
        if (!Files.exists(arquivo)) return estado;
        List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        if (linhas.isEmpty() || !(linhas.getFirst().equals("APS_CSV,1") || linhas.getFirst().equals("APS_CSV,2")))
            throw new IllegalStateException("Formato de dados inválido; não sobrescrever o arquivo.");
        record CabecalhoVenda(String id, EstadoVenda situacao, long subtotal, long desconto, long total,
                              String politica, String cliente, FormaPagamento forma, String atendente,
                              LocalDateTime criada, LocalDateTime confirmada) {}
        Map<String, CabecalhoVenda> metadados = new LinkedHashMap<>();
        Map<String, List<ItemVenda>> itens = new LinkedHashMap<>();
        for (int indice = 1; indice < linhas.size(); indice++) {
            String linha = linhas.get(indice);
            if (linha.isBlank()) continue;
            String[] c = linha.split(",", -1);
            try {
                switch (c[0]) {
                    case "P" -> {
                        obrigatorio(c, 6, 8);
                        Produto p = c.length == 6
                            ? new Produto(c[1], decodificar(c[2]), Long.parseLong(c[3]), Integer.parseInt(c[4]), Integer.parseInt(c[5]))
                            : new Produto(c[1], decodificar(c[2]), Long.parseLong(c[3]), Integer.parseInt(c[4]),
                                Integer.parseInt(c[5]), c[6], Long.parseLong(c[7]));
                        unico(estado.produtos, p.id(), p);
                    }
                    case "V" -> {
                        obrigatorio(c, 7, 12);
                        EstadoVenda situacao = EstadoVenda.valueOf(c[2]);
                        CabecalhoVenda cab = c.length == 7
                            ? new CabecalhoVenda(c[1], situacao, Long.parseLong(c[3]), Long.parseLong(c[4]),
                                Long.parseLong(c[5]), decodificar(c[6]), "", FormaPagamento.DINHEIRO, "",
                                DATA_MIGRACAO_V1, situacao == EstadoVenda.CONFIRMADA ? DATA_MIGRACAO_V1 : null)
                            : new CabecalhoVenda(c[1], situacao, Long.parseLong(c[3]), Long.parseLong(c[4]),
                                Long.parseLong(c[5]), decodificar(c[6]), c[7], FormaPagamento.valueOf(c[8]),
                                decodificar(c[9]), data(c[10]), dataOpcional(c[11]));
                        unico(metadados, c[1], cab);
                    }
                    case "I" -> {
                        obrigatorio(c, 6, 7);
                        ItemVenda i = new ItemVenda(c[2], Integer.parseInt(c[3]), Long.parseLong(c[4]),
                            Long.parseLong(c[5]), c.length == 7 ? Long.parseLong(c[6]) : 0);
                        itens.computeIfAbsent(c[1], k -> new ArrayList<>()).add(i);
                    }
                    case "D" -> {
                        obrigatorio(c, 6, 7);
                        Devolucao d = new Devolucao(c[1], c[2], c[3], Integer.parseInt(c[4]), Long.parseLong(c[5]),
                            c.length == 7 ? data(c[6]) : DATA_MIGRACAO_V1);
                        unico(estado.devolucoes, d.id(), d);
                    }
                    case "M" -> {
                        obrigatorio(c, 6, 7);
                        Movimentacao m = new Movimentacao(c[1], c[2], c[3], TipoMovimentacao.valueOf(c[4]),
                            Integer.parseInt(c[5]), c.length == 7 ? data(c[6]) : DATA_MIGRACAO_V1);
                        unico(estado.movimentacoes, m.id(), m);
                    }
                    case "G" -> {
                        obrigatorio(c, 3, 3);
                        unico(estado.categorias, c[1], new Categoria(c[1], decodificar(c[2])));
                    }
                    case "C" -> {
                        obrigatorio(c, 5, 5);
                        unico(estado.clientes, c[1], new Cliente(c[1], decodificar(c[2]), decodificar(c[3]), decodificar(c[4])));
                    }
                    case "F" -> {
                        obrigatorio(c, 5, 5);
                        unico(estado.fornecedores, c[1], new Fornecedor(c[1], decodificar(c[2]), decodificar(c[3]), decodificar(c[4])));
                    }
                    case "K" -> {
                        obrigatorio(c, 7, 7);
                        unico(estado.compras, c[1], new Compra(c[1], c[2], c[3], Integer.parseInt(c[4]),
                            Long.parseLong(c[5]), data(c[6])));
                    }
                    case "A" -> {
                        obrigatorio(c, 8, 8);
                        unico(estado.contas, c[1], new ContaFinanceira(c[1], TipoConta.valueOf(c[2]), decodificar(c[3]),
                            Long.parseLong(c[4]), LocalDate.parse(c[5]), SituacaoConta.valueOf(c[6]),
                            c[7].isEmpty() ? null : LocalDate.parse(c[7])));
                    }
                    case "U" -> {
                        obrigatorio(c, 6, 6);
                        Usuario u = new Usuario(decodificar(c[1]), decodificar(c[2]), Perfil.valueOf(c[3]), c[4],
                            Boolean.parseBoolean(c[5]));
                        unico(estado.usuarios, u.email(), u);
                    }
                    case "X" -> {
                        obrigatorio(c, 3, 3);
                        unico(estado.config, decodificar(c[1]), decodificar(c[2]));
                    }
                    default -> throw new IllegalStateException("Tipo CSV desconhecido: " + c[0]);
                }
            } catch (RuntimeException erro) {
                throw new IllegalStateException("CSV inválido na linha " + (indice + 1), erro);
            }
        }
        for (CabecalhoVenda c : metadados.values()) {
            estado.vendas.put(c.id(), new Venda(c.id(), c.situacao(), itens.getOrDefault(c.id(), List.of()),
                c.subtotal(), c.desconto(), c.total(), c.politica(), c.cliente(), c.forma(), c.atendente(),
                c.criada(), c.confirmada()));
        }
        for (String id : itens.keySet()) if (!estado.vendas.containsKey(id))
            throw new IllegalStateException("Item de venda sem cabeçalho: " + id);
        return estado;
    }
    private static LocalDateTime data(String texto) { return LocalDateTime.parse(texto); }
    private static LocalDateTime dataOpcional(String texto) { return texto.isEmpty() ? null : LocalDateTime.parse(texto); }
    private static String texto(Object valor) { return valor == null ? "" : valor.toString(); }

    private static <T> void unico(Map<String,T> destino, String id, T item) {
        if (destino.putIfAbsent(id, item) != null)
            throw new IllegalStateException("ID duplicado no CSV: " + id);
    }
    private static void obrigatorio(String[] linha, int versao1, int versao2) {
        if (linha.length != versao1 && linha.length != versao2)
            throw new IllegalArgumentException("Colunas diferentes do esperado: " + versao2);
    }
    private static String codificar(String texto) {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }
    private static String decodificar(String texto) {
        return new String(Base64.getUrlDecoder().decode(texto), StandardCharsets.UTF_8);
    }
    private void gravarAtomico(EstadoDados dados) throws IOException {
        Path temporario = Files.createTempFile(arquivo.getParent(), "aps-snapshot-", ".tmp");
        try {
            StringBuilder csv = new StringBuilder("APS_CSV,2\n");
            for (Categoria g : dados.categorias.values())
                linha(csv, "G", g.id(), codificar(g.nome()));
            for (Produto p : dados.produtos.values())
                linha(csv, "P", p.id(), codificar(p.nome()), p.precoCentavos(), p.estoque(), p.estoqueMinimo(),
                    p.categoriaId(), p.custoCentavos());
            for (Cliente c : dados.clientes.values())
                linha(csv, "C", c.id(), codificar(c.nome()), codificar(c.telefone()), codificar(c.email()));
            for (Fornecedor f : dados.fornecedores.values())
                linha(csv, "F", f.id(), codificar(f.nome()), codificar(f.documento()), codificar(f.telefone()));
            for (Usuario u : dados.usuarios.values())
                linha(csv, "U", codificar(u.email()), codificar(u.nome()), u.perfil(), u.hashSenha(), u.ativo());
            for (Venda v : dados.vendas.values()) {
                linha(csv, "V", v.id(), v.estado(), v.subtotalCentavos(), v.descontoCentavos(), v.totalCentavos(),
                    codificar(v.nomePolitica()), v.clienteId(), v.formaPagamento().name(), codificar(v.atendente()),
                    v.criadaEm(), texto(v.confirmadaEm()));
                for (ItemVenda i : v.itens())
                    linha(csv, "I", v.id(), i.produtoId(), i.quantidade(), i.precoUnitarioCentavos(),
                        i.descontoCentavos(), i.custoUnitarioCentavos());
            }
            for (Devolucao d : dados.devolucoes.values())
                linha(csv, "D", d.id(), d.vendaId(), d.produtoId(), d.quantidade(), d.reembolsoCentavos(), d.data());
            for (Movimentacao m : dados.movimentacoes.values())
                linha(csv, "M", m.id(), m.operacaoId(), m.produtoId(), m.tipo(), m.quantidade(), m.data());
            for (Compra k : dados.compras.values())
                linha(csv, "K", k.id(), k.fornecedorId(), k.produtoId(), k.quantidade(), k.custoUnitarioCentavos(), k.data());
            for (ContaFinanceira a : dados.contas.values())
                linha(csv, "A", a.id(), a.tipo().name(), codificar(a.descricao()), a.valorCentavos(), a.vencimento(),
                    a.situacao(), texto(a.quitadaEm()));
            for (var x : dados.config.entrySet())
                linha(csv, "X", codificar(x.getKey()), codificar(x.getValue()));
            byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
            try (FileChannel canal = FileChannel.open(temporario, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) canal.write(buffer);
                canal.force(true);
            }
            interceptor.antesDoCommit(); // injeção de defeito para teste do 'tudo ou nada'
            Files.move(temporario, arquivo, StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporario);
        }
    }
    private static void linha(StringBuilder csv, String tipo, Object... campos) {
        csv.append(tipo);
        for (Object campo : campos) csv.append(',').append(campo);
        csv.append('\n');
    }
}
