package br.ufpb.aps.grupo12.aplicacao;

import java.time.*;

/** Relógio cujo horário pode ser definido: gera histórico de demonstração com datas passadas. */
public final class RelogioAjustavel extends Clock {
    private final ZoneId zona;
    private Instant instante;
    public RelogioAjustavel(LocalDateTime inicio, ZoneId zona) { this.zona = zona; ajustar(inicio); }
    public void ajustar(LocalDateTime quando) { instante = quando.atZone(zona).toInstant(); }
    @Override public ZoneId getZone() { return zona; }
    @Override public Clock withZone(ZoneId z) { return new RelogioAjustavel(LocalDateTime.ofInstant(instante, zona), z); }
    @Override public Instant instant() { return instante; }
}
