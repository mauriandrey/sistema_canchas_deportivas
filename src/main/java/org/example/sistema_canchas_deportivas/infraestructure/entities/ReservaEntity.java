package org.example.sistema_canchas_deportivas.infraestructure.entities;

import org.example.sistema_canchas_deportivas.infraestructure.auditoria.RegistroAuditable;
import org.example.sistema_canchas_deportivas.infraestructure.auditoria.AuditoriaListener;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.example.sistema_canchas_deportivas.domain.models.Disciplina;
import org.example.sistema_canchas_deportivas.domain.models.EstadoReserva;

@Entity
@EntityListeners(AuditoriaListener.class)
@Table(name = "reservas", indexes = {
        @Index(name = "idx_reservas_cancha_fecha_estado", columnList = "cancha_id,fecha,estado,hora_inicio"),
        @Index(name = "idx_reservas_responsable_fecha_estado", columnList = "responsable_id,fecha,estado,hora_inicio")
}, check = @CheckConstraint(name = "ck_reservas_duracion", constraint = "duracion_horas in (1, 2)"))
public class ReservaEntity implements RegistroAuditable {
    // Fechas de auditoría administradas por los callbacks JPA.
    @Column(nullable = false, updatable = false)
    private LocalDateTime creado;

    @Column(nullable = false)
    private LocalDateTime modificado;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsable_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reservas_usuario"))
    private UsuarioEntity responsable;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cancha_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reservas_cancha"))
    private CanchaEntity cancha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Disciplina disciplina;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "duracion_horas", nullable = false)
    private int duracionHoras;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservaUtileriaEntity> utileria = new ArrayList<>();

    protected ReservaEntity() {
        // Requerido por JPA.
    }

    public ReservaEntity(Long id, UsuarioEntity responsable, CanchaEntity cancha, Disciplina disciplina, LocalDate fecha, LocalTime horaInicio, int duracionHoras, EstadoReserva estado, LocalDateTime fechaCreacion) {
        this.id = id;
        this.responsable = responsable;
        this.cancha = cancha;
        this.disciplina = disciplina;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.duracionHoras = duracionHoras;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
    }

    public Long getId() { return id; }
    public UsuarioEntity getResponsable() { return responsable; }
    public CanchaEntity getCancha() { return cancha; }
    public Disciplina getDisciplina() { return disciplina; }
    public LocalDate getFecha() { return fecha; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public int getDuracionHoras() { return duracionHoras; }
    public EstadoReserva getEstado() { return estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public List<ReservaUtileriaEntity> getUtileria() { return Collections.unmodifiableList(utileria); }

    public void agregarUtileria(ReservaUtileriaEntity detalle) {
        detalle.vincularReserva(this);
        utileria.add(detalle);
    }

    @Transient
    public LocalDateTime getInicio() { return LocalDateTime.of(fecha, horaInicio); }

    @Transient
    public LocalDateTime getFin() { return getInicio().plusHours(duracionHoras); }

    @Transient
    public LocalTime getHoraFin() { return getFin().toLocalTime(); }
    @Override
    public LocalDateTime getCreado() { return creado; }

    @Override
    public LocalDateTime getModificado() { return modificado; }

    @Override
    public void alCrear(LocalDateTime ahora) {
        this.creado = ahora;
        this.modificado = ahora;
    }

    @Override
    public void alModificar(LocalDateTime ahora) {
        this.modificado = ahora;
    }

    @Override
    public void conservarAuditoriaDe(RegistroAuditable existente) {
        this.creado = existente.getCreado();
        this.modificado = existente.getModificado();
    }
}

