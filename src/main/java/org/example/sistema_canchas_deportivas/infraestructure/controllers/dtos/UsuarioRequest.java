package org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotBlank(message = "El username es obligatorio.")
        @Pattern(regexp = "[A-Za-z0-9._-]{3,50}", message = "Usa de 3 a 50 letras, números, puntos, guiones o guiones bajos.")
        String username,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(min = 12, max = 72, message = "La contraseña debe tener entre 12 y 72 caracteres y no superar 72 bytes UTF-8.")
        String password,
        @org.example.sistema_canchas_deportivas.infraestructure.validation.CedulaValida String cedula,
        @NotBlank @Size(max = 100) String nombres,
        @NotBlank @Size(max = 100) String apellidos,
        @NotBlank @jakarta.validation.constraints.Email @Size(max = 100) String email,
        @NotBlank @Pattern(regexp = "[0-9]{7,10}") String telefono) {
    @Override public String toString() { return "UsuarioRequest[password=PROTEGIDO]"; }
}

