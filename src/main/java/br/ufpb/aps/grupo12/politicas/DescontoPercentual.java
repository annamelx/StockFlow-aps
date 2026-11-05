package br.ufpb.aps.grupo12.politicas;

import br.ufpb.aps.grupo12.dominio.ErroNegocio;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class DescontoPercentual implements PoliticaDesconto {
    private final BigDecimal percentual;
    public DescontoPercentual(BigDecimal percentual) {
        if (percentual == null || percentual.signum() < 0
            || percentual.compareTo(new BigDecimal("100")) > 0)
            throw new ErroNegocio("Desconto deve estar entre 0% e 100%.");
        this.percentual = percentual;
    }
    @Override public long descontoEmCentavos(long subtotal) {
        return BigDecimal.valueOf(subtotal).multiply(percentual)
            .divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP).longValueExact();
    }
    @Override public String nome() { return "Percentual " + percentual.toPlainString() + "%"; }
}
