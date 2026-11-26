package br.ufpb.aps.grupo12.interfacegrafica;

import br.ufpb.aps.grupo12.observadores.AlertaEstoqueMinimo;
import br.ufpb.aps.grupo12.politicas.fabricas.CatalogoDescontos;
import br.ufpb.aps.grupo12.seguranca.ContasDemo;
import br.ufpb.aps.grupo12.seguranca.ServicoAutenticacao;
import br.ufpb.aps.grupo12.seguranca.Usuario;
import br.ufpb.aps.grupo12.servicos.LojaFacade;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

/** Tela de entrada do StockFlow: cartão central sobre fundo escuro. */
public final class JanelaLogin extends JFrame {
    private final LojaFacade facade;
    private final ServicoAutenticacao autenticacao;
    private final CatalogoDescontos descontos;
    private final AlertaEstoqueMinimo alerta;
    private final JTextField email = Tema.campo(22);
    private final JPasswordField senha = new JPasswordField(22);

    public static void abrir(LojaFacade facade, ServicoAutenticacao autenticacao,
                             CatalogoDescontos descontos, AlertaEstoqueMinimo alerta) {
        SwingUtilities.invokeLater(() -> new JanelaLogin(facade, autenticacao, descontos, alerta).setVisible(true));
    }

    JanelaLogin(LojaFacade facade, ServicoAutenticacao autenticacao,
                CatalogoDescontos descontos, AlertaEstoqueMinimo alerta) {
        super("StockFlow — Entrar");
        this.facade = facade;
        this.autenticacao = autenticacao;
        this.descontos = descontos;
        this.alerta = alerta;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(conteudo());
        setSize(980, 620);
        setMinimumSize(new Dimension(860, 560));
        setLocationRelativeTo(null);
    }

    JPanel conteudo() {
        FundoGradiente raiz = new FundoGradiente(new BorderLayout(), true);
        JPanel topo = new JPanel(new BorderLayout());
        topo.setOpaque(false);
        topo.setBorder(new EmptyBorder(24, 32, 0, 32));
        topo.add(Marca.criar(true), BorderLayout.WEST);
        JLabel selo = new JLabel("APS 2026.2 · UFPB Campus IV · Grupo 12");
        selo.setFont(Tema.sans(12, Font.PLAIN));
        selo.setForeground(new Color(0x94A3B8));
        topo.add(selo, BorderLayout.EAST);
        raiz.add(topo, BorderLayout.NORTH);
        JPanel centro = new JPanel(new GridBagLayout());
        centro.setOpaque(false);
        centro.add(formulario());
        raiz.add(centro, BorderLayout.CENTER);
        return raiz;
    }

    private JPanel formulario() {
        JPanel f = new JPanel();
        f.setBackground(Color.WHITE);
        f.setLayout(new BoxLayout(f, BoxLayout.Y_AXIS));
        f.setBorder(new CompoundBorder(new LineBorder(new Color(0x1E2A4A), 1, true), new EmptyBorder(34, 38, 30, 38)));
        f.setPreferredSize(new Dimension(420, 500));

        JComponent icone = Marca.icone(48);
        JLabel bem = Tema.titulo("Entrar no StockFlow");
        JLabel sub = Tema.subtitulo("Controle de estoque, vendas e devoluções");
        Tema.estilizarCampo(senha);
        JButton entrar = Tema.botaoPrimario("Entrar  →");
        entrar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        entrar.addActionListener(e -> entrar());
        getRootPane().setDefaultButton(entrar);

        JPanel demo = new JPanel(new GridLayout(4, 1, 0, 4));
        demo.setBackground(Tema.AZUL_CLARO);
        demo.setBorder(new CompoundBorder(new MatteBorder(0, 3, 0, 0, Tema.AZUL), new EmptyBorder(10, 14, 10, 14)));
        JLabel dt = new JLabel("CONTAS DE DEMONSTRAÇÃO (clique para preencher)");
        dt.setFont(Tema.sans(10, Font.BOLD));
        dt.setForeground(Tema.AZUL_ESCURO);
        demo.add(dt);
        demo.add(linkDemo("Gerente — gerente@stockflow.demo", "gerente@stockflow.demo"));
        demo.add(linkDemo("Vendedora — vendedora@stockflow.demo", "vendedora@stockflow.demo"));
        JLabel senhaDemo = new JLabel("Senha: " + ContasDemo.SENHA_DEMO);
        senhaDemo.setFont(Tema.sans(12, Font.PLAIN));
        senhaDemo.setForeground(Tema.TEXTO_SUAVE);
        demo.add(senhaDemo);

        for (JComponent c : new JComponent[]{icone, bem, sub, email, senha, entrar, demo})
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
        icone.setMaximumSize(new Dimension(48, 48));
        email.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        senha.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        demo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        f.add(icone); f.add(Box.createVerticalStrut(16));
        f.add(bem); f.add(Box.createVerticalStrut(4)); f.add(sub);
        f.add(Box.createVerticalStrut(24)); f.add(alinhar(Tema.rotulo("E-mail"))); f.add(Box.createVerticalStrut(6)); f.add(email);
        f.add(Box.createVerticalStrut(14)); f.add(alinhar(Tema.rotulo("Senha"))); f.add(Box.createVerticalStrut(6)); f.add(senha);
        f.add(Box.createVerticalStrut(20)); f.add(entrar);
        f.add(Box.createVerticalStrut(18)); f.add(demo);
        return f;
    }

    private static JComponent alinhar(JComponent c) { c.setAlignmentX(Component.LEFT_ALIGNMENT); return c; }

    /** Clicar numa conta de demonstração apenas preenche o e-mail (a senha é digitada). */
    private JLabel linkDemo(String texto, String emailDemo) {
        JLabel l = new JLabel(texto);
        l.setFont(Tema.sans(12, Font.PLAIN));
        l.setForeground(Tema.TEXTO);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                email.setText(emailDemo);
                senha.requestFocusInWindow();
            }
        });
        return l;
    }

    private void entrar() {
        Usuario[] usuario = new Usuario[1];
        boolean ok = Tema.executar(this,
            () -> usuario[0] = autenticacao.entrar(email.getText(), new String(senha.getPassword())), null);
        senha.setText("");
        if (!ok) return;
        new JanelaPrincipal(new Contexto(facade, descontos, alerta, usuario[0]),
            () -> new JanelaLogin(facade, autenticacao, descontos, alerta).setVisible(true)).setVisible(true);
        dispose();
    }
}
