package br.ufpb.aps.grupo12.dominio;

public final class Validacao {
    private Validacao() {}
    public static void id(String valor) {
        if (valor == null || !valor.matches("[A-Za-z0-9_-]{1,60}"))
            throw new ErroNegocio("Identificador inválido (use letras, números, _ ou -; até 60 caracteres).");
    }
    public static void quantidadePositiva(int valor) {
        if (valor <= 0) throw new ErroNegocio("Quantidade deve ser maior que zero.");
    }
}
