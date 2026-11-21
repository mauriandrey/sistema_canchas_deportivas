package org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos;

import java.time.*;
import jakarta.validation.constraints.NotNull;
public record HorarioCrearRequest(@NotNull DayOfWeek dia, @NotNull LocalTime apertura, @NotNull LocalTime cierre) {}
