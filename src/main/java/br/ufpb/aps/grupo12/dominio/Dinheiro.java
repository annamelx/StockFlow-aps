package br.ufpb.aps.grupo12.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Conversão entre centavos (representação interna, sempre {@code long}) e texto em reais.
 * Centraliza formatação e leitura para que interface e console não façam contas com double.
 */
public final class Dinheiro {
    private static final Locale BRASIL = Locale.of("pt", "BR");
    private Dinheiro() {}

    /** 12345 → "R$ 123,45". */
    public static String formatar(long centavos) {
        NumberFormat formato = NumberFormat.getCurrencyInstance(BRASIL);
        return formato.format(BigDecimal.valueOf(centavos, 2)).replace(' ', ' ');
    }

    /** "23,50", "23.50", "R$ 1.234,56" → centavos. Rejeita valores com mais de 2 casas. */
    public static long lerReais(String texto) {
        if (texto == null || texto.isBlank()) throw new ErroNegocio("Informe um valor em reais.");
        String limpo = texto.replace("R$", "").replace(" ", "").trim();
        if (limpo.contains(",")) limpo = limpo.replace(".", "").replace(',', '.');
        try {
            BigDecimal reais = new BigDecimal(limpo);
            if (reais.scale() > 2) throw new ErroNegocio("Use no máximo duas casas decimais.");
            return reais.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            throw new ErroNegocio("Valor monetário inválido: " + texto);
        }
    }
}
