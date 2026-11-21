package org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos;
import jakarta.validation.constraints.*;
public record ReservaUtileriaRequest(@NotNull(message = "El implemento es obligatorio.") @Positive(message = "El ID del implemento debe ser positivo.") Long utileriaId, @NotNull(message = "La cantidad es obligatoria.") @Positive(message = "La cantidad debe ser positiva.") Integer cantidad) {}
