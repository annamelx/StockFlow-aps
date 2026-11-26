package br.ufpb.aps.grupo12.politicas.fabricas;

import java.util.List;
import java.util.Objects;

/**
 * Lista de criadores disponíveis, montada no App e injetada nas interfaces.
 * Para acrescentar uma política nova basta registrar mais um criador aqui (no App);
 * telas e serviço não mudam.
 */
public final class CatalogoDescontos {
    private final List<CriadorPoliticaDesconto> criadores;
    public CatalogoDescontos(List<CriadorPoliticaDesconto> criadores) {
        this.criadores = List.copyOf(Objects.requireNonNull(criadores));
        if (this.criadores.isEmpty()) throw new IllegalArgumentException("Catálogo vazio.");
    }
    public static CatalogoDescontos padrao() {
        return new CatalogoDescontos(List.of(new CriadorDescontoPercentual(),
            new CriadorDescontoFixo(), new CriadorDescontoPorFaixa()));
    }
    public List<CriadorPoliticaDesconto> criadores() { return criadores; }
}
