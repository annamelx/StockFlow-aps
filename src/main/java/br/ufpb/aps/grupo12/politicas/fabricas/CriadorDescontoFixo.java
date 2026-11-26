package br.ufpb.aps.grupo12.politicas.fabricas;

import br.ufpb.aps.grupo12.dominio.Dinheiro;
import br.ufpb.aps.grupo12.politicas.DescontoFixo;
import br.ufpb.aps.grupo12.politicas.PoliticaDesconto;

/** Creator concreto: produz {@link DescontoFixo} a partir de um valor em reais. */
public final class CriadorDescontoFixo extends CriadorPoliticaDesconto {
    @Override protected PoliticaDesconto fabricar(String parametro) {
        return new DescontoFixo(Dinheiro.lerReais(parametro));
    }
    @Override public String descricao() { return "Valor fixo"; }
    @Override public String rotuloParametro() { return "Valor (R$)"; }
}
