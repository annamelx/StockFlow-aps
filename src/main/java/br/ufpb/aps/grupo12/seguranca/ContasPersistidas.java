package br.ufpb.aps.grupo12.seguranca;

import br.ufpb.aps.grupo12.persistencia.Repositorio;
import java.util.Objects;
import java.util.Optional;

/** Busca usuárias gravadas no arquivo de dados (cadastradas pela gerente em "Atendentes"). */
public final class ContasPersistidas implements ContasDeUsuario {
    private final Repositorio repositorio;
    public ContasPersistidas(Repositorio repositorio) { this.repositorio = Objects.requireNonNull(repositorio); }
    @Override public Optional<Usuario> buscarPorEmail(String email) {
        if (email == null) return Optional.empty();
        String chave = email.trim().toLowerCase();
        return repositorio.consultar(e -> Optional.ofNullable(e.usuarios.get(chave)));
    }
}
