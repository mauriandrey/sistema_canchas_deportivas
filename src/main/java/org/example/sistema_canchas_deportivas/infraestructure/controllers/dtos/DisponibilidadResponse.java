package org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos;

import java.time.*;
import java.util.List;
import org.example.sistema_canchas_deportivas.domain.models.Disciplina;

public record DisponibilidadResponse(LocalDate fecha, Disciplina disciplina, int duracion,
                                     List<CanchaDisponible> canchas) {
    public record CanchaDisponible(CanchaResponse cancha, List<Bloque> horarios) {}
    public record Bloque(LocalTime inicio, LocalTime fin) {}
}
