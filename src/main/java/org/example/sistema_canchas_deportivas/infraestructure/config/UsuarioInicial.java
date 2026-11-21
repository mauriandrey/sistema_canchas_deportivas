package org.example.sistema_canchas_deportivas.infraestructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.example.sistema_canchas_deportivas.application.ports.in.UsuarioIn;
import org.example.sistema_canchas_deportivas.application.ports.out.UsuarioOut;

@Configuration
public class UsuarioInicial {
    private static final Logger LOGGER = LoggerFactory.getLogger(UsuarioInicial.class);
    private final UsuarioIn usuarioIn;
    private final UsuarioOut usuarioOut;
    private final Environment environment;

    public UsuarioInicial(UsuarioIn usuarioIn, UsuarioOut usuarioOut, Environment environment) {
        this.usuarioIn = usuarioIn;
        this.usuarioOut = usuarioOut;
        this.environment = environment;
    }

    @Bean
    public CommandLineRunner inicializarAdministrador() {
        return args -> {
            if (usuarioOut.existePorUsername("admin")) {
                LOGGER.info("El usuario admin ya existe; se conservan su contraseña y su rol.");
                return;
            }
            String password = environment.getProperty("app.admin.initial-password", "");
            boolean local = environment.matchesProfiles("local");
            if (password.isBlank() || (!local && password.length() < 12)) {
                throw new IllegalStateException(
                        "Configure ADMIN_INITIAL_PASSWORD; fuera del perfil local debe tener al menos 12 caracteres.");
            }
            if (usuarioIn.crearAdministradorInicial(password)) {
                LOGGER.info("Usuario administrador inicial creado con rol ADMIN.");
            } else {
                LOGGER.info("El usuario admin ya existe; no se modifica.");
            }
        };
    }
}

