package br.ufpb.aps.grupo12.dominio;

/** Validações de campos textuais de cadastro. */
public final class Texto {
    private Texto() {}
    public static String obrigatorio(String valor, String campo, int max) {
        if (valor == null || valor.isBlank()) throw new ErroNegocio(campo + " é obrigatório.");
        String limpo = valor.trim();
        if (limpo.length() > max) throw new ErroNegocio(campo + " deve ter até " + max + " caracteres.");
        return limpo;
    }
    public static String opcional(String valor, String campo, int max) {
        if (valor == null || valor.isBlank()) return "";
        String limpo = valor.trim();
        if (limpo.length() > max) throw new ErroNegocio(campo + " deve ter até " + max + " caracteres.");
        return limpo;
    }
}
