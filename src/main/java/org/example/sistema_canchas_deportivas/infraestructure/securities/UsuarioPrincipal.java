package org.example.sistema_canchas_deportivas.infraestructure.securities;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.example.sistema_canchas_deportivas.domain.models.Usuario;

/** Añade la identidad de dominio al UserDetails construido con User.withUsername. */
public final class UsuarioPrincipal implements UserDetails {
    private final Usuario usuario;
    private final UserDetails detalles;

    public UsuarioPrincipal(Usuario usuario, UserDetails detalles) {
        this.usuario = usuario;
        this.detalles = detalles;
    }

    public Long getId() { return usuario.getId(); }
    public Usuario getUsuario() { return usuario; }
    @Override public String getUsername() { return detalles.getUsername(); }
    @Override public String getPassword() { return detalles.getPassword(); }
    @Override public boolean isEnabled() { return detalles.isEnabled(); }
    @Override public boolean isAccountNonExpired() { return detalles.isAccountNonExpired(); }
    @Override public boolean isAccountNonLocked() { return detalles.isAccountNonLocked(); }
    @Override public boolean isCredentialsNonExpired() { return detalles.isCredentialsNonExpired(); }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return detalles.getAuthorities();
    }
}
