package br.ufpb.aps.grupo12.politicas;

import br.ufpb.aps.grupo12.dominio.ErroNegocio;
/** Nova opção de Strategy: acrescentada sem alterar o serviço de vendas. */
public final class DescontoFixo implements PoliticaDesconto {
    private final long valorCentavos;
    public DescontoFixo(long valorCentavos) {
        if (valorCentavos < 0) throw new ErroNegocio("Desconto fixo negativo.");
        this.valorCentavos = valorCentavos;
    }
    @Override public long descontoEmCentavos(long subtotal) {
        if (valorCentavos > subtotal) throw new ErroNegocio("Desconto não pode superar subtotal.");
        return valorCentavos;
    }
    @Override public String nome() { return "Desconto fixo"; }
}
