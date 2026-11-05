package br.ufpb.aps.grupo12.observadores;
/** GoF Observer: avisos emitidos após commit da transação. */
public interface ObservadorEstoque {
    void aoAlterarEstoque(EventoEstoque evento);
}
