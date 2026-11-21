package org.example.sistema_canchas_deportivas.infraestructure.controllers;

import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.example.sistema_canchas_deportivas.domain.exception.CredencialesInvalidasException;
import org.example.sistema_canchas_deportivas.infraestructure.securities.JWTServices;
import org.example.sistema_canchas_deportivas.infraestructure.securities.UsuarioPrincipal;
import org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JWTServices jwtServices;

    public AuthController(AuthenticationManager authenticationManager, JWTServices jwtServices) {
        this.authenticationManager = authenticationManager;
        this.jwtServices = jwtServices;
    }

    @PostMapping("/login")
    public ResponseEntity<ActionResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new CredencialesInvalidasException();
        }
        var autenticacion = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username().strip().toLowerCase(Locale.ROOT), request.password()));
        var principal = (UsuarioPrincipal) autenticacion.getPrincipal();
        String rol = principal.getUsuario().getRol().name();
        String token = jwtServices.generar(autenticacion.getName(), rol, principal.getId());
        return ResponseEntity.ok(new ActionResponse<>("Sesión iniciada",
                new LoginResponse(autenticacion.getName(), rol, token)));
    }
}

