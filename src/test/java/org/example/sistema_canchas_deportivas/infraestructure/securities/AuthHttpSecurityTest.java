package org.example.sistema_canchas_deportivas.infraestructure.securities;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import org.example.sistema_canchas_deportivas.application.ports.in.*;
import org.example.sistema_canchas_deportivas.application.ports.out.*;
import org.example.sistema_canchas_deportivas.domain.models.*;
import org.example.sistema_canchas_deportivas.domain.exception.CredencialesInvalidasException;
import org.example.sistema_canchas_deportivas.domain.exception.ReglaNegocioException;
import org.example.sistema_canchas_deportivas.infraestructure.config.*;
import org.example.sistema_canchas_deportivas.infraestructure.controllers.AuthController;
import org.example.sistema_canchas_deportivas.infraestructure.controllers.UsuarioController;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.example.sistema_canchas_deportivas.infraestructure.exceptions.GlobalExceptionHandler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AuthController.class, UsuarioController.class, AuthHttpSecurityTest.RutasPrueba.class})
@Import({SecurityConfig.class, WebConfig.class, JWTServices.class, UsuarioDetailsService.class,
        SecurityErrorHandler.class, AutorizacionReserva.class, GlobalExceptionHandler.class,
        AuthHttpSecurityTest.RutasPrueba.class})
