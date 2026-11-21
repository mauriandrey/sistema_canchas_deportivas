package org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos;

import java.time.*;
import java.util.List;
import org.example.sistema_canchas_deportivas.domain.models.*;

public record ReservaResponse(Long id, Long responsableId, Long canchaId, Disciplina disciplina,
        LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, int duracion, EstadoReserva estado,
        LocalDateTime fechaCreacion, LocalDateTime creado, LocalDateTime modificado,
        List<ReservaUtileriaResponse> utileria) {}

