package org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos;
import java.time.*;
import java.util.List;
import org.example.sistema_canchas_deportivas.domain.models.Disciplina;

public record UtileriaDisponibilidadResponse(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
        int duracion, Disciplina disciplina, List<ImplementoDisponible> implementos) {
    public record ImplementoDisponible(UtileriaResponse implemento, long comprometidas, int disponibles) {}
}
