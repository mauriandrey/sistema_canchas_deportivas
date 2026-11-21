package org.example.sistema_canchas_deportivas.infraestructure.controllers;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.TransactionTimedOutException;
import org.example.sistema_canchas_deportivas.application.ports.in.*;
import org.example.sistema_canchas_deportivas.application.ports.out.*;
import org.example.sistema_canchas_deportivas.domain.exception.*;
import org.example.sistema_canchas_deportivas.infraestructure.config.*;
import org.example.sistema_canchas_deportivas.infraestructure.exceptions.GlobalExceptionHandler;
import org.example.sistema_canchas_deportivas.infraestructure.mappers.*;
import org.example.sistema_canchas_deportivas.infraestructure.securities.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AuthController.class, UsuarioController.class, CanchaController.class, HorarioAtencionController.class,
        ReservaController.class, UtileriaController.class})
@Import({SecurityConfig.class, WebConfig.class, JWTServices.class, UsuarioDetailsService.class,
        SecurityErrorHandler.class, AutorizacionReserva.class, GlobalExceptionHandler.class,
        CanchaMapper.class, HorarioAtencionMapper.class, ReservaMapper.class, ReservaUtileriaMapper.class, UtileriaMapper.class})
@ActiveProfiles("test")
class ValidacionGlobalHttpTest {
    @Autowired MockMvc mvc;
    @MockitoBean org.springframework.security.authentication.AuthenticationManager auth;
    @MockitoBean UsuarioIn usuariosIn;
    @MockitoBean CanchaIn canchas;
    @MockitoBean HorarioAtencionIn horarios;
    @MockitoBean ReservaIn reservas;
    @MockitoBean UtileriaIn utileria;
    @MockitoBean UsuarioOut usuarios;
    @MockitoBean ReservaOut reservasOut;

    static Stream<Arguments> solicitudesInvalidas() {
        return Stream.of(
            Arguments.of("POST", "/api/auth/registro", "{}", "username", "El username es obligatorio."),
            Arguments.of("POST", "/api/auth/login", "{}", "password", "La contraseña es obligatoria."),
            Arguments.of("POST", "/api/admin/canchas", "{}", "tipo", "El tipo de cancha es obligatorio."),
            Arguments.of("PATCH", "/api/admin/canchas/1/estado", "{}", "activa", "Indica si la cancha está activa."),
            Arguments.of("PUT", "/api/admin/horarios/MONDAY", "{}", "apertura", "La hora de apertura es obligatoria."),
            Arguments.of("POST", "/api/admin/utileria", "{}", "disciplinasCompatibles", "Indica al menos una disciplina compatible."),
            Arguments.of("PATCH", "/api/admin/utileria/1/existencias", "{\"existencias\":-1}", "existencias", "Las existencias no pueden ser negativas."),
            Arguments.of("POST", "/api/reservas", "{}", "fecha", "La fecha es obligatoria."),
            Arguments.of("POST", "/api/reservas",
                "{\"canchaId\":1,\"disciplina\":\"TENIS\",\"fecha\":\"2030-01-07\",\"horaInicio\":\"10:00\",\"duracion\":1,\"utileria\":[{\"utileriaId\":1,\"cantidad\":0}]}",
                "utileria[0].cantidad", "La cantidad debe ser positiva."));
    }

