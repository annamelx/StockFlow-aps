package br.ufpb.aps.grupo12.dominio;

/** Forma de pagamento registrada na venda (simulada: não há integração com operadora). */
public enum FormaPagamento {
    DINHEIRO("Dinheiro"), PIX("Pix"), CARTAO_CREDITO("Cartão crédito"), CARTAO_DEBITO("Cartão débito");
    private final String rotulo;
    FormaPagamento(String rotulo) { this.rotulo = rotulo; }
    public String rotulo() { return rotulo; }
    @Override public String toString() { return rotulo; }
}
