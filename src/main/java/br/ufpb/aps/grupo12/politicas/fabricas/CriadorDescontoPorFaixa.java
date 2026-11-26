package br.ufpb.aps.grupo12.politicas.fabricas;

import br.ufpb.aps.grupo12.politicas.DescontoPorFaixa;
import br.ufpb.aps.grupo12.politicas.PoliticaDesconto;

/** Creator concreto da extensão: produz o desconto progressivo padrão da loja. */
public final class CriadorDescontoPorFaixa extends CriadorPoliticaDesconto {
    @Override protected PoliticaDesconto fabricar(String parametro) {
        return DescontoPorFaixa.padraoDaLoja();
    }
    @Override public String descricao() { return "Progressivo (≥R$300: 5%, ≥R$500: 10%)"; }
    @Override public boolean exigeParametro() { return false; }
    @Override public String rotuloParametro() { return "Sem parâmetro"; }
}
