package org.example.sistema_canchas_deportivas.domain.models;

import org.example.sistema_canchas_deportivas.domain.exception.DatoInvalidoException;
import org.example.sistema_canchas_deportivas.domain.exception.ReglaNegocioException;

import java.time.LocalDateTime;
import java.util.Set;

/** Existencias físicas reutilizables; las reservas ocupan unidades durante su intervalo. */
public final class Utileria {
    private final LocalDateTime creado;
    private final LocalDateTime modificado;
    private final Long id;
    private final String nombre;
    private final TipoImplemento tipo;
    private final int existencias;
    private final boolean activa;
    private final Set<Disciplina> disciplinasCompatibles;

    // Constructor compatible con las fases previas: las fechas aún no se conocen.
    public Utileria(Long id, String nombre, TipoImplemento tipo, int existencias, boolean activa, Set<Disciplina> disciplinasCompatibles) {
        this(id, nombre, tipo, existencias, activa, disciplinasCompatibles, null, null);
    }

    // Constructor completo para reconstruir el dominio desde persistencia.
    public Utileria(Long id, String nombre, TipoImplemento tipo, int existencias, boolean activa, Set<Disciplina> disciplinasCompatibles,
                   LocalDateTime creado, LocalDateTime modificado) {
        this.creado = creado;
        this.modificado = modificado;
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.existencias = existencias;
        this.activa = activa;
        this.disciplinasCompatibles = Set.copyOf(disciplinasCompatibles);
    }

    public static Utileria nuevo(String nombre, TipoImplemento tipo, int existencias,
                                 boolean activa, Set<Disciplina> disciplinas) {
        validar(nombre, tipo, existencias, disciplinas);
        return new Utileria(null, nombre.strip(), tipo, existencias, activa, disciplinas);
    }

    public Utileria actualizar(String nombre, TipoImplemento tipo, int existencias,
                              boolean activa, Set<Disciplina> disciplinas) {
        validar(nombre, tipo, existencias, disciplinas);
        return new Utileria(id, nombre.strip(), tipo, existencias, activa, disciplinas, creado, modificado);
    }

    private static void validar(String nombre, TipoImplemento tipo, int existencias, Set<Disciplina> disciplinas) {
        if (nombre == null || nombre.isBlank() || nombre.strip().length() > 100 || tipo == null
                || existencias < 0 || disciplinas == null || disciplinas.isEmpty()
                || disciplinas.stream().anyMatch(java.util.Objects::isNull)) {
            throw new DatoInvalidoException(
                    "Nombre de 1 a 100 caracteres, tipo, existencias no negativas y disciplinas son obligatorios.");
        }
    }

    public boolean admite(Disciplina disciplina) { return disciplinasCompatibles.contains(disciplina); }

    public void validarUso(Disciplina disciplina) {
        if (!activa) throw new ReglaNegocioException("La utilería está inactiva.");
        if (!admite(disciplina)) throw new DatoInvalidoException(
                "La utilería no es compatible con la disciplina.");
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public TipoImplemento getTipo() { return tipo; }
    public int getExistencias() { return existencias; }
    public boolean isActiva() { return activa; }
    public Set<Disciplina> getDisciplinasCompatibles() { return disciplinasCompatibles; }
    public LocalDateTime getCreado() { return creado; }
    public LocalDateTime getModificado() { return modificado; }
}

