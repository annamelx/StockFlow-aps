package br.ufpb.aps.grupo12.dominio;

/** Falha esperada de validação ou transição de estado do negócio. */
public final class ErroNegocio extends RuntimeException {
    public ErroNegocio(String mensagem) { super(mensagem); }
}
