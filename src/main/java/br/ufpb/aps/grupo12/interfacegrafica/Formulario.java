package br.ufpb.aps.grupo12.interfacegrafica;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Diálogo simples de formulário (rótulo + campo). */
final class Formulario {
    private Formulario() {}
    static boolean mostrar(Component pai, String titulo, String[] rotulos, JComponent[] campos) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(8, 4, 8, 4));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 4, 5, 4);
        c.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < rotulos.length; i++) {
            c.gridy = i; c.gridx = 0; c.weightx = 0; c.fill = GridBagConstraints.NONE;
            p.add(Tema.rotulo(rotulos[i]), c);
            c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
            p.add(campos[i], c);
        }
        return JOptionPane.showConfirmDialog(pai, p, titulo, JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }
}
