package org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos;
import java.util.Set;
import jakarta.validation.constraints.*;
import org.example.sistema_canchas_deportivas.domain.models.*;

public record UtileriaRequest(@NotBlank(message = "El nombre es obligatorio.") @Size(max = 100, message = "El nombre admite hasta 100 caracteres.") String nombre, @NotNull(message = "El tipo de implemento es obligatorio.") TipoImplemento tipo,
        @NotNull(message = "Las existencias son obligatorias.") @Min(value = 0, message = "Las existencias no pueden ser negativas.") Integer existencias, @NotNull(message = "Indica si el implemento está activo.") Boolean activa,
        @NotEmpty(message = "Indica al menos una disciplina compatible.") Set<@NotNull(message = "La disciplina no puede ser null.") Disciplina> disciplinasCompatibles) {}

