package br.ufpb.aps.grupo12.relatorios;

import java.util.List;
/** Contrato externo fictício: API legada recebe linhas de texto separadas. */
public final class ExportadorAntigo {
    public String arquivo(List<String[]> linhas) {
        StringBuilder csv = new StringBuilder("CODIGO;PRODUTO;QUANTIDADE\n");
        for (String[] linha : linhas) {
            for (int i = 0; i < linha.length; i++) {
                if (i > 0) csv.append(';');
                csv.append('"').append(linha[i].replace("\"", "\"\"")).append('"');
            }
            csv.append('\n');
        }
        return csv.toString();
    }
}
