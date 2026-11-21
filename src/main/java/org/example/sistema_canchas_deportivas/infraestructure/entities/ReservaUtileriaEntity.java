package org.example.sistema_canchas_deportivas.infraestructure.entities;

import java.time.LocalDateTime;
import org.example.sistema_canchas_deportivas.infraestructure.auditoria.RegistroAuditable;
import org.example.sistema_canchas_deportivas.infraestructure.auditoria.AuditoriaListener;
import jakarta.persistence.*;

@Entity
@EntityListeners(AuditoriaListener.class)
@Table(name = "reserva_utileria",
        uniqueConstraints = @UniqueConstraint(name = "uk_reserva_utileria", columnNames = {"reserva_id", "utileria_id"}),
        indexes = @Index(name = "idx_reserva_utileria_utileria", columnList = "utileria_id"),
        check = @CheckConstraint(name = "ck_detalle_cantidad", constraint = "cantidad > 0"))
public class ReservaUtileriaEntity implements RegistroAuditable {
    // Fechas de auditoría administradas por los callbacks JPA.
    @Column(nullable = false, updatable = false)
    private LocalDateTime creado;

    @Column(nullable = false)
    private LocalDateTime modificado;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false, foreignKey = @ForeignKey(name = "fk_detalle_reserva"))
    private ReservaEntity reserva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utileria_id", nullable = false, foreignKey = @ForeignKey(name = "fk_detalle_utileria"))
    private UtileriaEntity utileria;

    @Column(nullable = false)
    private int cantidad;

    protected ReservaUtileriaEntity() {
        // Requerido por JPA.
    }

    public ReservaUtileriaEntity(Long id, ReservaEntity reserva, UtileriaEntity utileria, int cantidad) {
        this.id = id;
        this.reserva = reserva;
        this.utileria = utileria;
        this.cantidad = cantidad;
    }

    public Long getId() { return id; }
    public ReservaEntity getReserva() { return reserva; }
    public UtileriaEntity getUtileria() { return utileria; }
    public int getCantidad() { return cantidad; }
    void vincularReserva(ReservaEntity reserva) {
        this.reserva = reserva;
    }
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

