package org.example.sistema_canchas_deportivas.domain.models;

import java.time.*;
import java.util.*;

/** Ocupación confirmada de un implemento, sin datos personales. */
public record CompromisoUtileria(Disciplina disciplina, LocalDate fecha, LocalTime horaInicio,
                                int duracionHoras, int cantidad) {
    public LocalDateTime inicio() { return fecha.atTime(horaInicio); }
    public LocalDateTime fin() { return inicio().plusHours(duracionHoras); }

    /** Pico simultáneo en [desde, hasta). Un hasta null permite revisar todas las reservas futuras. */
    public static long maximoSimultaneo(List<CompromisoUtileria> compromisos, LocalDateTime desde, LocalDateTime hasta) {
        Map<LocalDateTime, Long> eventos = new TreeMap<>();
        for (var compromiso : compromisos) {
            LocalDateTime inicio = compromiso.inicio().isBefore(desde) ? desde : compromiso.inicio();
            LocalDateTime fin = hasta != null && compromiso.fin().isAfter(hasta) ? hasta : compromiso.fin();
            if (!inicio.isBefore(fin)) continue;
            eventos.merge(inicio, (long) compromiso.cantidad(), Long::sum);
            eventos.merge(fin, -(long) compromiso.cantidad(), Long::sum);
        }
        long ocupadas = 0, maximo = 0;
        for (long cambio : eventos.values()) {
            ocupadas += cambio;
            maximo = Math.max(maximo, ocupadas);
        }
        return maximo;
    }
}
