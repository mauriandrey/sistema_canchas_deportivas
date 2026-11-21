package org.example.sistema_canchas_deportivas.domain.models;

import java.time.*;
import org.example.sistema_canchas_deportivas.domain.models.Disciplina;

/** Proyección de ocupación confirmada: no contiene datos del responsable ni utilería. */
public record OcupacionCancha(Long canchaId, Disciplina disciplina, LocalDate fecha,
                             LocalTime horaInicio, int duracionHoras) {
    public LocalDateTime inicio() { return fecha.atTime(horaInicio); }
    public LocalDateTime fin() { return inicio().plusHours(duracionHoras); }
    public boolean seSuperpone(LocalDateTime inicio, LocalDateTime fin) {
        return inicio.isBefore(fin()) && fin.isAfter(inicio());
    }
}
