package br.ufpb.aps.grupo12.relatorios;
import br.ufpb.aps.grupo12.persistencia.EstadoDados;
import java.util.List;

/** GoF Adapter (estrutural): transforma nossos produtos no contrato legado. */
public final class AdaptadorRelatorio implements ExportadorRelatorio {
    private final ExportadorAntigo legado;
    public AdaptadorRelatorio(ExportadorAntigo legado) { this.legado = legado; }
    @Override public String exportar(EstadoDados dados) {
        List<String[]> linhas = dados.produtos.values().stream()
            .map(p -> new String[]{p.id(), p.nome(), String.valueOf(p.estoque())})
            .toList();
        return legado.arquivo(linhas);
    }
}
