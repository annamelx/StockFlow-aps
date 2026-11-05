package br.ufpb.aps.grupo12.relatorios;
import br.ufpb.aps.grupo12.persistencia.EstadoDados;
public interface ExportadorRelatorio {
    String exportar(EstadoDados dados);
}
