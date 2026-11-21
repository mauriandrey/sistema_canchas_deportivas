package org.example.sistema_canchas_deportivas.infraestructure.persistence;

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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
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
@Transactional
class CanchasMysqlTest {
    @Autowired WebApplicationContext context;
    @Autowired CanchaIn gestion;
    @Autowired HorarioAtencionIn gestionHorarios;
    @Autowired DatosInicialesIn inicializador;
    @Autowired CanchaOut canchas;
    @Autowired HorarioAtencionOut horarios;
    @Autowired ReservaOut reservas;
    @Autowired UsuarioOut usuarios;
    @Autowired JWTServices jwt;
    @Autowired ObjectMapper json;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;
    private MockMvc mvc;
    private String bearer;
    private LocalDate lunes;

    @BeforeEach void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        bearer = "Bearer " + jwt.generar(usuarios.buscarPorUsername("admin").orElseThrow());
        lunes = LocalDate.now(clock).with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(2);
    }

    @Test void creaEditaDesactivaYReactivaMedianteHttpConJwtAdmin() throws Exception {
        String nombre = "Prueba " + UUID.randomUUID();
        String cuerpo = json.writeValueAsString(Map.of("nombre", nombre, "tipo", "TENIS", "activa", true));
        var respuesta = mvc.perform(post("/api/admin/canchas").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.disciplinasCompatibles[0]").value("TENIS"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(respuesta).get("id").asLong();
        mvc.perform(put("/api/admin/canchas/" + id).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo.replace(nombre, nombre + " editada")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value(nombre + " editada"));
        mvc.perform(patch("/api/admin/canchas/" + id + "/estado").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activa\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activa").value(false));
        em.clear();
        assertFalse(canchas.buscarPorId(id).orElseThrow().isActiva());
        mvc.perform(patch("/api/admin/canchas/" + id + "/estado").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activa\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activa").value(true));
        mvc.perform(get("/api/canchas/" + id)).andExpect(status().isOk());
    }

    @Test void nombreDuplicadoIncluyeMayusculasYEspaciosExternos() throws Exception {
        var existente = compartida();
        String cuerpo = json.writeValueAsString(Map.of("nombre", " " + existente.getNombre().toUpperCase(Locale.ROOT) + " ",
                "tipo", "VOLEY_ECUAVOLEY", "activa", true));
        mvc.perform(post("/api/admin/canchas").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict());
    }

    @Test void reservaConfirmadaBloqueaAmbasDisciplinasYCanceladaLibera() {
        Cancha cancha = compartida();
        Reserva guardada = reservar(cancha, lunes, 10, 2, EstadoReserva.CONFIRMADA);
        var ocupaciones = reservas.ocupacionesConfirmadas(lunes, lunes);
        assertTrue(ocupaciones.stream().anyMatch(o -> o.canchaId().equals(cancha.getId())));
        assertFalse(inicios(cancha, Disciplina.ECUAVOLEY).contains(LocalTime.of(10, 0)));
        assertFalse(inicios(cancha, Disciplina.VOLEY).contains(LocalTime.of(11, 0)));
        assertTrue(inicios(cancha, Disciplina.ECUAVOLEY).contains(LocalTime.of(12, 0)));
        reservas.guardar(new Reserva(guardada.getId(), guardada.getResponsableId(), cancha.getId(), Disciplina.VOLEY,
                guardada.getInicio(), 2, EstadoReserva.CANCELADA, guardada.getFechaCreacion(), List.of()));
        em.clear();
        assertTrue(inicios(cancha, Disciplina.ECUAVOLEY).contains(LocalTime.of(10, 0)));
        assertTrue(inicios(cancha, Disciplina.VOLEY).contains(LocalTime.of(11, 0)));
    }

    @Test void canchaInactivaNoApareceEnDisponibilidadYSeConservaEnCatalogo() {
        Cancha cancha = compartida();
        gestion.cambiarEstado(cancha.getId(), false);
        assertTrue(gestion.disponibilidad(lunes, Disciplina.VOLEY, 1).stream().noneMatch(d -> d.cancha().getId().equals(cancha.getId())));
        assertFalse(gestion.buscarPorId(cancha.getId()).isActiva());
    }

    @Test void cambioDeHorarioConReservaFuturaDevuelve409SinModificarLaFila() throws Exception {
        reservar(compartida(), lunes, 15, 1, EstadoReserva.CONFIRMADA);
        var antes = horarios.buscarPorDia(DayOfWeek.MONDAY).orElseThrow();
        mvc.perform(put("/api/admin/horarios/MONDAY").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"apertura\":\"08:00\",\"cierre\":\"15:00\"}"))
                .andExpect(status().isConflict());
        em.clear();
        assertEquals(antes.getCierre(), horarios.buscarPorDia(DayOfWeek.MONDAY).orElseThrow().getCierre());
    }

    @Test void horarioSePuedeCambiarCuandoLaReservaEstaCancelada() throws Exception {
        reservar(compartida(), lunes, 15, 1, EstadoReserva.CANCELADA);
        mvc.perform(put("/api/admin/horarios/MONDAY").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"apertura\":\"09:00\",\"cierre\":\"15:00\"}"))
                .andExpect(status().isOk());
        em.clear();
        assertEquals(LocalTime.of(15, 0), horarios.buscarPorDia(DayOfWeek.MONDAY).orElseThrow().getCierre());
        var bloques = gestion.disponibilidad(lunes, Disciplina.VOLEY, 2).getFirst().horarios();
        assertEquals(LocalTime.of(9, 0), bloques.getFirst().inicio());
        assertEquals(LocalTime.of(15, 0), bloques.getLast().fin());
    }

    @Test void reinicioNoRecreaCanchaRenombradaNiSobrescribeHorarios() {
        Cancha cancha = compartida();
        int cantidad = canchas.listar().size();
        String nombre = "Renombrada " + UUID.randomUUID();
        gestion.actualizar(cancha.getId(), nombre, cancha.getTipo(), null, true);
        gestionHorarios.actualizar(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(15, 0));
        inicializador.inicializar();
        inicializador.inicializar();
        em.clear();
        assertEquals(cantidad, canchas.listar().size());
        assertEquals(nombre, canchas.buscarPorId(cancha.getId()).orElseThrow().getNombre());
        assertEquals(LocalTime.of(9, 0), horarios.buscarPorDia(DayOfWeek.MONDAY).orElseThrow().getApertura());
    }

    @Test void desactivarConReservaVigenteDevuelve409YConservaHistorial() throws Exception {
        Cancha cancha = compartida();
        Reserva reserva = reservar(cancha, lunes, 10, 1, EstadoReserva.CONFIRMADA);
        mvc.perform(patch("/api/admin/canchas/" + cancha.getId() + "/estado").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activa\":false}"))
                .andExpect(status().isConflict());
        assertTrue(canchas.buscarPorId(cancha.getId()).orElseThrow().isActiva());
        assertTrue(reservas.buscarPorId(reserva.getId()).isPresent());
    }

    @Test void cambioDeTipoConHistorialCanceladoSeRechaza() throws Exception {
        Cancha cancha = compartida();
        reservar(cancha, lunes, 10, 1, EstadoReserva.CANCELADA);
        mvc.perform(put("/api/admin/canchas/" + cancha.getId()).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Conversión\",\"tipo\":\"TENIS\",\"activa\":true}"))
                .andExpect(status().isConflict());
    }

    @Test void disponibilidadHttpNoExponeResponsablesNiReservasPrivadas() throws Exception {
        reservar(compartida(), lunes, 10, 1, EstadoReserva.CONFIRMADA);
        String respuesta = mvc.perform(get("/api/canchas/disponibilidad")
                        .param("fecha", lunes.toString()).param("disciplina", "VOLEY").param("duracion", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.canchas").isArray())
                .andReturn().getResponse().getContentAsString();
        assertFalse(respuesta.contains("responsable"));
        assertFalse(respuesta.contains("password"));
        assertFalse(respuesta.contains("admin"));
    }

    private Cancha compartida() {
        return canchas.listar().stream().filter(c -> c.getTipo() == TipoCancha.VOLEY_ECUAVOLEY).findFirst().orElseThrow();
    }

    private List<LocalTime> inicios(Cancha cancha, Disciplina disciplina) {
        return gestion.disponibilidad(lunes, disciplina, 1).stream().filter(d -> d.cancha().getId().equals(cancha.getId()))
                .findFirst().orElseThrow().horarios().stream().map(b -> b.inicio()).toList();
    }

    private Reserva reservar(Cancha cancha, LocalDate fecha, int hora, int duracion, EstadoReserva estado) {
        return reservas.guardar(new Reserva(null, usuarios.buscarPorUsername("admin").orElseThrow().getId(),
                cancha.getId(), Disciplina.VOLEY, fecha.atTime(hora, 0), duracion, estado, LocalDateTime.now(clock), List.of()));
    }
}
