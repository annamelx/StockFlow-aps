package br.ufpb.aps.grupo12.dominio;
public enum TipoConta {
    PAGAR("A pagar"), RECEBER("A receber");
    private final String rotulo;
    TipoConta(String r) { rotulo = r; }
    @Override public String toString() { return rotulo; }
}
