package br.ufpb.aps.grupo12.dominio;

public record Fornecedor(String id, String nome, String documento, String telefone) {
    public Fornecedor {
        Validacao.id(id);
        nome = Texto.obrigatorio(nome, "Nome do fornecedor", 100);
        documento = Texto.opcional(documento, "CNPJ/CPF", 30);
        telefone = Texto.opcional(telefone, "Telefone", 30);
    }
}
