package br.ufpb.aps.grupo12.dominio;

public record Produto(String id, String nome, long precoCentavos, int estoque, int estoqueMinimo,
                      String categoriaId, long custoCentavos) {
    public Produto {
        Validacao.id(id);
        if (nome == null || nome.isBlank() || nome.length() > 100)
            throw new ErroNegocio("Nome do produto obrigatório e com até 100 caracteres.");
        if (precoCentavos <= 0) throw new ErroNegocio("Preço deve ser positivo.");
        if (estoque < 0 || estoqueMinimo < 0) throw new ErroNegocio("Estoque e mínimo não podem ser negativos.");
        if (categoriaId == null || categoriaId.isBlank()) categoriaId = Categoria.GERAL;
        Validacao.id(categoriaId);
        if (custoCentavos < 0) throw new ErroNegocio("Custo não pode ser negativo.");
    }
    /** Produto sem categoria/custo informados (categoria GERAL, custo zero). */
    public Produto(String id, String nome, long precoCentavos, int estoque, int estoqueMinimo) {
        this(id, nome, precoCentavos, estoque, estoqueMinimo, Categoria.GERAL, 0);
    }
    /** Regra de alerta: saldo igual ou abaixo do mínimo configurado. */
    public boolean emAlerta() { return estoque <= estoqueMinimo; }
    public Produto comEstoque(int saldo) {
        return new Produto(id, nome, precoCentavos, saldo, estoqueMinimo, categoriaId, custoCentavos);
    }
    /** Margem bruta unitária (preço − custo). */
    public long margemCentavos() { return precoCentavos - custoCentavos; }
}