    @ParameterizedTest @MethodSource("solicitudesInvalidas")
    void validaTodosLosDtosEnEspanolAntesDelCasoDeUso(String metodo, String ruta, String json,
                                                     String campo, String mensaje) throws Exception {
        String rol = ruta.equals("/api/reservas") ? "CLIENTE" : "ADMIN";
        mvc.perform(request(org.springframework.http.HttpMethod.valueOf(metodo), ruta)
                .with(user("prueba").roles(rol)).contentType(MediaType.APPLICATION_JSON).content(json)
                .header("Accept-Language", "en"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("VALIDACION"))
            .andExpect(jsonPath("$.detalles['" + campo + "']").value(mensaje));
        verifyNoInteractions(auth, usuariosIn, canchas, horarios, reservas, utileria);
    }

    @Test void rechazaJsonDesconocidoDecimalesEnumsYFechasInvalidas() throws Exception {
        for (String json : List.of(
                "{\"existencias\":1.5}", "{\"existencias\":2147483648}",
                "{\"existencias\":1,\"creado\":\"2030-01-01\"}", "{")) {
            mvc.perform(patch("/api/admin/utileria/1/existencias").with(user("admin").roles("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("JSON_INVALIDO"));
        }
        for (String json : List.of(
                "{\"canchaId\":1.5}", "{\"canchaId\":1,\"disciplina\":\"AJEDREZ\"}",
                "{\"disciplina\":0}", "{\"fecha\":\"2030-02-30\"}", "{\"horaInicio\":\"25:00\"}")) {
            mvc.perform(post("/api/reservas").with(user("cliente").roles("CLIENTE"))
                    .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("JSON_INVALIDO"));
        }
        verifyNoInteractions(reservas, utileria);
    }

    @Test void erroresDelProtocoloConservanEstadoYCabecerasEnJson() throws Exception {
        mvc.perform(patch("/api/admin/canchas").with(user("admin").roles("ADMIN")))
            .andExpect(status().isMethodNotAllowed()).andExpect(header().exists("Allow"))
            .andExpect(jsonPath("$.codigo").value("METODO_NO_PERMITIDO"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.TEXT_PLAIN).content("texto"))
            .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.codigo").value("CONTENIDO_NO_SOPORTADO"));
        when(canchas.listar()).thenReturn(List.of());
        mvc.perform(get("/api/canchas").accept(MediaType.APPLICATION_XML))
            .andExpect(status().isNotAcceptable()).andExpect(jsonPath("$.codigo").value("RESPUESTA_NO_ACEPTABLE"));
        mvc.perform(get("/api/canchas/ruta/inexistente"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test void erroresDeDominioConservanEstadoYMensaje() throws Exception {
        when(canchas.buscarPorId(0L)).thenThrow(new DatoInvalidoException("El ID debe ser positivo."));
        when(canchas.buscarPorId(9L)).thenThrow(new RecursoNoEncontradoException("Cancha no encontrada."));
        when(canchas.buscarPorId(8L)).thenThrow(new ReglaNegocioException("La cancha está en uso."));
        mvc.perform(get("/api/canchas/0")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));
        mvc.perform(get("/api/canchas/9")).andExpect(status().isNotFound()).andExpect(jsonPath("$.mensaje").value("Cancha no encontrada."));
        mvc.perform(get("/api/canchas/8")).andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CONFLICTO"));
    }

    @Test void conflictosYFallosNoExponenDatosInternos() throws Exception {
        for (RuntimeException error : List.of(
                new DataIntegrityViolationException("SQL contraseña privada"),
                new TransactionTimedOutException("SQL contraseña privada"),
                new IllegalStateException("SQL contraseña privada"),
                new AccessDeniedException("SQL contraseña privada"))) {
            reset(canchas);
            when(canchas.listar()).thenThrow(error);
            int esperado = error instanceof IllegalStateException ? 500 : error instanceof AccessDeniedException ? 403 : 409;
            var respuesta = mvc.perform(get("/api/canchas")).andExpect(status().is(esperado))
                .andExpect(jsonPath("$.fecha").exists()).andExpect(jsonPath("$.detalles").isMap()).andReturn();
            assertFalse(respuesta.getResponse().getContentAsString().contains("privada"));
            assertFalse(respuesta.getResponse().getContentAsString().contains("SQL"));
        }
    }

    @Test void seguridadMantieneContratoJsonYSinSesion() throws Exception {
        mvc.perform(get("/api/admin/reservas")).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO")).andExpect(jsonPath("$.detalles").isMap())
            .andExpect(header().string("WWW-Authenticate", "Bearer")).andExpect(header().doesNotExist("Set-Cookie"));
        mvc.perform(get("/api/admin/reservas").with(user("cliente").roles("CLIENTE")))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        verifyNoInteractions(reservas);
    }

    @Test void corsPermiteLeerLocationYNoUsaCredencialesDeCookies() throws Exception {
        mvc.perform(options("/api/reservas").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
            .andExpect(header().string("Access-Control-Expose-Headers", "Location, WWW-Authenticate"))
            .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }
}
