package br.ufpb.aps.grupo12.persistencia;

import java.util.function.Function;

/**
 * Implementação em memória do contrato Repositorio, com a mesma semântica de "tudo ou nada"
 * (a operação trabalha numa cópia; só substitui o estado se terminar sem exceção).
 * Usada para gerar os dados de demonstração e em testes que não precisam de arquivo.
 */
public final class RepositorioMemoria implements Repositorio {
    private EstadoDados estado = new EstadoDados();
    @Override public synchronized <T> T consultar(Function<EstadoDados, T> fn) { return fn.apply(estado.copiar()); }
    @Override public synchronized <T> T transacionar(Function<EstadoDados, T> fn) {
        EstadoDados trabalho = estado.copiar();
        T resultado = fn.apply(trabalho);
        estado = trabalho;
        return resultado;
    }
}
