package org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos;
import java.time.LocalDateTime;
public record ReservaUtileriaResponse(Long id, Long utileriaId, int cantidad, LocalDateTime creado, LocalDateTime modificado) {}
