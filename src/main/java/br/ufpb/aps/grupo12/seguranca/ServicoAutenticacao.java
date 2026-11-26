package br.ufpb.aps.grupo12.seguranca;

import br.ufpb.aps.grupo12.dominio.ErroNegocio;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Identifica quem está usando o sistema e devolve o perfil. A AUTORIZAÇÃO de cada operação
 * continua sendo feita no ServicoLoja (camada de serviço), não aqui nem na tela.
 */
public final class ServicoAutenticacao {
    private final ContasDeUsuario contas;
    public ServicoAutenticacao(ContasDeUsuario contas) { this.contas = Objects.requireNonNull(contas); }

    public Usuario entrar(String email, String senha) {
        if (email == null || email.isBlank() || senha == null || senha.isEmpty())
            throw new ErroNegocio("Informe e-mail e senha.");
        Usuario u = contas.buscarPorEmail(email)
            .orElseThrow(() -> new ErroNegocio("E-mail ou senha incorretos."));
        byte[] esperado = u.hashSenha().getBytes(StandardCharsets.UTF_8);
        byte[] informado = hash(u.email(), senha).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(esperado, informado))
            throw new ErroNegocio("E-mail ou senha incorretos.");
        return u;
    }

    /** SHA-256 de "email:senha" (o e-mail funciona como sal simples). */
    public static String hash(String email, String senha) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest((email.trim().toLowerCase() + ":" + senha).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }
    }
}
