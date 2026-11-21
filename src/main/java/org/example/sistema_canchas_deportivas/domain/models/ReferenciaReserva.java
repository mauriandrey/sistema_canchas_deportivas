package org.example.sistema_canchas_deportivas.domain.models;
import java.time.LocalDate;
/** Datos inmutables para adquirir bloqueos en orden, sin cargar una entidad JPA antes del bloqueo. */
public record ReferenciaReserva(Long responsableId, Long canchaId, LocalDate fecha) {}
