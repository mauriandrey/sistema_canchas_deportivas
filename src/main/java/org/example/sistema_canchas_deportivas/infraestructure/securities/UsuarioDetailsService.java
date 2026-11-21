package org.example.sistema_canchas_deportivas.infraestructure.securities;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.example.sistema_canchas_deportivas.application.ports.out.UsuarioOut;
import org.example.sistema_canchas_deportivas.domain.models.Usuario;

@Service
public class UsuarioDetailsService implements UserDetailsService {
    private final UsuarioOut usuarioOut;

    public UsuarioDetailsService(UsuarioOut usuarioOut) {
        this.usuarioOut = usuarioOut;
    }

    @Override
    public UsuarioPrincipal loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioOut.buscarPorUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado."));
        UserDetails detalles = User.withUsername(usuario.getUsername())
                .password(usuario.getPasswordHash())
                .roles(usuario.getRol().name())
                .disabled(!usuario.isActivo())
                .build();
        // Conserva el ID además del UserDetails visto en clase, para comprobar la propiedad de reservas.
        return new UsuarioPrincipal(usuario, detalles);
    }
}

