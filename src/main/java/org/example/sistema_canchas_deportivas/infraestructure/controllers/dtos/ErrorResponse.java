package org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(String codigo, String mensaje, Instant fecha, Map<String, String> detalles) {
}
