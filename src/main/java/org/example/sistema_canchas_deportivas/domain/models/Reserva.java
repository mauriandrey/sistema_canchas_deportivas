package org.example.sistema_canchas_deportivas.domain.models;

import org.example.sistema_canchas_deportivas.domain.exception.DatoInvalidoException;
import org.example.sistema_canchas_deportivas.domain.exception.ReglaNegocioException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Reserva inmutable: reglas propias en el dominio; conflictos persistidos en el caso de uso. */
public final class Reserva {
    private final LocalDateTime creado;
    private final LocalDateTime modificado;
    private final Long id;
    private final Long responsableId;
    private final Long canchaId;
    private final Disciplina disciplina;
    private final LocalDateTime inicio;
    private final int duracionHoras;
    private final EstadoReserva estado;
    private final LocalDateTime fechaCreacion;
    private final List<ReservaUtileria> utileria;

    // Constructor compatible con las fases previas: las fechas aún no se conocen.
    public Reserva(Long id, Long responsableId, Long canchaId, Disciplina disciplina, LocalDateTime inicio, int duracionHoras, EstadoReserva estado, LocalDateTime fechaCreacion, List<ReservaUtileria> utileria) {
        this(id, responsableId, canchaId, disciplina, inicio, duracionHoras, estado, fechaCreacion, utileria, null, null);
    }

    // Constructor completo para reconstruir el dominio desde persistencia.
    public Reserva(Long id, Long responsableId, Long canchaId, Disciplina disciplina, LocalDateTime inicio, int duracionHoras, EstadoReserva estado, LocalDateTime fechaCreacion, List<ReservaUtileria> utileria,
                   LocalDateTime creado, LocalDateTime modificado) {
        this.creado = creado;
        this.modificado = modificado;
        this.id = id;
        this.responsableId = responsableId;
        this.canchaId = canchaId;
        this.disciplina = disciplina;
        this.inicio = inicio;
        this.duracionHoras = duracionHoras;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        this.utileria = List.copyOf(utileria);
    }

    public static void validarHorario(LocalDateTime inicio, int duracion, LocalDateTime ahora) {
        if (inicio == null || inicio.getYear() > 9999 || inicio.isBefore(ahora)) {
            throw new DatoInvalidoException("La reserva debe comenzar en una fecha y hora actual o futura válida.");
        }
        if (duracion != 1 && duracion != 2) {
            throw new DatoInvalidoException("La duración debe ser de 1 o 2 horas.");
        }
        if (inicio.getMinute() != 0 || inicio.getSecond() != 0 || inicio.getNano() != 0) {
            throw new DatoInvalidoException("La reserva debe comenzar en una hora exacta.");
        }
    }

    public static Reserva nueva(Long responsableId, Long canchaId, Disciplina disciplina,
                                LocalDateTime inicio, int duracion, LocalDateTime ahora) {
        validarHorario(inicio, duracion, ahora);
        if (responsableId == null || responsableId <= 0 || canchaId == null || canchaId <= 0 || disciplina == null) {
            throw new DatoInvalidoException("Responsable, cancha y disciplina son obligatorios.");
        }
        return new Reserva(null, responsableId, canchaId, disciplina, inicio, duracion,
                EstadoReserva.CONFIRMADA, ahora.truncatedTo(java.time.temporal.ChronoUnit.MICROS), List.of());
    }

    public Reserva conUtileria(List<ReservaUtileria> detalles) {
        var ids = new java.util.HashSet<Long>();
        for (var detalle : detalles) {
            if (detalle == null) throw new DatoInvalidoException("El detalle de utilería no puede ser null.");
            ReservaUtileria.nuevo(detalle.getUtileriaId(), detalle.getCantidad());
            if (!ids.add(detalle.getUtileriaId())) throw new DatoInvalidoException("No se puede repetir un implemento en la reserva.");
        }
        return new Reserva(id, responsableId, canchaId, disciplina, inicio, duracionHoras,
                estado, fechaCreacion, detalles, creado, modificado);
    }

    public boolean seSuperpone(Reserva otra) {
        return inicio.isBefore(otra.getFin()) && getFin().isAfter(otra.inicio);
    }

    public Reserva cancelar(LocalDateTime ahora) {
        if (estado == EstadoReserva.CANCELADA) return this;
        if (!inicio.isAfter(ahora)) {
            throw new ReglaNegocioException("No se puede cancelar una reserva que ya comenzó.");
        }
        return new Reserva(id, responsableId, canchaId, disciplina, inicio, duracionHoras,
                EstadoReserva.CANCELADA, fechaCreacion, utileria, creado, modificado);
    }

    public Long getId() { return id; }
    public Long getResponsableId() { return responsableId; }
    public Long getCanchaId() { return canchaId; }
    public Disciplina getDisciplina() { return disciplina; }
    public LocalDateTime getInicio() { return inicio; }
    public LocalDate getFecha() { return inicio.toLocalDate(); }
    public LocalTime getHoraInicio() { return inicio.toLocalTime(); }
    public int getDuracionHoras() { return duracionHoras; }
    // Fin calculado: no se persiste un valor redundante que pueda contradecir la duración.
    public LocalDateTime getFin() { return inicio.plusHours(duracionHoras); }
    public LocalTime getHoraFin() { return getFin().toLocalTime(); }
    public EstadoReserva getEstado() { return estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public List<ReservaUtileria> getUtileria() { return utileria; }
    public LocalDateTime getCreado() { return creado; }
    public LocalDateTime getModificado() { return modificado; }
}

