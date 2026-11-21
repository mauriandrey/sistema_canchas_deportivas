package org.example.sistema_canchas_deportivas.infraestructure.securities;

import java.util.function.Supplier;
import org.springframework.security.core.Authentication;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.example.sistema_canchas_deportivas.application.ports.out.ReservaOut;
import org.example.sistema_canchas_deportivas.domain.models.Rol;

/** Comprobación de propietario en HTTP; los casos de uso vuelven a verificar los permisos. */
@Component
public class AutorizacionReserva {
    private final ReservaOut reservaOut;

    public AutorizacionReserva(ReservaOut reservaOut) {
        this.reservaOut = reservaOut;
    }

    public AuthorizationDecision consultar(Supplier<? extends Authentication> supplier, RequestAuthorizationContext context) {
        return decidir(supplier.get(), context, true);
    }

    public AuthorizationDecision cancelar(Supplier<? extends Authentication> supplier, RequestAuthorizationContext context) {
        return decidir(supplier.get(), context, false);
    }

    private AuthorizationDecision decidir(Authentication authentication, RequestAuthorizationContext context,
                                           boolean permitirAdmin) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UsuarioPrincipal principal)) {
            return new AuthorizationDecision(false);
        }
        if (permitirAdmin && principal.getUsuario().getRol() == Rol.ADMIN) {
            return new AuthorizationDecision(true);
        }
        Long id;
        try {
            id = Long.valueOf(context.getVariables().get("id"));
        } catch (RuntimeException exception) {
            return new AuthorizationDecision(false);
        }
        boolean propietario = reservaOut.buscarPorId(id)
                .map(reserva -> principal.getId().equals(reserva.getResponsableId())).orElse(false);
        return new AuthorizationDecision(propietario);
    }
}
