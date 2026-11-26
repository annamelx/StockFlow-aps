package br.ufpb.aps.grupo12.seguranca;

import java.util.Optional;

/** Contrato de busca de usuários: permite trocar as contas fixas de demonstração em testes. */
public interface ContasDeUsuario {
    Optional<Usuario> buscarPorEmail(String email);
}
