package org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos;

import java.time.*;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.example.sistema_canchas_deportivas.domain.models.*;
public record ReservaAdminRequest(@NotNull @Positive Long responsableId, @NotNull @Positive Long canchaId,
        @NotNull Disciplina disciplina, @NotNull LocalDate fecha, @NotNull LocalTime horaInicio,
        @NotNull @Min(1) @Max(2) Integer duracion, EstadoReserva estado,
        List<@NotNull @Valid ReservaUtileriaRequest> utileria) {}
