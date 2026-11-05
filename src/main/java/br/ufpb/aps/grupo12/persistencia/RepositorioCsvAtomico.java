package br.ufpb.aps.grupo12.persistencia;

import br.ufpb.aps.grupo12.dominio.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
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
    private EstadoDados ler() throws IOException {
        EstadoDados estado = new EstadoDados();
        if (!Files.exists(arquivo)) return estado;
        List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        if (linhas.isEmpty() || !linhas.getFirst().equals("APS_CSV,1"))
            throw new IllegalStateException("Formato de dados inválido; não sobrescrever o arquivo.");
        record CabecalhoVenda(String id, EstadoVenda situacao, long subtotal, long desconto,
                             long total, String politica) {}
        Map<String, CabecalhoVenda> metadados = new LinkedHashMap<>();
        Map<String, List<ItemVenda>> itens = new LinkedHashMap<>();
        for (int indice = 1; indice < linhas.size(); indice++) {
            String linha = linhas.get(indice);
            if (linha.isBlank()) continue;
            String[] c = linha.split(",", -1);
            try {
                switch (c[0]) {
                    case "P" -> {
                        obrigatorio(c, 6);
                        Produto p = new Produto(c[1], decodificar(c[2]), Long.parseLong(c[3]),
                            Integer.parseInt(c[4]), Integer.parseInt(c[5]));
                        unico(estado.produtos, p.id(), p);
                    }
                    case "V" -> {
                        obrigatorio(c, 7);
                        CabecalhoVenda cab = new CabecalhoVenda(c[1], EstadoVenda.valueOf(c[2]),
                            Long.parseLong(c[3]), Long.parseLong(c[4]), Long.parseLong(c[5]),
                            decodificar(c[6]));
                        unico(metadados, c[1], cab);
                    }
                    case "I" -> {
                        obrigatorio(c, 6);
                        ItemVenda i = new ItemVenda(c[2], Integer.parseInt(c[3]),
                            Long.parseLong(c[4]), Long.parseLong(c[5]));
                        itens.computeIfAbsent(c[1], k -> new ArrayList<>()).add(i);
                    }
                    case "D" -> {
                        obrigatorio(c, 6);
                        Devolucao d = new Devolucao(c[1], c[2], c[3], Integer.parseInt(c[4]),
                            Long.parseLong(c[5]));
                        unico(estado.devolucoes, d.id(), d);
                    }
                    case "M" -> {
                        obrigatorio(c, 6);
                        Movimentacao m = new Movimentacao(c[1], c[2], c[3],
                            TipoMovimentacao.valueOf(c[4]), Integer.parseInt(c[5]));
                        unico(estado.movimentacoes, m.id(), m);
                    }
                    default -> throw new IllegalStateException("Tipo CSV desconhecido: " + c[0]);
                }
            } catch (RuntimeException erro) {
                throw new IllegalStateException("CSV inválido na linha " + (indice + 1), erro);
            }
        }
        for (CabecalhoVenda c : metadados.values()) {
            estado.vendas.put(c.id(), new Venda(c.id(), c.situacao(),
                itens.getOrDefault(c.id(), List.of()), c.subtotal(), c.desconto(), c.total(), c.politica()));
        }
        for (String id : itens.keySet()) if (!estado.vendas.containsKey(id))
            throw new IllegalStateException("Item de venda sem cabeçalho: " + id);
        return estado;
    }
    private static <T> void unico(Map<String,T> destino, String id, T item) {
        if (destino.putIfAbsent(id, item) != null)
            throw new IllegalStateException("ID duplicado no CSV: " + id);
    }
    private static void obrigatorio(String[] linha, int quantidadeCampos) {
        if (linha.length != quantidadeCampos)
            throw new IllegalArgumentException("Colunas diferentes do esperado: " + quantidadeCampos);
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
            StringBuilder csv = new StringBuilder("APS_CSV,1\n");
            for (Produto p : dados.produtos.values())
                csv.append("P,").append(p.id()).append(',').append(codificar(p.nome()))
                    .append(',').append(p.precoCentavos()).append(',').append(p.estoque())
                    .append(',').append(p.estoqueMinimo()).append('\n');
            for (Venda v : dados.vendas.values()) {
                csv.append("V,").append(v.id()).append(',').append(v.estado()).append(',')
                    .append(v.subtotalCentavos()).append(',').append(v.descontoCentavos())
                    .append(',').append(v.totalCentavos()).append(',').append(codificar(v.nomePolitica()))
                    .append('\n');
                for (ItemVenda i : v.itens())
                    csv.append("I,").append(v.id()).append(',').append(i.produtoId()).append(',')
                        .append(i.quantidade()).append(',').append(i.precoUnitarioCentavos()).append(',')
                        .append(i.descontoCentavos()).append('\n');
            }
            for (Devolucao d : dados.devolucoes.values())
                csv.append("D,").append(d.id()).append(',').append(d.vendaId()).append(',')
                    .append(d.produtoId()).append(',').append(d.quantidade())
                    .append(',').append(d.reembolsoCentavos()).append('\n');
            for (Movimentacao m : dados.movimentacoes.values())
                csv.append("M,").append(m.id()).append(',').append(m.operacaoId()).append(',')
                    .append(m.produtoId()).append(',').append(m.tipo()).append(',')
                    .append(m.quantidade()).append('\n');
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
    /** Retorna a base para demonstração a um estado conhecido, não dados de produção. */
    public void restaurarDemo() {
        transacionar(e -> {
            e.produtos.clear(); e.vendas.clear(); e.devolucoes.clear(); e.movimentacoes.clear();
            e.produtos.put("BOLSA01", new Produto("BOLSA01", "Bolsa transversal", 15000, 10, 3));
            e.produtos.put("BOLSA02", new Produto("BOLSA02", "Bolsa de ombro", 20000, 6, 2));
            e.produtos.put("CARTEIRA01", new Produto("CARTEIRA01", "Carteira feminina", 4500, 8, 2));
            return null;
        });
    }
}
