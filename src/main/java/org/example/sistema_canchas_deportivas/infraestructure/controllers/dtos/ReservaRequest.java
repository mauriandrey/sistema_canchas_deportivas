package org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import java.time.LocalTime;
import jakarta.validation.constraints.*;
import org.example.sistema_canchas_deportivas.domain.models.Disciplina;

public record ReservaRequest(@NotNull(message = "La cancha es obligatoria.") @Positive(message = "El ID de cancha debe ser positivo.") Long canchaId,
        @NotNull(message = "La disciplina es obligatoria.") Disciplina disciplina, @NotNull(message = "La fecha es obligatoria.") LocalDate fecha, @NotNull(message = "La hora de inicio es obligatoria.") LocalTime horaInicio,
        @NotNull(message = "La duración es obligatoria.") @Min(value = 1, message = "La duración debe ser de 1 o 2 horas.") @Max(value = 2, message = "La duración debe ser de 1 o 2 horas.") Integer duracion,
        List<@NotNull(message = "El detalle de utilería no puede ser null.") @Valid ReservaUtileriaRequest> utileria) {}

