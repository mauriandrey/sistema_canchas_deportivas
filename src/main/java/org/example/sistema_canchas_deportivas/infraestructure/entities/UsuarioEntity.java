package org.example.sistema_canchas_deportivas.infraestructure.entities;

import java.time.LocalDateTime;
import org.example.sistema_canchas_deportivas.infraestructure.auditoria.RegistroAuditable;
import org.example.sistema_canchas_deportivas.infraestructure.auditoria.AuditoriaListener;
import jakarta.persistence.EntityListeners;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.example.sistema_canchas_deportivas.domain.models.Rol;

@Entity
@EntityListeners(AuditoriaListener.class)
@Table(name = "usuarios", uniqueConstraints = @UniqueConstraint(
        name = "uk_usuarios_username", columnNames = "username"))
public class UsuarioEntity implements RegistroAuditable {
    // Fechas de auditoría administradas por los callbacks JPA.
    @Column(nullable = false, updatable = false)
    private LocalDateTime creado;

    @Column(nullable = false)
    private LocalDateTime modificado;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo;

    @jakarta.persistence.OneToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "cliente_id", unique = true,
            foreignKey = @jakarta.persistence.ForeignKey(name = "fk_usuarios_cliente"))
    private ClienteEntity cliente;

    public ClienteEntity getCliente() { return cliente; }
    public void setCliente(ClienteEntity cliente) { this.cliente = cliente; }

    @OneToMany(mappedBy = "responsable")
    private List<ReservaEntity> reservas = new ArrayList<>();

    protected UsuarioEntity() {
        // Requerido por JPA.
    }

    public UsuarioEntity(Long id, String username, String passwordHash, Rol rol, boolean activo) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.activo = activo;
    }

    public List<ReservaEntity> getReservas() { return Collections.unmodifiableList(reservas); }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public Rol getRol() { return rol; }
    public boolean isActivo() { return activo; }
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

