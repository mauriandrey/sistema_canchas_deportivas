package org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos;

import jakarta.validation.constraints.NotNull;
public record EstadoCanchaRequest(@NotNull(message = "Indica si la cancha está activa.") Boolean activa) {}
