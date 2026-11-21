package org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos;
import java.time.LocalDateTime;
public record ClienteResponse(Long id, String cedula, String nombres, String apellidos,
        String email, String telefono, LocalDateTime creado, LocalDateTime modificado) {}
