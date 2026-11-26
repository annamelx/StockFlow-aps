package br.ufpb.aps.grupo12.politicas.fabricas;

import br.ufpb.aps.grupo12.dominio.ErroNegocio;
import br.ufpb.aps.grupo12.politicas.DescontoPercentual;
import br.ufpb.aps.grupo12.politicas.PoliticaDesconto;
import java.math.BigDecimal;

/** Creator concreto: produz {@link DescontoPercentual}. */
public final class CriadorDescontoPercentual extends CriadorPoliticaDesconto {
    @Override protected PoliticaDesconto fabricar(String parametro) {
        try {
            return new DescontoPercentual(new BigDecimal(parametro.replace(',', '.').replace("%", "")));
        } catch (NumberFormatException e) {
            throw new ErroNegocio("Percentual inválido: " + parametro);
        }
    }
    @Override public String descricao() { return "Percentual"; }
    @Override public String rotuloParametro() { return "Percentual (%)"; }
}
