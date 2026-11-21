package org.example.sistema_canchas_deportivas.infraestructure.persistence;

import static org.example.sistema_canchas_deportivas.CedulasPrueba.nueva;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import org.example.sistema_canchas_deportivas.application.ports.out.UsuarioOut;
import org.example.sistema_canchas_deportivas.domain.models.Rol;
import org.example.sistema_canchas_deportivas.infraestructure.securities.JWTServices;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HTTP, BCrypt, JWT y MySQL reales; cada usuario de prueba se revierte mediante rollback. */
@SpringBootTest(properties = "spring.jpa.show-sql=false")
@ActiveProfiles({"local", "test"})
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_TESTS", matches = "true")
@Transactional
class AuthMysqlTest {
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder encoder;
    @Autowired UsuarioOut usuarios;
    @Autowired JWTServices jwt;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    private MockMvc mvc;
    private String username;
    private static final String PASSWORD = "ClaveSoloPruebas2026!";

    @BeforeEach void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        username = "fase3_" + UUID.randomUUID().toString().replace("-", "");
    }

    @Test void registroLoginYAutorizacionConPersistenciaReal() throws Exception {
        registrar();
        entityManager.clear();
        var usuario = usuarios.buscarPorUsername(username).orElseThrow();
        assertEquals(Rol.CLIENTE, usuario.getRol());
        assertTrue(encoder.matches(PASSWORD, usuario.getPasswordHash()));
        assertNotEquals(PASSWORD, usuario.getPasswordHash());
        String token = login(username, PASSWORD);
        assertEquals(usuario.getId(), jwt.validarYObtenerUsuarioId(token));
        mvc.perform(get("/api/admin/canchas").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test void adminExistentePuedeIniciarSesionYSuHashNoCambia() throws Exception {
        var antes = usuarios.buscarPorUsername("admin").orElseThrow();
        String passwordAdmin = System.getenv().getOrDefault("ADMIN_TEST_PASSWORD", "admin123");
        String token = login("admin", passwordAdmin);
        assertEquals(antes.getId(), jwt.validarYObtenerUsuarioId(token));
        // La fase 5 permite consultar las reservas con el JWT del administrador.
        mvc.perform(get("/api/admin/reservas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
        entityManager.clear();
        assertTrue(antes.getPasswordHash().equals(usuarios.buscarPorUsername("admin").orElseThrow().getPasswordHash()));
    }

    @Test void registroDuplicadoNoCambiaLaClaveNiRolDelAdministrador() throws Exception {
        var antes = usuarios.buscarPorUsername("admin").orElseThrow();
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(datosRegistro("ADMIN"))))
                .andExpect(status().isConflict());
        entityManager.clear();
        var despues = usuarios.buscarPorUsername("admin").orElseThrow();
        assertEquals(Rol.ADMIN, despues.getRol());
        assertTrue(antes.getPasswordHash().equals(despues.getPasswordHash()));
    }

    @Test void credencialesIncorrectasYUsuarioInexistenteDevuelvenElMismoError() throws Exception {
        registrar();
        for (String nombre : new String[]{username, username + "_no"}) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(java.util.Map.of("username", nombre, "password", "incorrecta"))))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
        }
    }

    @Test void desactivarUsuarioImpideLoginYUsoDeUnTokenYaEmitido() throws Exception {
        registrar();
        String token = login(username, PASSWORD);
        jdbc.update("update usuarios set activo=false where username=?", username);
        entityManager.clear();
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(java.util.Map.of("username", username, "password", PASSWORD))))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/reservas/mis-reservas").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private void registrar() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(datosRegistro(username))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    private String login(String nombre, String password) throws Exception {
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(java.util.Map.of("username", nombre, "password", password))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Sesión iniciada"))
                .andExpect(jsonPath("$.password").doesNotExist()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("result").get("token").asString();
    }
    private java.util.Map<String, String> datosRegistro(String nombre) {
        return java.util.Map.of("username", nombre, "password", PASSWORD,
                "cedula", nueva(),
                "nombres", "Persona", "apellidos", "Prueba", "email", nombre + "@example.test", "telefono", "0999999999");
    }
}

