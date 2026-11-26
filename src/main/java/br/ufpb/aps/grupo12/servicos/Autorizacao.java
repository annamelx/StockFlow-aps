package br.ufpb.aps.grupo12.servicos;

import br.ufpb.aps.grupo12.dominio.ErroNegocio;
import br.ufpb.aps.grupo12.dominio.Perfil;

/** Verificação de permissão usada por todos os serviços (camada de aplicação, não a tela). */
final class Autorizacao {
    private Autorizacao() {}
    static void exigir(Perfil perfil, boolean requerGerente) {
        if (perfil == null || (requerGerente && perfil != Perfil.GERENTE))
            throw new ErroNegocio("Perfil não tem autorização para esta operação.");
    }
}
