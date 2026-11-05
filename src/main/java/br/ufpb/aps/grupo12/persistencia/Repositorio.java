package br.ufpb.aps.grupo12.persistencia;
import java.util.function.Function;

public interface Repositorio {
    <T> T consultar(Function<EstadoDados, T> consulta);
    <T> T transacionar(Function<EstadoDados, T> operacao);
}
