package org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "El username es obligatorio.") @Size(max = 50, message = "El username admite hasta 50 caracteres.") String username,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank(message = "La contraseña es obligatoria.") @Size(max = 72, message = "La contraseña admite hasta 72 caracteres y no debe superar 72 bytes UTF-8.") String password) {
    @Override public String toString() { return "LoginRequest[password=PROTEGIDO]"; }
}

