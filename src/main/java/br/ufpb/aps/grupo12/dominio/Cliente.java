package br.ufpb.aps.grupo12.dominio;

public record Cliente(String id, String nome, String telefone, String email) {
    public Cliente {
        Validacao.id(id);
        nome = Texto.obrigatorio(nome, "Nome do cliente", 100);
        telefone = Texto.opcional(telefone, "Telefone", 30);
        email = Texto.opcional(email, "E-mail", 100);
        if (!email.isEmpty() && !email.matches("[^@\\s]+@[^@\\s]+"))
            throw new ErroNegocio("E-mail do cliente inválido.");
    }
}
