package br.ufpb.aps.grupo12.politicas;

import br.ufpb.aps.grupo12.dominio.ErroNegocio;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Extensão do Strategy (3ª opção): desconto progressivo pelo valor da compra.
 * Padrão da loja: a partir de R$ 300,00 → 5%; a partir de R$ 500,00 → 10%.
 *
 * Acrescentada SEM alterar ServicoLoja, Venda, ItemVenda ou PoliticaDesconto:
 * o serviço continua chamando apenas descontoEmCentavos(subtotal).
 */
public final class DescontoPorFaixa implements PoliticaDesconto {
    private final NavigableMap<Long, BigDecimal> faixas;

    /** Faixas padrão da loja. */
    public static DescontoPorFaixa padraoDaLoja() {
        return new DescontoPorFaixa(Map.of(30000L, new BigDecimal("5"), 50000L, new BigDecimal("10")));
    }

    /** @param faixas mínimo do subtotal em centavos → percentual aplicado a partir desse valor */
    public DescontoPorFaixa(Map<Long, BigDecimal> faixas) {
        if (faixas == null || faixas.isEmpty()) throw new ErroNegocio("Informe ao menos uma faixa.");
        this.faixas = new TreeMap<>();
        for (var faixa : faixas.entrySet()) {
            BigDecimal pct = faixa.getValue();
            if (faixa.getKey() == null || faixa.getKey() < 0 || pct == null
                || pct.signum() < 0 || pct.compareTo(new BigDecimal("100")) > 0)
                throw new ErroNegocio("Faixa de desconto inválida.");
            this.faixas.put(faixa.getKey(), pct);
        }
    }

    @Override public long descontoEmCentavos(long subtotal) {
        var faixa = faixas.floorEntry(subtotal);
        if (faixa == null) return 0;
        return BigDecimal.valueOf(subtotal).multiply(faixa.getValue())
            .divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP).longValueExact();
    }

    @Override public String nome() {
        StringBuilder sb = new StringBuilder("Progressivo");
        faixas.forEach((min, pct) -> sb.append(" ≥").append(min / 100).append(':')
            .append(pct.stripTrailingZeros().toPlainString()).append('%'));
        return sb.toString();
    }
}
