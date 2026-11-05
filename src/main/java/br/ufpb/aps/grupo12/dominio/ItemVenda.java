package br.ufpb.aps.grupo12.dominio;

import java.math.BigInteger;
public record ItemVenda(String produtoId, int quantidade, long precoUnitarioCentavos, long descontoCentavos) {
    public ItemVenda {
        Validacao.id(produtoId);
        Validacao.quantidadePositiva(quantidade);
        if (precoUnitarioCentavos <= 0 || descontoCentavos < 0)
            throw new ErroNegocio("Preço/desconto inválido para o item.");
        if (descontoCentavos > Math.multiplyExact(precoUnitarioCentavos, quantidade))
            throw new ErroNegocio("Desconto do item supera o valor bruto.");
    }
    public long brutoCentavos() { return Math.multiplyExact(precoUnitarioCentavos, quantidade); }
    public long liquidoCentavos() { return brutoCentavos() - descontoCentavos; }
    /** Total cumulativo dos centavos restituíveis até certa quantidade. */
    public long restituiAte(int quantidadeDevolvida) {
        if (quantidadeDevolvida < 0 || quantidadeDevolvida > quantidade)
            throw new ErroNegocio("Quantidade de devolução fora do intervalo.");
        return BigInteger.valueOf(liquidoCentavos()).multiply(BigInteger.valueOf(quantidadeDevolvida))
            .divide(BigInteger.valueOf(quantidade)).longValueExact();
    }
}
