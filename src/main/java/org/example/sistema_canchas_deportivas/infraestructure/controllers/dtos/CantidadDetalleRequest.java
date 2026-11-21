package org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos;

import jakarta.validation.constraints.*;
public record CantidadDetalleRequest(@NotNull @Positive Integer cantidad) {}
