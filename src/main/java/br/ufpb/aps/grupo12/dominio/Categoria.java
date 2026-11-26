package br.ufpb.aps.grupo12.dominio;

public record Categoria(String id, String nome) {
    public static final String GERAL = "GERAL";
    public Categoria {
        Validacao.id(id);
        nome = Texto.obrigatorio(nome, "Nome da categoria", 60);
    }
}
