package org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos;

import java.time.*;
public record HorarioAtencionResponse(Long id, DayOfWeek dia, LocalTime apertura, LocalTime cierre) {}

