package br.ufpb.aps.grupo12.interfacegrafica;

import br.ufpb.aps.grupo12.dominio.Perfil;
import br.ufpb.aps.grupo12.seguranca.Usuario;
import java.awt.*;
import java.util.List;
import javax.swing.*;

/** Usuárias com acesso ao sistema. Senhas são guardadas apenas como hash. */
final class PaginaAtendentes extends PaginaListaDetalhe<Usuario> {
    private final JTextField email = Tema.campo(24), nome = Tema.campo(24);
    private final JPasswordField senha = new JPasswordField(16);
    private final JComboBox<Perfil> perfil = new JComboBox<>(Perfil.values());
    private final JButton ativar = Tema.botaoSecundario("Desativar acesso");

    PaginaAtendentes(Contexto ctx) {
        super(ctx, "Atendentes", "usuária", "Nova atendente", Icone.Tipo.PESSOAS);
        Tema.estilizarCampo(senha);
        Tema.estilizarCampo(perfil);
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 12));
        p.setOpaque(false);
        p.add(ativar);
        extras.add(p, BorderLayout.NORTH);
        ativar.addActionListener(e -> {
            if (selecionado == null) return;
            Usuario u = selecionado;
            executar(() -> selecionado = ctx.facade().alterarAtivo(ctx.perfil(), u.email(), !u.ativo()),
                u.ativo() ? "Acesso desativado." : "Acesso reativado.");
        });
        montarFormulario();
    }
    @Override List<Usuario> carregar() { return ctx.facade().usuarios(ctx.perfil()); }
    @Override String nome(Usuario u) { return u.nome(); }
    @Override String detalhe(Usuario u) { return u.email(); }
    @Override boolean permiteExcluir() { return false; }
    @Override JComponent etiqueta(Usuario u) {
        return u.ativo() ? Componentes.pilula(u.rotuloPerfil(), Tema.AZUL_CLARO, Tema.AZUL_ESCURO)
            : Componentes.pilula("Inativa", new Color(0xFEE2E2), Tema.ALERTA);
    }
    @Override String[] rotulos() { return new String[]{"E-mail", "Nome", "Perfil", "Senha inicial"}; }
    @Override JComponent[] campos() { return new JComponent[]{email, nome, perfil, senha}; }
    @Override void preencher(Usuario u) {
        boolean novo = u == null;
        email.setEditable(novo); nome.setEditable(novo); perfil.setEnabled(novo); senha.setEnabled(novo);
        email.setText(novo ? "" : u.email());
        nome.setText(novo ? "" : u.nome());
        perfil.setSelectedItem(novo ? Perfil.VENDEDOR : u.perfil());
        senha.setText("");
        ativar.setVisible(!novo);
        if (!novo) ativar.setText(u.ativo() ? "Desativar acesso" : "Reativar acesso");
    }
    @Override void salvar(boolean novo) {
        if (!novo) throw new br.ufpb.aps.grupo12.dominio.ErroNegocio("Para alterar dados, desative e cadastre novamente.");
        selecionado = ctx.facade().cadastrarUsuario(ctx.perfil(), email.getText(), nome.getText(),
            (Perfil) perfil.getSelectedItem(), new String(senha.getPassword()));
    }
}
