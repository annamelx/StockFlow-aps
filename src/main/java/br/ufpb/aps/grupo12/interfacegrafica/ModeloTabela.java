package br.ufpb.aps.grupo12.interfacegrafica;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.swing.table.AbstractTableModel;

/** Modelo de tabela somente leitura, alimentado por uma lista e funções de coluna. */
final class ModeloTabela<T> extends AbstractTableModel {
    record Coluna<T>(String titulo, Function<T, Object> valor) {}
    private final List<Coluna<T>> colunas;
    private List<T> linhas = new ArrayList<>();

    @SafeVarargs ModeloTabela(Coluna<T>... colunas) { this.colunas = List.of(colunas); }

    void definir(List<T> novas) { linhas = List.copyOf(novas); fireTableDataChanged(); }
    T linha(int indiceModelo) { return linhas.get(indiceModelo); }

    @Override public int getRowCount() { return linhas.size(); }
    @Override public int getColumnCount() { return colunas.size(); }
    @Override public String getColumnName(int c) { return colunas.get(c).titulo(); }
    @Override public Object getValueAt(int l, int c) { return colunas.get(c).valor().apply(linhas.get(l)); }
}
