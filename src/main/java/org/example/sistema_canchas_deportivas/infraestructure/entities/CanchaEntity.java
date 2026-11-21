package org.example.sistema_canchas_deportivas.infraestructure.entities;

import java.time.LocalDateTime;
import org.example.sistema_canchas_deportivas.infraestructure.auditoria.RegistroAuditable;
import org.example.sistema_canchas_deportivas.infraestructure.auditoria.AuditoriaListener;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.example.sistema_canchas_deportivas.domain.models.TipoCancha;
import org.example.sistema_canchas_deportivas.domain.models.Superficie;

@Entity
@EntityListeners(AuditoriaListener.class)
@Table(name = "canchas", uniqueConstraints = @UniqueConstraint(name = "uk_canchas_nombre", columnNames = "nombre"))
public class CanchaEntity implements RegistroAuditable {
    // Fechas de auditoría administradas por los callbacks JPA.
    @Column(nullable = false, updatable = false)
    private LocalDateTime creado;

    @Column(nullable = false)
    private LocalDateTime modificado;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoCancha tipo;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Superficie superficie;

    @Column(nullable = false)
    private boolean activa;

    @OneToMany(mappedBy = "cancha")
    private List<ReservaEntity> reservas = new ArrayList<>();

    protected CanchaEntity() {
        // Requerido por JPA.
    }

    public CanchaEntity(Long id, String nombre, TipoCancha tipo, Superficie superficie, boolean activa) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.superficie = superficie;
        this.activa = activa;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public TipoCancha getTipo() { return tipo; }
    public Superficie getSuperficie() { return superficie; }
    public boolean isActiva() { return activa; }
    public List<ReservaEntity> getReservas() { return Collections.unmodifiableList(reservas); }
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

