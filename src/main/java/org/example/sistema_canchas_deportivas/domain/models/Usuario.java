package org.example.sistema_canchas_deportivas.domain.models;

import java.time.LocalDateTime;
/** Modelo de usuario. passwordHash contiene exclusivamente el hash; nunca la clave original. */
public final class Usuario {
    private final LocalDateTime creado;
    private final LocalDateTime modificado;
    private final Long id;
    private final Long clienteId;
    private final String username;
    private final String passwordHash;
    private final Rol rol;
    private final boolean activo;

    // Constructor compatible con las fases previas: las fechas aún no se conocen.
    public Usuario(Long id, String username, String passwordHash, Rol rol, boolean activo) {
        this(id, username, passwordHash, rol, activo, null, null);
    }

    // Constructor completo para reconstruir el dominio desde persistencia.
    public Usuario(Long id, String username, String passwordHash, Rol rol, boolean activo,
                   LocalDateTime creado, LocalDateTime modificado) {
        this(id, username, passwordHash, rol, activo, creado, modificado, null);
    }

    public Usuario(Long id, String username, String passwordHash, Rol rol, boolean activo,
                   LocalDateTime creado, LocalDateTime modificado, Long clienteId) {
        this.clienteId = clienteId;
        this.creado = creado;
        this.modificado = modificado;
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.activo = activo;
    }

    public static Usuario nuevo(String username, String passwordHash, Rol rol) {
        return new Usuario(null, username, passwordHash, rol, true, null, null);
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Rol getRol() {
        return rol;
    }

    public boolean isActivo() {
        return activo;
    }
    public LocalDateTime getCreado() { return creado; }
    public LocalDateTime getModificado() { return modificado; }
    public Long getClienteId() { return clienteId; }
}

