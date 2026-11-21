package org.example.sistema_canchas_deportivas.infraestructure.persistence;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.example.sistema_canchas_deportivas.domain.models.SolicitudUtileria;
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
class UtileriaMysqlTest {
    @Autowired WebApplicationContext context;
    @Autowired UtileriaIn gestion;
    @Autowired ReservaIn servicioReservas;
    @Autowired DatosInicialesIn inicializador;
    @Autowired UtileriaOut utileria;
    @Autowired ReservaOut reservas;
    @Autowired UsuarioOut usuarios;
    @Autowired CanchaOut canchas;
    @Autowired JWTServices jwt;
    @Autowired ObjectMapper json;
    @Autowired EntityManager em;
    @Autowired Clock clock;
    MockMvc mvc;
    Usuario cliente, otro, admin;
    Utileria balon, chaleco;
    List<Cancha> compartidas;
    LocalDate fecha;

    @BeforeEach void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        cliente = usuario(); otro = usuario(); admin = usuarios.buscarPorUsername("admin").orElseThrow();
        balon = gestion.crear("Balón " + UUID.randomUUID(), TipoImplemento.BALON, 2, true, Set.of(Disciplina.VOLEY));
        chaleco = gestion.crear("Chalecos " + UUID.randomUUID(), TipoImplemento.CHALECO, 3, true, Set.of(Disciplina.VOLEY));
        compartidas = canchas.listar().stream().filter(c -> c.getTipo() == TipoCancha.VOLEY_ECUAVOLEY).toList();
        fecha = LocalDate.now(clock).with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(2);
    }

    @Test void variosImplementosPersistenConAuditoriaYCancelacionLiberaSinDescontarStock() throws Exception {
        String detalles = "[{\"utileriaId\":" + balon.getId() + ",\"cantidad\":1},{\"utileriaId\":" + chaleco.getId() + ",\"cantidad\":2}]";
        var respuesta = mvc.perform(post("/api/reservas").header("Authorization", bearer(cliente)).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(detalles))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.utileria[0].creado").exists()).andExpect(jsonPath("$.utileria.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(respuesta).get("id").asLong();
        var antes = reservas.buscarPorId(id).orElseThrow();
        assertEquals(1, disponibles(balon));
        assertEquals(1, disponibles(chaleco));
        assertEquals(2, utileria.buscarPorId(balon.getId()).orElseThrow().getExistencias());
        assertEquals(balon.getModificado(), utileria.buscarPorId(balon.getId()).orElseThrow().getModificado());
        mvc.perform(patch("/api/reservas/" + id + "/cancelar").header("Authorization", bearer(cliente)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.utileria.length()").value(2));
        em.clear();
        assertEquals(2, disponibles(balon));
        assertEquals(3, disponibles(chaleco));
        assertEquals(antes.getUtileria().getFirst().getCreado(), reservas.buscarPorId(id).orElseThrow().getUtileria().getFirst().getCreado());
    }

    @ParameterizedTest @ValueSource(strings = {"omitido", "null", "[]"})
    void utileriaEsOpcional(String detalle) throws Exception {
        mvc.perform(post("/api/reservas").header("Authorization", bearer(cliente)).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(detalle))).andExpect(status().isCreated()).andExpect(jsonPath("$.utileria").isEmpty());
    }

    @Test void cantidadesInvalidasDuplicadosENulosNoCreanReserva() throws Exception {
        for (String detalle : List.of("[null]", "[{\"utileriaId\":" + balon.getId() + ",\"cantidad\":0}]",
                "[{\"utileriaId\":" + balon.getId() + ",\"cantidad\":1.5}]",
                "[{\"utileriaId\":1.5,\"cantidad\":1}]",
                "[{\"utileriaId\":" + balon.getId() + "}]",
                "[{\"utileriaId\":" + balon.getId() + ",\"cantidad\":1},{\"utileriaId\":" + balon.getId() + ",\"cantidad\":1}]")) {
            mvc.perform(post("/api/reservas").header("Authorization", bearer(cliente)).contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo(detalle))).andExpect(status().isBadRequest());
        }
        assertTrue(reservas.listarPorResponsable(cliente.getId()).isEmpty());
    }

    @Test void implementoIncompatibleOInsuficienteRechazaTodaLaReserva() throws Exception {
        var raqueta = gestion.crear("Raqueta " + UUID.randomUUID(), TipoImplemento.RAQUETA, 2, true, Set.of(Disciplina.TENIS));
        mvc.perform(post("/api/reservas").header("Authorization", bearer(cliente)).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("[{\"utileriaId\":" + raqueta.getId() + ",\"cantidad\":1}]"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/reservas").header("Authorization", bearer(cliente)).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("[{\"utileriaId\":" + balon.getId() + ",\"cantidad\":1},{\"utileriaId\":" + chaleco.getId() + ",\"cantidad\":4}]")))
                .andExpect(status().isConflict());
        assertTrue(reservas.listarPorResponsable(cliente.getId()).isEmpty());
        assertEquals(2, disponibles(balon));
    }

    @Test void reservasConsecutivasReutilizanUnidadesEnUnaConsultaDeDosHoras() {
        reservar(cliente, 0, 10, 1, 1);
        reservar(cliente, 0, 11, 1, 1);
        assertEquals(1, disponibles(balon));
        reservar(otro, 1, 10, 2, 1);
        assertEquals(0, disponibles(balon));
        assertEquals(2, utileria.buscarPorId(balon.getId()).orElseThrow().getExistencias());
    }

    @Test void administracionProtegeCompromisosYPermiteReducirTrasCancelar() throws Exception {
        var reserva = reservar(cliente, 0, 10, 2, 2);
        mvc.perform(patch("/api/admin/utileria/" + balon.getId() + "/existencias").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"existencias\":1}")).andExpect(status().isConflict());
        mvc.perform(put("/api/admin/utileria/" + balon.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(datos(balon.getNombre(), 2, false, List.of("VOLEY"))))
                .andExpect(status().isConflict());
        servicioReservas.cancelar(cliente.getId(), reserva.getId());
        mvc.perform(patch("/api/admin/utileria/" + balon.getId() + "/existencias").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"existencias\":0}")).andExpect(status().isOk());
        assertEquals(0, disponibles(balon));
    }

    @Test void catalogoAdministrativoValidaPermisosCreacionEdicionYDuplicados() throws Exception {
        String nombre = "Nuevo " + UUID.randomUUID();
        mvc.perform(get("/api/utileria")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/utileria").header("Authorization", bearer(cliente))).andExpect(status().isOk());
        mvc.perform(post("/api/admin/utileria").header("Authorization", bearer(cliente)).contentType(MediaType.APPLICATION_JSON)
                        .content(datos(nombre, 0, true, List.of("VOLEY")))).andExpect(status().isForbidden());
        String respuesta = mvc.perform(post("/api/admin/utileria").header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(datos(nombre, 0, true, List.of("VOLEY")))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.existencias").value(0)).andReturn().getResponse().getContentAsString();
        long id = json.readTree(respuesta).get("id").asLong();
        mvc.perform(put("/api/admin/utileria/" + id).header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(datos(nombre + " editado", 1, true, List.of("VOLEY", "ECUAVOLEY")))).andExpect(status().isOk());
        mvc.perform(post("/api/admin/utileria").header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(datos(nombre + " editado", 0, true, List.of("VOLEY")))).andExpect(status().isConflict());
    }

    @Test void soloCambiarDisciplinasTambienActualizaLaFechaDeModificacion() {
        var editado = gestion.actualizar(balon.getId(), balon.getNombre(), balon.getTipo(), balon.getExistencias(),
                balon.isActiva(), Set.of(Disciplina.VOLEY, Disciplina.ECUAVOLEY));
        em.clear();
        var actual = utileria.buscarPorId(balon.getId()).orElseThrow();
        assertEquals(Set.of(Disciplina.VOLEY, Disciplina.ECUAVOLEY), actual.getDisciplinasCompatibles());
        assertEquals(balon.getCreado(), actual.getCreado());
        assertTrue(actual.getModificado().isAfter(balon.getModificado()), "Cambiar compatibilidades debe registrar modificación");
        assertEquals(actual.getModificado(), editado.getModificado());
    }

    @Test void reiniciarNoRecreaImplementosInicialesRenombrados() {
        var inicial = utileria.listar().stream().filter(u -> u.getNombre().equals("Balón Mikasa V200W")).findFirst().orElseThrow();
        int cantidad = utileria.listar().size();
        gestion.actualizar(inicial.getId(), "Renombrado " + UUID.randomUUID(), inicial.getTipo(), inicial.getExistencias(),
                inicial.isActiva(), inicial.getDisciplinasCompatibles());
        inicializador.inicializar();
        assertEquals(cantidad, utileria.listar().size());
    }

    @Test void disponibilidadHttpNoExponeReservasPrivadasYValidaIntervalo() throws Exception {
        reservar(cliente, 0, 10, 2, 1);
        String respuesta = mvc.perform(get("/api/utileria/disponibilidad").header("Authorization", bearer(otro))
                        .param("fecha", fecha.toString()).param("horaInicio", "10:00").param("duracion", "2").param("disciplina", "VOLEY"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.implementos").isArray())
                .andReturn().getResponse().getContentAsString();
        assertFalse(respuesta.contains(cliente.getUsername()));
        assertFalse(respuesta.contains("responsableId"));
        mvc.perform(get("/api/utileria/disponibilidad").header("Authorization", bearer(otro))
                        .param("fecha", fecha.toString()).param("horaInicio", "10:30").param("duracion", "2").param("disciplina", "VOLEY"))
                .andExpect(status().isBadRequest());
    }

    private Usuario usuario() {
        String nombre = "u6_" + UUID.randomUUID().toString().replace("-", "");
        usuarios.guardar(Usuario.nuevo(nombre, "hash-fixture", Rol.CLIENTE));
        return usuarios.buscarPorUsername(nombre).orElseThrow();
    }
    private String bearer(Usuario u) { return "Bearer " + jwt.generar(u); }
    private String cuerpo(String detalle) {
        return "{\"canchaId\":" + compartidas.getFirst().getId() + ",\"disciplina\":\"VOLEY\",\"fecha\":\"" + fecha
                + "\",\"horaInicio\":\"10:00\",\"duracion\":2" + (detalle.equals("omitido") ? "" : ",\"utileria\":" + detalle) + "}";
    }
    private String datos(String nombre, int stock, boolean activa, List<String> disciplinas) {
        return json.writeValueAsString(Map.of("nombre", nombre, "tipo", "BALON", "existencias", stock,
                "activa", activa, "disciplinasCompatibles", disciplinas));
    }
    private Reserva reservar(Usuario u, int cancha, int hora, int duracion, int cantidad) {
        return servicioReservas.crear(u.getId(), compartidas.get(cancha).getId(), Disciplina.VOLEY, fecha.atTime(hora, 0),
                duracion, List.of(new SolicitudUtileria(balon.getId(), cantidad)));
    }
    private int disponibles(Utileria item) {
        return gestion.disponibilidad(fecha.atTime(10, 0), 2, Disciplina.VOLEY).stream()
                .filter(d -> d.utileria().getId().equals(item.getId())).findFirst().orElseThrow().disponibles();
    }
}
