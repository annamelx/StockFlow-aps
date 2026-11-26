package br.ufpb.aps.grupo12.interfacegrafica;

import java.awt.*;
import javax.swing.*;

/** Cartão de indicador (KPI): ícone, rótulo, valor e rodapé. */
final class CartaoIndicador extends Cartao {
    private final JLabel valor = new JLabel("–");
    private final JLabel rodape = new JLabel(" ");
    CartaoIndicador(Icone.Tipo icone, String rotulo, String sub, Color cor) {
        super(new BorderLayout(0, 10));
        JPanel topo = new JPanel(new BorderLayout(10, 0));
        topo.setOpaque(false);
        topo.add(Componentes.selo(icone, cor), BorderLayout.WEST);
        JPanel textos = new JPanel(new GridLayout(2, 1));
        textos.setOpaque(false);
        JLabel r = new JLabel(rotulo.toUpperCase());
        r.setFont(Tema.sans(11, Font.BOLD));
        r.setForeground(cor);
        JLabel s = new JLabel(sub);
        s.setFont(Tema.sans(11, Font.PLAIN));
        s.setForeground(Tema.TEXTO_SUAVE);
        textos.add(r); textos.add(s);
        topo.add(textos, BorderLayout.CENTER);
        add(topo, BorderLayout.NORTH);
        valor.setFont(Tema.titulo(24));
        valor.setForeground(Tema.TEXTO);
        add(valor, BorderLayout.CENTER);
        rodape.setFont(Tema.sans(11, Font.PLAIN));
        rodape.setForeground(Tema.TEXTO_SUAVE);
        add(rodape, BorderLayout.SOUTH);
    }
    void valor(String v) { valor.setText(v); }
    void valor(String v, Color cor) { valor.setText(v); valor.setForeground(cor); }
    void rodape(String r) { rodape.setText(r == null || r.isEmpty() ? " " : r); }
}
