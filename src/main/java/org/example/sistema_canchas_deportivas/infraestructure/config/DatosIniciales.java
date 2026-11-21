package org.example.sistema_canchas_deportivas.infraestructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.example.sistema_canchas_deportivas.application.ports.in.DatosInicialesIn;

@Configuration
public class DatosIniciales {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatosIniciales.class);

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.catalogos.inicializar", havingValue = "true")
    public CommandLineRunner inicializarCatalogos(DatosInicialesIn datosInicialesIn) {
        return args -> {
            datosInicialesIn.inicializar();
            LOGGER.info("Catálogos iniciales de canchas, horarios y utilería verificados.");
        };
    }
}
