package org.example.sistema_canchas_deportivas.infraestructure.persistence;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.*;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import org.example.sistema_canchas_deportivas.application.ports.in.*;
import org.example.sistema_canchas_deportivas.application.ports.out.*;
import org.example.sistema_canchas_deportivas.domain.models.*;
import org.example.sistema_canchas_deportivas.infraestructure.securities.JWTServices;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.jpa.show-sql=false")
@ActiveProfiles({"local", "test"})
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_TESTS", matches = "true")
@Transactional(isolation = Isolation.READ_COMMITTED)
class ReservasMysqlTest {
    @Autowired WebApplicationContext context;
    @Autowired UsuarioOut usuarios;
    @Autowired CanchaOut canchas;
    @Autowired ReservaOut reservas;
    @Autowired CanchaIn disponibilidad;
    @Autowired JWTServices jwt;
    @Autowired ObjectMapper json;
    @Autowired Clock clock;
    private MockMvc mvc;
    private Usuario cliente;
    private Usuario otro;
    private Cancha cancha;
    private LocalDate fecha;

    @BeforeEach void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        cliente = usuario();
        otro = usuario();
        cancha = canchas.listar().stream().filter(c -> c.getTipo() == TipoCancha.VOLEY_ECUAVOLEY).findFirst().orElseThrow();
        fecha = LocalDate.now(clock).with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(2);
    }

    @Test void creaConsultaYCancelaConJwtConservandoAuditoriaYRegistro() throws Exception {
        long id = crear(cliente, cancha.getId(), 10, 2);
        var antes = reservas.buscarPorId(id).orElseThrow();
        assertEquals(cliente.getId(), antes.getResponsableId());
        assertNotNull(antes.getCreado());
        assertEquals(antes.getCreado(), antes.getModificado());
        assertTrue(antes.getUtileria().isEmpty());
        mvc.perform(get("/api/reservas/" + id).header("Authorization", bearer(cliente)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.horaFin").value("12:00:00"))
                .andExpect(jsonPath("$.creado").exists()).andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(get("/api/reservas/mis-reservas").header("Authorization", bearer(cliente)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(patch("/api/reservas/" + id + "/cancelar").header("Authorization", bearer(cliente)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CANCELADA"));
        var cancelada = reservas.buscarPorId(id).orElseThrow();
        assertEquals(antes.getCreado(), cancelada.getCreado());
        assertEquals(antes.getFechaCreacion(), cancelada.getFechaCreacion());
        assertEquals(EstadoReserva.CANCELADA, cancelada.getEstado());
        mvc.perform(patch("/api/reservas/" + id + "/cancelar").header("Authorization", bearer(cliente)))
                .andExpect(status().isOk());
        crear(cliente, cancha.getId(), 10, 2); // La cancelación libera horario y límite diario.
    }

    @Test void ajenosNoConsultanNiCancelanYAdminSoloConsulta() throws Exception {
        long id = crear(cliente, cancha.getId(), 10, 1);
        for (var peticion : List.of(get("/api/reservas/" + id), patch("/api/reservas/" + id + "/cancelar"))) {
            mvc.perform(peticion.header("Authorization", bearer(otro))).andExpect(status().isForbidden());
        }
        var admin = usuarios.buscarPorUsername("admin").orElseThrow();
        mvc.perform(get("/api/reservas/" + id).header("Authorization", bearer(admin))).andExpect(status().isOk());
        mvc.perform(patch("/api/reservas/" + id + "/cancelar").header("Authorization", bearer(admin))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/reservas").header("Authorization", bearer(admin))).andExpect(status().isOk());
        mvc.perform(get("/api/admin/reservas").header("Authorization", bearer(cliente))).andExpect(status().isForbidden());
        mvc.perform(get("/api/reservas/mis-reservas").header("Authorization", bearer(otro)))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/reservas/9223372036854775807").header("Authorization", bearer(admin))).andExpect(status().isNotFound());
    }

    @Test void rechazaResponsableEstadoYAuditoriaPropuestosPorElCliente() throws Exception {
        for (String campo : List.of("\"responsableId\":1", "\"estado\":\"CANCELADA\"", "\"creado\":\"2000-01-01T00:00:00\"",
                "\"campoNoPermitido\":[]")) {
            String cuerpo = cuerpo(cancha.getId(), 10, 1);
            cuerpo = cuerpo.substring(0, cuerpo.length() - 1) + "," + campo + "}";
            mvc.perform(post("/api/reservas").header("Authorization", bearer(cliente))
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isBadRequest());
        }
        assertTrue(reservas.listarPorResponsable(cliente.getId()).isEmpty());
    }

    @Test void reservasExigenAutenticacionYCrearExigeCliente() throws Exception {
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(cuerpo(cancha.getId(), 10, 1)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/reservas/mis-reservas")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/reservas").header("Authorization", bearer(usuarios.buscarPorUsername("admin").orElseThrow()))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(cancha.getId(), 10, 1)))
                .andExpect(status().isForbidden());
    }

    @Test void permiteDosConsecutivasYRechazaLaTerceraSinGuardarParcialmente() throws Exception {
        crear(cliente, cancha.getId(), 10, 1);
        crear(cliente, cancha.getId(), 11, 1);
        mvc.perform(post("/api/reservas").header("Authorization", bearer(cliente))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(cancha.getId(), 12, 1)))
                .andExpect(status().isConflict());
        assertEquals(2, reservas.listarPorResponsable(cliente.getId()).size());
    }

    @Test void superposicionBloqueaLaCanchaFisicaEntreVoleyYEcuavoley() throws Exception {
        crear(cliente, cancha.getId(), 10, 2);
        mvc.perform(post("/api/reservas").header("Authorization", bearer(otro))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(cancha.getId(), 11, 1).replace("VOLEY", "ECUAVOLEY")))
                .andExpect(status().isConflict());
        assertTrue(reservas.listarPorResponsable(otro.getId()).isEmpty());
        var bloques = disponibilidad.disponibilidad(fecha, Disciplina.ECUAVOLEY, 1).stream()
                .filter(d -> d.cancha().getId().equals(cancha.getId())).findFirst().orElseThrow().horarios();
        assertTrue(bloques.stream().noneMatch(b -> b.inicio().equals(LocalTime.of(10, 0))));
        assertTrue(bloques.stream().anyMatch(b -> b.inicio().equals(LocalTime.of(12, 0))));
    }

    @Test void validaPasadoMinutosDuracionYCamposObligatorios() throws Exception {
        String correcto = cuerpo(cancha.getId(), 10, 1);
        for (String invalido : List.of(correcto.replace(fecha.toString(), LocalDate.now(clock).minusDays(1).toString()),
                correcto.replace("10:00", "10:30"), correcto.replace("\"duracion\":1", "\"duracion\":3"), "{}")) {
            mvc.perform(post("/api/reservas").header("Authorization", bearer(cliente))
                            .contentType(MediaType.APPLICATION_JSON).content(invalido)).andExpect(status().isBadRequest());
        }
        assertTrue(reservas.listarPorResponsable(cliente.getId()).isEmpty());
    }

    @Test void noCancelaReservasComenzadas() throws Exception {
        var pasada = reservas.guardar(new Reserva(null, cliente.getId(), cancha.getId(), Disciplina.VOLEY,
                LocalDate.now(clock).minusDays(1).atTime(10, 0), 1, EstadoReserva.CONFIRMADA,
                LocalDateTime.now(clock).minusDays(2), List.of()));
        mvc.perform(patch("/api/reservas/" + pasada.getId() + "/cancelar").header("Authorization", bearer(cliente)))
                .andExpect(status().isConflict());
        assertEquals(EstadoReserva.CONFIRMADA, reservas.buscarPorId(pasada.getId()).orElseThrow().getEstado());
    }

    private Usuario usuario() {
        String nombre = "res_" + UUID.randomUUID().toString().replace("-", "");
        usuarios.guardar(Usuario.nuevo(nombre, "hash-solo-fixture", Rol.CLIENTE));
        return usuarios.buscarPorUsername(nombre).orElseThrow();
    }
    private String bearer(Usuario usuario) { return "Bearer " + jwt.generar(usuario); }
    private String cuerpo(Long canchaId, int hora, int duracion) {
        return "{\"canchaId\":" + canchaId + ",\"disciplina\":\"VOLEY\",\"fecha\":\"" + fecha
                + "\",\"horaInicio\":\"" + String.format("%02d:00", hora) + "\",\"duracion\":" + duracion + "}";
    }
    private long crear(Usuario usuario, Long canchaId, int hora, int duracion) throws Exception {
        var respuesta = mvc.perform(post("/api/reservas").header("Authorization", bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(canchaId, hora, duracion)))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA")).andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta).get("id").asLong();
    }
}
