package br.ufpb.aps.grupo12.seguranca;

import br.ufpb.aps.grupo12.dominio.ErroNegocio;
import br.ufpb.aps.grupo12.dominio.Perfil;

/** Usuária do sistema (gerente ou atendente). A senha nunca é guardada em texto: só o hash SHA-256. */
public record Usuario(String email, String nome, Perfil perfil, String hashSenha, boolean ativo) {
    public Usuario {
        if (email == null || !email.trim().matches("[^@\\s,]+@[^@\\s,]+"))
            throw new ErroNegocio("E-mail inválido.");
        if (nome == null || nome.isBlank()) throw new ErroNegocio("Nome obrigatório.");
        if (perfil == null || hashSenha == null || hashSenha.isBlank())
            throw new ErroNegocio("Usuário incompleto.");
        email = email.trim().toLowerCase();
        nome = nome.trim();
    }
    public Usuario(String email, String nome, Perfil perfil, String hashSenha) {
        this(email, nome, perfil, hashSenha, true);
    }
    public Usuario comAtivo(boolean novo) { return new Usuario(email, nome, perfil, hashSenha, novo); }
    /** Rótulo do perfil exibido nas telas. */
    public String rotuloPerfil() { return perfil == Perfil.GERENTE ? "Gerente" : "Atendente"; }
}
