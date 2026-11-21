package org.example.sistema_canchas_deportivas.infraestructure.persistence;

import static org.example.sistema_canchas_deportivas.CedulasPrueba.nueva;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.*;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import org.example.sistema_canchas_deportivas.application.ports.in.*;
import org.example.sistema_canchas_deportivas.application.ports.out.*;
import org.example.sistema_canchas_deportivas.domain.models.*;
import org.example.sistema_canchas_deportivas.infraestructure.securities.JWTServices;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Cada caso revierte sus datos mediante rollback; no elimina registros del usuario. */
@SpringBootTest(properties = "spring.jpa.show-sql=false")
@ActiveProfiles({"local", "test"})
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_TESTS", matches = "true")
@Transactional(isolation = Isolation.READ_COMMITTED)
class CrudAdminMysqlTest {
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper json;
    @Autowired JWTServices jwt;
    @Autowired UsuarioOut usuarios;
    @Autowired ClienteOut clientes;
    @Autowired CanchaOut canchas;
    @Autowired UtileriaOut utileria;
    @Autowired ReservaOut reservas;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;
    private MockMvc mvc;
    private String adminToken, nombre, cedula;
    private long usuarioId, clienteId, canchaId, implementoId;
    private LocalDate fecha;

    @BeforeEach void preparar() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        var admin = usuarios.buscarPorUsername("admin").orElseThrow();
        adminToken = "Bearer " + jwt.generar(admin.getUsername(), "ADMIN", admin.getId());
        cedula = nueva();
        nombre = "crud_" + UUID.randomUUID().toString().replace("-", "");
        var cuenta = enviar(post("/api/auth/registro"), perfil(nombre), 201, false);
        usuarioId = cuenta.get("id").asLong(); clienteId = cuenta.get("clienteId").asLong();
        canchaId = canchas.guardar(Cancha.nuevo(nombre, TipoCancha.VOLEY_ECUAVOLEY, null, true)).getId();
        implementoId = utileria.guardar(Utileria.nuevo(nombre, TipoImplemento.BALON, 2, true, Set.of(Disciplina.VOLEY))).getId();
        fecha = LocalDate.now(clock).with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(20);
        em.clear();
    }

    Map<String, Object> perfil(String username) {
        return Map.of("username", username, "password", "ClaveSoloPruebas2026!", "cedula", cedula,
                "nombres", "Persona", "apellidos", "Prueba", "email", username + "@example.test", "telefono", "0999999999");
    }
    Map<String, Object> reserva(int hora, List<?> detalles) {
        return Map.of("responsableId", usuarioId, "canchaId", canchaId, "disciplina", "VOLEY", "fecha", fecha.toString(),
                "horaInicio", String.format("%02d:00:00", hora), "duracion", 1, "estado", "CONFIRMADA", "utileria", detalles);
    }
    JsonNode enviar(MockHttpServletRequestBuilder req, Object cuerpo, int status, boolean admin) throws Exception {
        if (admin) req.header("Authorization", adminToken);
        if (cuerpo != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo));
        var respuesta = mvc.perform(req).andExpect(status().is(status)).andReturn().getResponse().getContentAsString();
        return respuesta.isEmpty() ? null : json.readTree(respuesta);
    }
    long crearReserva() throws Exception {
        return enviar(post("/api/admin/reservas"), reserva(10, List.of(Map.of("utileriaId", implementoId, "cantidad", 1))), 201, true).get("id").asLong();
    }

    @Test void registroVinculaPerfilYLoginConservaRolClienteSinExponerClave() throws Exception {
        var cuenta = usuarios.buscarPorId(usuarioId).orElseThrow();
        assertEquals(Rol.CLIENTE, cuenta.getRol()); assertEquals(clienteId, cuenta.getClienteId());
        var perfil = clientes.buscarPorId(clienteId).orElseThrow();
        assertEquals(nombre + "@example.test", perfil.getEmail()); assertNotNull(perfil.getCreado());
        var login = enviar(post("/api/auth/login"), Map.of("username", nombre, "password", "ClaveSoloPruebas2026!"), 200, false);
        assertEquals("CLIENTE", login.get("result").get("rol").asString());
        var leida = enviar(get("/api/admin/usuarios/" + usuarioId), null, 200, true);
        assertFalse(leida.has("password")); assertFalse(leida.has("passwordHash"));
        assertEquals(clienteId, leida.get("clienteId").asLong());
    }

    @Test void crudUsuarioYClienteRespetaVinculoYAuditoria() throws Exception {
        enviar(delete("/api/admin/clientes/" + clienteId), null, 409, true);
        var antes = usuarios.buscarPorId(usuarioId).orElseThrow().getCreado();
        enviar(put("/api/admin/usuarios/" + usuarioId), Map.of("username", nombre, "activo", false, "clienteId", clienteId), 200, true);
        em.clear();
        assertEquals(antes, usuarios.buscarPorId(usuarioId).orElseThrow().getCreado());
        enviar(post("/api/auth/login"), Map.of("username", nombre, "password", "ClaveSoloPruebas2026!"), 401, false);
        enviar(put("/api/admin/clientes/" + clienteId), Map.of("cedula", cedula, "nombres", "Nombre corregido",
                "apellidos", "Prueba", "email", nombre + "@example.test", "telefono", "0988888888"), 200, true);
        enviar(get("/api/admin/usuarios"), null, 200, true);
        enviar(delete("/api/admin/usuarios/" + usuarioId), null, 204, true);
        assertTrue(clientes.buscarPorId(clienteId).isPresent());
        enviar(delete("/api/admin/clientes/" + clienteId), null, 200, true);
        enviar(get("/api/admin/usuarios/" + usuarioId), null, 404, true);
        enviar(get("/api/admin/clientes/" + clienteId), null, 404, true);
    }

    @Test void crudCanchaYUtileriaConRegistrosNuevos() throws Exception {
        var cancha = enviar(post("/api/admin/canchas"), Map.of("nombre", nombre + "_nueva", "tipo", "TENIS", "activa", true), 201, true);
        long id = cancha.get("id").asLong();
        enviar(put("/api/admin/canchas/" + id), Map.of("nombre", nombre + "_editada", "tipo", "TENIS", "activa", true), 200, true);
        enviar(get("/api/admin/canchas/" + id), null, 200, true);
        enviar(delete("/api/admin/canchas/" + id), null, 204, true);
        enviar(get("/api/admin/canchas/" + id), null, 404, true);
        var item = enviar(post("/api/admin/utileria"), Map.of("nombre", nombre + "_nuevo", "tipo", "BALON", "existencias", 3,
                "activa", true, "disciplinasCompatibles", List.of("VOLEY")), 201, true);
        id = item.get("id").asLong();
        enviar(put("/api/admin/utileria/" + id), Map.of("nombre", nombre + "_editado", "tipo", "BALON", "existencias", 4,
                "activa", true, "disciplinasCompatibles", List.of("VOLEY")), 200, true);
        enviar(get("/api/admin/utileria/" + id), null, 200, true);
        enviar(delete("/api/admin/utileria/" + id), null, 204, true);
        enviar(get("/api/admin/utileria/" + id), null, 404, true);
    }

    @Test void crudReservaConservaAuditoriaYEliminaSoloSusDetalles() throws Exception {
        long id = crearReserva();
        var antes = reservas.buscarPorId(id).orElseThrow(); em.clear();
        enviar(put("/api/admin/reservas/" + id), reserva(11, List.of(Map.of("utileriaId", implementoId, "cantidad", 2))), 200, true);
        em.clear();
        var despues = reservas.buscarPorId(id).orElseThrow();
        assertEquals(antes.getCreado(), despues.getCreado());
        assertEquals(antes.getFechaCreacion(), despues.getFechaCreacion());
        assertEquals(antes.getUtileria().getFirst().getId(), despues.getUtileria().getFirst().getId());
        assertEquals(antes.getUtileria().getFirst().getCreado(), despues.getUtileria().getFirst().getCreado());
        assertEquals(2, despues.getUtileria().getFirst().getCantidad());
        enviar(get("/api/admin/reservas/" + id), null, 200, true);
        enviar(delete("/api/admin/reservas/" + id), null, 204, true);
        em.clear();
        assertTrue(reservas.buscarPorId(id).isEmpty());
        assertEquals(0, jdbc.queryForObject("select count(*) from reserva_utileria where reserva_id=?", Integer.class, id));
        assertTrue(usuarios.buscarPorId(usuarioId).isPresent()); assertTrue(canchas.buscarPorId(canchaId).isPresent());
        assertTrue(utileria.buscarPorId(implementoId).isPresent());
    }

    @Test void crudDetalleYReemplazoCompletoNoDejanHuerfanos() throws Exception {
        long id = crearReserva();
        var creado = reservas.buscarPorId(id).orElseThrow().getCreado(); em.clear();
        enviar(get("/api/admin/reservas/" + id + "/utileria"), null, 200, true);
        enviar(put("/api/admin/reservas/" + id + "/utileria/" + implementoId), Map.of("cantidad", 2), 200, true);
        enviar(get("/api/admin/reservas/" + id + "/utileria/" + implementoId), null, 200, true);
        enviar(delete("/api/admin/reservas/" + id + "/utileria/" + implementoId), null, 204, true);
        em.clear();
        assertEquals(0, jdbc.queryForObject("select count(*) from reserva_utileria where reserva_id=?", Integer.class, id));
        enviar(post("/api/admin/reservas/" + id + "/utileria"), Map.of("utileriaId", implementoId, "cantidad", 1), 201, true);
        enviar(put("/api/admin/reservas/" + id), reserva(11, List.of()), 200, true);
        em.clear();
        assertTrue(reservas.buscarPorId(id).orElseThrow().getUtileria().isEmpty());
        assertEquals(creado, reservas.buscarPorId(id).orElseThrow().getCreado());
    }

    @Test void borradoDePadresConReservasDevuelveConflicto() throws Exception {
        crearReserva();
        enviar(delete("/api/admin/canchas/" + canchaId), null, 409, true);
        enviar(delete("/api/admin/utileria/" + implementoId), null, 409, true);
        enviar(delete("/api/admin/usuarios/" + usuarioId), null, 409, true);
        assertTrue(canchas.buscarPorId(canchaId).isPresent());
    }

    @Test void cancelarComoAdminConservaReservaYLiberaIntervalo() throws Exception {
        long id = crearReserva();
        enviar(patch("/api/admin/reservas/" + id + "/cancelar"), null, 200, true);
        em.clear();
        assertEquals(EstadoReserva.CANCELADA, reservas.buscarPorId(id).orElseThrow().getEstado());
        assertEquals(1, reservas.buscarPorId(id).orElseThrow().getUtileria().size());
        crearReserva();
    }

    @Test void registroDuplicadoNoCreaOtraCuentaNiOtroPerfil() throws Exception {
        int antesCuentas = jdbc.queryForObject("select count(*) from usuarios", Integer.class);
        int antesClientes = jdbc.queryForObject("select count(*) from cliente", Integer.class);
        var datos = new HashMap<>(perfil(nombre + "_otro")); datos.put("email", nombre + "@example.test"); datos.put("cedula", nueva());
        enviar(post("/api/auth/registro"), datos, 409, false);
        assertEquals(antesCuentas, jdbc.queryForObject("select count(*) from usuarios", Integer.class));
        assertEquals(antesClientes, jdbc.queryForObject("select count(*) from cliente", Integer.class));
    }

    @Test void cuentaAnteriorPuedeVincularseSoloUnaVezPorAdmin() throws Exception {
        usuarios.guardar(Usuario.nuevo(nombre + "_legacy", "hash-fixture", Rol.CLIENTE));
        var antigua = usuarios.buscarPorUsername(nombre + "_legacy").orElseThrow();
        enviar(put("/api/admin/usuarios/" + antigua.getId()), Map.of("username", antigua.getUsername(), "activo", true, "clienteId", clienteId), 409, true);
        var nuevoPerfil = clientes.guardar(Cliente.nuevo(nueva(), "Perfil", "Anterior", nombre + "_legacy@example.test", "0999999999"));
        enviar(put("/api/admin/usuarios/" + antigua.getId()), Map.of("username", antigua.getUsername(), "activo", true, "clienteId", nuevoPerfil.getId()), 200, true);
        em.clear();
        assertEquals(nuevoPerfil.getId(), usuarios.buscarPorId(antigua.getId()).orElseThrow().getClienteId());
    }
}