@ActiveProfiles("test")
class AuthHttpSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired JWTServices jwt;
    @MockitoBean UsuarioIn usuariosIn;
    @MockitoBean AuthenticationManager auth;
    @MockitoBean UsuarioOut usuarios;
    @MockitoBean ReservaOut reservas;

    private final Usuario cliente = new Usuario(11L, "cliente", "hash", Rol.CLIENTE, true);
    private final Usuario otro = new Usuario(22L, "otro", "hash", Rol.CLIENTE, true);
    private final Usuario admin = new Usuario(1L, "admin", "hash", Rol.ADMIN, true);

    @BeforeEach void preparar() {
        when(usuarios.buscarPorUsername("cliente")).thenReturn(Optional.of(cliente));
        when(usuarios.buscarPorUsername("otro")).thenReturn(Optional.of(otro));
        when(usuarios.buscarPorUsername("admin")).thenReturn(Optional.of(admin));
        var inicio = LocalDateTime.of(2026, 12, 21, 10, 0);
        when(reservas.buscarPorId(50L)).thenReturn(Optional.of(new Reserva(50L, 11L, 2L,
                Disciplina.FUTBOL_11, inicio, 1, EstadoReserva.CONFIRMADA, inicio.minusDays(1), List.of())));
    }

    @Test void registroDevuelve201SinExponerPassword() throws Exception {
        when(usuariosIn.registrarCliente(eq("cliente"), eq("ClaveDePrueba2026"), any(Cliente.class))).thenReturn(cliente);
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"cliente\",\"password\":\"ClaveDePrueba2026\",\"cedula\":\"1750397802\",\"nombres\":\"Ana\",\"apellidos\":\"Prueba\",\"email\":\"ana@example.test\",\"telefono\":\"0999999999\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test void noPermiteInyectarRolAdmin() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"cliente\",\"password\":\"ClaveDePrueba2026\",\"rol\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("JSON_INVALIDO"));
        verifyNoInteractions(usuariosIn);
    }

    @Test void validaCamposSinDevolverValoresSecretos() throws Exception {
        var resultado = mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"corta\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles.password").exists()).andReturn();
        assertFalse(resultado.getResponse().getContentAsString().contains("corta"));
        verifyNoInteractions(usuariosIn);
    }

    @Test void usuarioDuplicadoDevuelve409() throws Exception {
        when(usuariosIn.registrarCliente(anyString(), anyString(), any(Cliente.class))).thenThrow(new ReglaNegocioException("El username ya está registrado."));
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"cliente\",\"password\":\"ClaveDePrueba2026\",\"cedula\":\"1750397802\",\"nombres\":\"Ana\",\"apellidos\":\"Prueba\",\"email\":\"ana@example.test\",\"telefono\":\"0999999999\"}"))
                .andExpect(status().isConflict());
    }

    @Test void conflictoConcurrenteDevuelve409SinExponerSql() throws Exception {
        when(usuariosIn.registrarCliente(anyString(), anyString(), any(Cliente.class))).thenThrow(new DataIntegrityViolationException("SQL privado"));
        var resultado = mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"cliente\",\"password\":\"ClaveDePrueba2026\",\"cedula\":\"1750397802\",\"nombres\":\"Ana\",\"apellidos\":\"Prueba\",\"email\":\"ana@example.test\",\"telefono\":\"0999999999\"}"))
                .andExpect(status().isConflict()).andReturn();
        assertFalse(resultado.getResponse().getContentAsString().contains("SQL privado"));
    }

    @Test void loginDevuelveTokenYNoCreaSesion() throws Exception {
        var principal = new UsuarioDetailsService(usuarios).loadUserByUsername("cliente");
        when(auth.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        var resultado = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"cliente\",\"password\":\"ClaveDePrueba2026\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.token").isNotEmpty())
                .andExpect(jsonPath("$.message").value("Sesión iniciada")).andExpect(jsonPath("$.result.rol").value("CLIENTE"))
                .andExpect(header().doesNotExist("Set-Cookie")).andReturn();
        assertNull(resultado.getRequest().getSession(false));
    }

    @Test void loginIncorrectoDevuelve401() throws Exception {
        when(auth.authenticate(any())).thenThrow(new BadCredentialsException("Credenciales inválidas"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"cliente\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test void loginNormalizaUsernameAntesDeAuthenticationManager() throws Exception {
        var principal = new UsuarioDetailsService(usuarios).loadUserByUsername("cliente");
        when(auth.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\" CLIENTE \",\"password\":\"ClaveDePrueba2026\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.result.username").value("cliente"))
            .andExpect(jsonPath("$.token").doesNotExist());
        verify(auth).authenticate(argThat(a -> a.getName().equals("cliente")
                && a.getCredentials().equals("ClaveDePrueba2026")));
    }

    @Test void loginRechazaMasDe72BytesSinLlamarAlAuthenticationManager() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"cliente\",\"password\":\"" + "á".repeat(37) + "\"}"))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(auth);
    }

    @Test void consultasDeCanchasSonPublicas() throws Exception {
        mvc.perform(get("/api/canchas")).andExpect(status().isOk());
        mvc.perform(get("/api/canchas/disponibilidad")).andExpect(status().isOk());
    }

    @Test void sinTokenDevuelve401YClienteSinPermiso403() throws Exception {
        mvc.perform(get("/api/admin/prueba")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO")).andExpect(header().string("WWW-Authenticate", "Bearer"));
        mvc.perform(get("/api/admin/prueba").header("Authorization", bearer(cliente))).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        mvc.perform(get("/api/admin/prueba").header("Authorization", bearer(admin))).andExpect(status().isOk());
    }

    @Test void clienteSoloConsultaYCancelaReservasPropias() throws Exception {
        mvc.perform(get("/api/reservas/50").header("Authorization", bearer(cliente))).andExpect(status().isOk());
        mvc.perform(patch("/api/reservas/50/cancelar").header("Authorization", bearer(cliente))).andExpect(status().isOk());
        mvc.perform(get("/api/reservas/50").header("Authorization", bearer(otro))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/reservas/50/cancelar").header("Authorization", bearer(otro))).andExpect(status().isForbidden());
        mvc.perform(get("/api/reservas/50").header("Authorization", bearer(admin))).andExpect(status().isOk());
        mvc.perform(patch("/api/reservas/50/cancelar").header("Authorization", bearer(admin))).andExpect(status().isForbidden());
    }

    @Test void noAceptaUnTokenDeOtraIdentidadConElMismoUsername() throws Exception {
        String token = bearer(cliente);
        when(usuarios.buscarPorUsername("cliente")).thenReturn(
                Optional.of(new Usuario(99L, "cliente", "hash", Rol.CLIENTE, true)));
        mvc.perform(get("/api/reservas/mis-reservas").header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test void tokenDeUsuarioInactivoOEliminadoEsRechazado() throws Exception {
        String token = bearer(cliente);
        when(usuarios.buscarPorUsername("cliente")).thenReturn(Optional.of(new Usuario(11L, "cliente", "hash", Rol.CLIENTE, false)));
        mvc.perform(get("/api/reservas/mis-reservas").header("Authorization", token)).andExpect(status().isUnauthorized());
        when(usuarios.buscarPorUsername("cliente")).thenReturn(Optional.empty());
        mvc.perform(get("/api/reservas/mis-reservas").header("Authorization", token)).andExpect(status().isUnauthorized());
    }

    @Test void aplicaElRolActualAunqueElTokenDigaAdmin() throws Exception {
        String token = bearer(admin);
        when(usuarios.buscarPorUsername("admin")).thenReturn(Optional.of(new Usuario(1L, "admin", "hash", Rol.CLIENTE, true)));
        mvc.perform(get("/api/admin/prueba").header("Authorization", token)).andExpect(status().isForbidden());
    }

    @Test void tokenMalformadoRechazadoInclusoEnRutaPublica() throws Exception {
        mvc.perform(get("/api/canchas").header("Authorization", "Bearer no.es.jwt")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/canchas").header("Authorization", "Bearer ")).andExpect(status().isUnauthorized());
    }

    @Test void corsPermiteReactYRechazaOrigenAjeno() throws Exception {
        mvc.perform(options("/api/auth/login").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options("/api/auth/login").header("Origin", "https://no-permitido.example")
                        .header("Access-Control-Request-Method", "POST")).andExpect(status().isForbidden());
    }

    @Test void filtroJwtSeEjecutaUnaVezYNoMantieneAutenticacionEntreSolicitudes() throws Exception {
        mvc.perform(get("/api/reservas/mis-reservas").header("Authorization", bearer(cliente)))
                .andExpect(status().isOk()).andExpect(header().doesNotExist("Set-Cookie"));
        verify(usuarios, times(1)).buscarPorUsername("cliente");
        mvc.perform(get("/api/reservas/mis-reservas")).andExpect(status().isUnauthorized());
    }

    private String bearer(Usuario usuario) { return "Bearer " + jwt.generar(usuario); }

    /** Rutas de prueba únicamente: no implementan los módulos de fases 4 y 5. */
    @RestController
    @org.springframework.boot.test.context.TestComponent
    public static class RutasPrueba {
        @GetMapping({"/api/canchas", "/api/canchas/disponibilidad", "/api/admin/prueba",
                "/api/reservas/{id}", "/api/reservas/mis-reservas"})
        public Map<String, Boolean> consultar() { return Map.of("permitido", true); }

        @PatchMapping("/api/reservas/{id}/cancelar")
        public Map<String, Boolean> cancelar() { return Map.of("permitido", true); }
    }
}

