package org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos;
import jakarta.validation.constraints.*;
public record ClienteRequest(
        @org.example.sistema_canchas_deportivas.infraestructure.validation.CedulaValida String cedula,
        @NotBlank(message = "Los nombres son obligatorios.") @Size(max = 100, message = "Los nombres admiten hasta 100 caracteres.") String nombres,
        @NotBlank(message = "Los apellidos son obligatorios.") @Size(max = 100, message = "Los apellidos admiten hasta 100 caracteres.") String apellidos,
        @NotBlank(message = "El email es obligatorio.") @Email(message = "El email no tiene un formato válido.")
        @Size(max = 100, message = "El email admite hasta 100 caracteres.") String email,
        @Pattern(regexp = "^(?:[0-9]{7,10})?$", message = "El teléfono debe tener entre 7 y 10 dígitos.") String telefono) {}
