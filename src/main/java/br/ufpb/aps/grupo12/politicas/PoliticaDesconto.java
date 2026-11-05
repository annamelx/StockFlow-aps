package br.ufpb.aps.grupo12.politicas;
/** GoF Strategy (comportamental): cálculo intercambiável. */
public interface PoliticaDesconto {
    long descontoEmCentavos(long subtotalCentavos);
    String nome();
}
