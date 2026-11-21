package org.example.sistema_canchas_deportivas.domain.models;

import java.time.LocalDateTime;
import java.util.Set;
import org.example.sistema_canchas_deportivas.domain.exception.DatoInvalidoException;

public final class Cancha {
    private final LocalDateTime creado;
    private final LocalDateTime modificado;
    private final Long id;
    private final String nombre;
    private final TipoCancha tipo;
    private final Superficie superficie;
    private final boolean activa;

    // Constructor compatible con las fases previas: las fechas aún no se conocen.
    public Cancha(Long id, String nombre, TipoCancha tipo, Superficie superficie, boolean activa) {
        this(id, nombre, tipo, superficie, activa, null, null);
    }

    // Constructor completo para reconstruir el dominio desde persistencia.
    public Cancha(Long id, String nombre, TipoCancha tipo, Superficie superficie, boolean activa,
                   LocalDateTime creado, LocalDateTime modificado) {
        this.creado = creado;
        this.modificado = modificado;
        if (nombre == null || nombre.isBlank() || nombre.strip().length() > 100 || tipo == null) {
            throw new DatoInvalidoException("Nombre de 1 a 100 caracteres y tipo de cancha obligatorios.");
        }
        if ((tipo == TipoCancha.FUTBOL_11 && superficie != Superficie.NATURAL)
                || (tipo == TipoCancha.FUTBOL_6 && superficie != Superficie.SINTETICA)) {
            throw new DatoInvalidoException("Fútbol 11 requiere superficie NATURAL y fútbol 6 SINTETICA.");
        }
        this.id = id;
        this.nombre = nombre.strip();
        this.tipo = tipo;
        this.superficie = superficie;
        this.activa = activa;
    }

    public static Cancha nuevo(String nombre, TipoCancha tipo, Superficie superficie, boolean activa) {
        return new Cancha(null, nombre, tipo, superficie, activa, null, null);
    }

    // La versión actualizada conserva identidad y auditoría; JPA asignará modificado al guardar.
    public Cancha actualizar(String nombre, TipoCancha tipo, Superficie superficie, boolean activa) {
        return new Cancha(id, nombre, tipo, superficie, activa, creado, modificado);
    }

    public Set<Disciplina> getDisciplinasCompatibles() {
        return switch (tipo) {
            case FUTBOL_11 -> Set.of(Disciplina.FUTBOL_11);
            case FUTBOL_6 -> Set.of(Disciplina.FUTBOL_6);
            case BASQUET -> Set.of(Disciplina.BASQUET);
            case VOLEY_ECUAVOLEY -> Set.of(Disciplina.VOLEY, Disciplina.ECUAVOLEY);
            case TENIS -> Set.of(Disciplina.TENIS);
        };
    }

    public boolean admite(Disciplina disciplina) { return getDisciplinasCompatibles().contains(disciplina); }
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public TipoCancha getTipo() { return tipo; }
    public Superficie getSuperficie() { return superficie; }
    public boolean isActiva() { return activa; }
    public LocalDateTime getCreado() { return creado; }
    public LocalDateTime getModificado() { return modificado; }
}

