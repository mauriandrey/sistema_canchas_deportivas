package org.example.sistema_canchas_deportivas.infraestructure.entities;

import java.time.LocalDateTime;
import org.example.sistema_canchas_deportivas.infraestructure.auditoria.RegistroAuditable;
import org.example.sistema_canchas_deportivas.infraestructure.auditoria.AuditoriaListener;
import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@EntityListeners(AuditoriaListener.class)
@Table(name = "horarios_atencion",
        uniqueConstraints = @UniqueConstraint(name = "uk_horarios_dia", columnNames = "dia"),
        check = @CheckConstraint(name = "ck_horarios_intervalo", constraint = "apertura < cierre"))
public class HorarioAtencionEntity implements RegistroAuditable {
    // Fechas de auditoría administradas por los callbacks JPA.
    @Column(nullable = false, updatable = false)
    private LocalDateTime creado;

    @Column(nullable = false)
    private LocalDateTime modificado;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DayOfWeek dia;

    @Column(nullable = false)
    private LocalTime apertura;

    @Column(nullable = false)
    private LocalTime cierre;

    protected HorarioAtencionEntity() {
        // Requerido por JPA.
    }

    public HorarioAtencionEntity(Long id, DayOfWeek dia, LocalTime apertura, LocalTime cierre) {
        this.id = id;
        this.dia = dia;
        this.apertura = apertura;
        this.cierre = cierre;
    }

    public Long getId() { return id; }
    public DayOfWeek getDia() { return dia; }
    public LocalTime getApertura() { return apertura; }
    public LocalTime getCierre() { return cierre; }

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

