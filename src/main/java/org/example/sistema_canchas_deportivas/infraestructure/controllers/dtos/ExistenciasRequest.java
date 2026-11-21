package org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos;
import jakarta.validation.constraints.*;
public record ExistenciasRequest(@NotNull(message = "Las existencias son obligatorias.") @Min(value = 0, message = "Las existencias no pueden ser negativas.") Integer existencias) {}
