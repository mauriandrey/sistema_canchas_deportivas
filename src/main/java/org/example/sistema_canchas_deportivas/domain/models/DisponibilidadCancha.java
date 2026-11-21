package org.example.sistema_canchas_deportivas.domain.models;

import java.time.LocalTime;
import java.util.List;
import org.example.sistema_canchas_deportivas.domain.models.Cancha;

public record DisponibilidadCancha(Cancha cancha, List<Bloque> horarios) {
    public DisponibilidadCancha { horarios = List.copyOf(horarios); }
    public record Bloque(LocalTime inicio, LocalTime fin) {}
}
