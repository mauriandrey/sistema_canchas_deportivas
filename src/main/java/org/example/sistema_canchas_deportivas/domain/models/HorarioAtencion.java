package org.example.sistema_canchas_deportivas.domain.models;

import java.time.*;
import org.example.sistema_canchas_deportivas.domain.exception.DatoInvalidoException;

public final class HorarioAtencion {
    private final LocalDateTime creado;
    private final LocalDateTime modificado;
    private final Long id;
    private final DayOfWeek dia;
    private final LocalTime apertura;
    private final LocalTime cierre;

    // Constructor compatible con las fases previas: las fechas aún no se conocen.
    public HorarioAtencion(Long id, DayOfWeek dia, LocalTime apertura, LocalTime cierre) {
        this(id, dia, apertura, cierre, null, null);
    }

    // Constructor completo para reconstruir el dominio desde persistencia.
    public HorarioAtencion(Long id, DayOfWeek dia, LocalTime apertura, LocalTime cierre,
                   LocalDateTime creado, LocalDateTime modificado) {
        this.creado = creado;
        this.modificado = modificado;
        if (dia == null || apertura == null || cierre == null || !apertura.isBefore(cierre)
                || apertura.getSecond() != 0 || apertura.getNano() != 0
                || cierre.getSecond() != 0 || cierre.getNano() != 0) {
            throw new DatoInvalidoException("Día y horas obligatorios, con precisión de minutos y apertura anterior al cierre.");
        }
        this.id = id;
        this.dia = dia;
        this.apertura = apertura;
        this.cierre = cierre;
    }

    public static HorarioAtencion nuevo(DayOfWeek dia, LocalTime apertura, LocalTime cierre) {
        return new HorarioAtencion(null, dia, apertura, cierre, null, null);
    }

    public HorarioAtencion actualizar(LocalTime apertura, LocalTime cierre) {
        return new HorarioAtencion(id, dia, apertura, cierre, creado, modificado);
    }

    public boolean contiene(LocalDateTime inicio, LocalDateTime fin) {
        return inicio.getDayOfWeek() == dia && inicio.toLocalDate().equals(fin.toLocalDate())
                && !inicio.toLocalTime().isBefore(apertura) && !fin.toLocalTime().isAfter(cierre);
    }

    public Long getId() { return id; }
    public DayOfWeek getDia() { return dia; }
    public LocalTime getApertura() { return apertura; }
    public LocalTime getCierre() { return cierre; }
    public LocalDateTime getCreado() { return creado; }
    public LocalDateTime getModificado() { return modificado; }
}

