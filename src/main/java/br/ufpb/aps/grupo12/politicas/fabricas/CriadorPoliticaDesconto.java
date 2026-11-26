package br.ufpb.aps.grupo12.politicas.fabricas;

import br.ufpb.aps.grupo12.dominio.ErroNegocio;
import br.ufpb.aps.grupo12.politicas.PoliticaDesconto;

/**
 * GoF Factory Method (criação) — classe "Creator".
 *
 * Problema: a interface (console ou gráfica) precisava escolher e montar a política de desconto
 * com if/else ("1 = percentual, 2 = fixo"), conhecendo os construtores e a conversão de cada valor.
 * Cada nova política obrigava a mexer nas telas.
 *
 * Solução: a operação {@link #fabricar(String)} é abstrata e SOBRESCRITA por cada criador concreto.
 * O método-modelo {@link #criar(String)} contém o fluxo comum (validação da entrada e do produto),
 * mas delega a decisão de QUAL classe instanciar às subclasses.
 */
public abstract class CriadorPoliticaDesconto {

    /** Fluxo comum de criação; não é sobrescrito. */
    public final PoliticaDesconto criar(String parametro) {
        String entrada = parametro == null ? "" : parametro.trim();
        if (exigeParametro() && entrada.isEmpty())
            throw new ErroNegocio("Informe " + rotuloParametro().toLowerCase() + ".");
        PoliticaDesconto politica = fabricar(entrada);
        if (politica == null) throw new IllegalStateException("Criador não produziu política: " + getClass());
        return politica;
    }

    /** Factory Method: cada subclasse decide qual Strategy concreta instanciar. */
    protected abstract PoliticaDesconto fabricar(String parametro);

    /** Nome exibido na interface. */
    public abstract String descricao();

    /** Rótulo do campo de parâmetro (ex.: "Percentual (%)"). */
    public String rotuloParametro() { return "Parâmetro"; }

    /** Algumas políticas (ex.: faixa padrão) não precisam de valor digitado. */
    public boolean exigeParametro() { return true; }

    @Override public String toString() { return descricao(); }
}
