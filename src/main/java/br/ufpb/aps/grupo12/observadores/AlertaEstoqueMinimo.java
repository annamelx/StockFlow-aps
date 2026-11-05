package br.ufpb.aps.grupo12.observadores;

import java.util.ArrayList;
import java.util.List;
public final class AlertaEstoqueMinimo implements ObservadorEstoque {
    private final List<String> alertas = new ArrayList<>();
    @Override public void aoAlterarEstoque(EventoEstoque evento) {
        if (evento.novoSaldo() <= evento.minimo())
            alertas.add("Alerta: estoque mínimo em " + evento.produtoId()
                + " (saldo=" + evento.novoSaldo() + ")");
    }
    public List<String> alertas() { return List.copyOf(alertas); }
}
