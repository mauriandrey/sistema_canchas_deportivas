package org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos;

import jakarta.validation.constraints.*;
import org.example.sistema_canchas_deportivas.domain.models.*;

public record CanchaRequest(
        @NotBlank(message = "El nombre es obligatorio.") @Size(max = 100, message = "El nombre admite hasta 100 caracteres.") String nombre,
        @NotNull(message = "El tipo de cancha es obligatorio.") TipoCancha tipo,
        Superficie superficie,
        @NotNull(message = "Indica si la cancha está activa.") Boolean activa) {}

