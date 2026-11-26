package br.ufpb.aps.grupo12.seguranca;

import br.ufpb.aps.grupo12.dominio.Perfil;
import java.util.List;
import java.util.Optional;

/**
 * Contas fictícias do StockFlow (domínio .demo inexistente). Somente para demonstração acadêmica:
 * não há cadastro de usuários nem conta pessoal real.
 */
public final class ContasDemo implements ContasDeUsuario {
    public static final String SENHA_DEMO = "stockflow2026";
    private final List<Usuario> usuarios = List.of(
        new Usuario("gerente@stockflow.demo", "Gerente da loja", Perfil.GERENTE,
            ServicoAutenticacao.hash("gerente@stockflow.demo", SENHA_DEMO)),
        new Usuario("vendedora@stockflow.demo", "Vendedora", Perfil.VENDEDOR,
            ServicoAutenticacao.hash("vendedora@stockflow.demo", SENHA_DEMO)));

    @Override public Optional<Usuario> buscarPorEmail(String email) {
        if (email == null) return Optional.empty();
        String chave = email.trim().toLowerCase();
        return usuarios.stream().filter(u -> u.email().equals(chave)).findFirst();
    }
}
