package org.example.sistema_canchas_deportivas.domain.models;

import org.example.sistema_canchas_deportivas.domain.exception.DatoInvalidoException;

import java.time.LocalDateTime;
/** Cantidad solicitada de un implemento para el intervalo de una reserva. */
public final class ReservaUtileria {
    private final LocalDateTime creado;
    private final LocalDateTime modificado;
    private final Long id;
    private final Long reservaId;
    private final Long utileriaId;
    private final int cantidad;

    // Constructor compatible con las fases previas: las fechas aún no se conocen.
    public ReservaUtileria(Long id, Long reservaId, Long utileriaId, int cantidad) {
        this(id, reservaId, utileriaId, cantidad, null, null);
    }

    // Constructor completo para reconstruir el dominio desde persistencia.
    public ReservaUtileria(Long id, Long reservaId, Long utileriaId, int cantidad,
                   LocalDateTime creado, LocalDateTime modificado) {
        this.creado = creado;
        this.modificado = modificado;
        this.id = id;
        this.reservaId = reservaId;
        this.utileriaId = utileriaId;
        this.cantidad = cantidad;
    }

    public static ReservaUtileria nuevo(Long utileriaId, int cantidad) {
        if (utileriaId == null || utileriaId <= 0 || cantidad <= 0) {
            throw new DatoInvalidoException(
                    "El ID de utilería y la cantidad deben ser positivos.");
        }
        return new ReservaUtileria(null, null, utileriaId, cantidad);
    }

    public Long getId() {
        return id;
    }

    public Long getReservaId() {
        return reservaId;
    }

    public Long getUtileriaId() {
        return utileriaId;
    }

    public int getCantidad() {
        return cantidad;
    }
    public LocalDateTime getCreado() { return creado; }
    public LocalDateTime getModificado() { return modificado; }
}

