package org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

public record UsuarioActualizarRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{3,50}") String username,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @Size(min = 12, max = 72) String password,
        @NotNull Boolean activo, @Positive Long clienteId) {
    @Override public String toString() { return "UsuarioActualizarRequest[password=PROTEGIDO]"; }
}
