package org.example.sistema_canchas_deportivas.infraestructure.securities;

import java.io.IOException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/** Se instancia únicamente dentro de SecurityFilterChain; no es un filtro de servlet independiente. */
public class JWTFilter extends OncePerRequestFilter {
    private final JWTServices jwtServices;
    private final UsuarioDetailsService usuarios;
    private final SecurityErrorHandler errors;

    public JWTFilter(JWTServices jwtServices, UsuarioDetailsService usuarios, SecurityErrorHandler errors) {
        this.jwtServices = jwtServices;
        this.usuarios = usuarios;
        this.errors = errors;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null) {
            try {
                if (!header.regionMatches(true, 0, "Bearer ", 0, 7) || header.length() > 8192) {
                    throw new BadCredentialsException("Token inválido.");
                }
                String token = header.substring(7);
                Claims payload = jwtServices.leer(token);
                UsuarioPrincipal principal = usuarios.loadUserByUsername(payload.getSubject());
                if (!principal.getId().equals(payload.get("usuarioId", Long.class))) {
                    throw new BadCredentialsException("El token no corresponde al usuario actual.");
                }
                if (!principal.isEnabled()) throw new DisabledException("Usuario inactivo.");
                // Los permisos se obtienen de la base, no de un rol desactualizado dentro del token.
                var authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            } catch (JwtException | IllegalArgumentException | AuthenticationException exception) {
                SecurityContextHolder.clearContext();
                errors.commence(request, response, new BadCredentialsException("Token inválido."));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}

