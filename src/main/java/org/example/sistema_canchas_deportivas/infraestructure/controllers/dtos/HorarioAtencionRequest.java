package org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos;

import java.time.LocalTime;
import jakarta.validation.constraints.NotNull;

public record HorarioAtencionRequest(@NotNull(message = "La hora de apertura es obligatoria.") LocalTime apertura, @NotNull(message = "La hora de cierre es obligatoria.") LocalTime cierre) {}

