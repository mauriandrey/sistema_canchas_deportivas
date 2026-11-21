package org.example.sistema_canchas_deportivas.infraestructure.persistence;

import jakarta.persistence.EntityManager;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.example.sistema_canchas_deportivas.application.ports.in.DatosInicialesIn;
import org.example.sistema_canchas_deportivas.application.ports.out.*;
import org.example.sistema_canchas_deportivas.domain.models.*;
import org.example.sistema_canchas_deportivas.infraestructure.config.UsuarioInicial;
import org.example.sistema_canchas_deportivas.infraestructure.entities.*;
import org.example.sistema_canchas_deportivas.infraestructure.repositories.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integración contra MySQL real, habilitada explícitamente con RUN_MYSQL_TESTS=true.
 * El arranque agrega el catálogo que falte; las escrituras de cada prueba se revierten.
 * Nunca se usa create/create-drop, DELETE ni TRUNCATE.
 */
@SpringBootTest(properties = "spring.jpa.show-sql=false")
@ActiveProfiles({"local", "test"})
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_TESTS", matches = "true")
@Transactional
class PersistenciaMysqlTest {
    @Autowired private DatosInicialesIn inicializador;
    @Autowired private UsuarioInicial usuarioInicial;
    @Autowired private CanchaOut canchas;
    @Autowired private HorarioAtencionOut horarios;
    @Autowired private UtileriaOut utileria;
    @Autowired private UsuarioOut usuarios;
    @Autowired private ReservaOut reservas;
    @Autowired private CanchaRepository canchaRepository;
    @Autowired private HorarioAtencionRepository horarioRepository;
    @Autowired private UtileriaRepository utileriaRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void catalogoYAdministradorSeConservanAlInicializarDosVeces() throws Exception {
        String hash = usuarios.buscarPorUsername("admin").orElseThrow().getPasswordHash();
        assertEquals(Rol.ADMIN, usuarios.buscarPorUsername("admin").orElseThrow().getRol());
        assertTrue(hash.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}"));
        var canchasAntes = jdbc.queryForList("select id, nombre from canchas order by id");
        var horariosAntes = jdbc.queryForList("select id, dia, apertura, cierre from horarios_atencion order by id");
        var utileriaAntes = jdbc.queryForList("select id, nombre, existencias from utileria order by id");

        inicializador.inicializar();
        usuarioInicial.inicializarAdministrador().run();
        inicializador.inicializar();
        usuarioInicial.inicializarAdministrador().run();
        entityManager.clear();

        assertEquals(canchasAntes, jdbc.queryForList("select id, nombre from canchas order by id"));
        assertEquals(horariosAntes, jdbc.queryForList("select id, dia, apertura, cierre from horarios_atencion order by id"));
        assertEquals(utileriaAntes, jdbc.queryForList("select id, nombre, existencias from utileria order by id"));
        assertTrue(hash.equals(usuarios.buscarPorUsername("admin").orElseThrow().getPasswordHash()),
                "La inicialización no debe cambiar el hash del administrador");
        assertEquals(1, jdbc.queryForObject("select count(*) from usuarios where username='admin'", Integer.class));
        assertEquals(17, canchas.listar().size());
        assertEquals(7, horarios.listar().size());
        assertEquals(8, utileria.listar().size());
        assertEquals(9, jdbc.queryForObject("select count(*) from utileria_disciplinas", Integer.class));
        assertEquals("sistema_canchas_deportivas", jdbc.queryForObject("select database()", String.class));
    }

    @Test
    void reinicializarRespetaExistenciasEstadoYHorariosModificados() {
        Long canchaId = cancha().getId();
        Long implementoId = implemento().getId();
        jdbc.update("update canchas set activa=false where id=?", canchaId);
        jdbc.update("update utileria set existencias=12 where id=?", implementoId);
        jdbc.update("update horarios_atencion set cierre='15:00:00' where dia='MONDAY'");
        inicializador.inicializar();
        entityManager.clear();
        assertFalse(canchas.buscarPorId(canchaId).orElseThrow().isActiva());
        assertEquals(12, utileria.buscarPorId(implementoId).orElseThrow().getExistencias());
        assertEquals(LocalTime.of(15, 0), horarios.listar().stream()
                .filter(h -> h.getDia() == DayOfWeek.MONDAY).findFirst().orElseThrow().getCierre());
    }

    @Test
    void persisteYRecuperaReservaConDetalleYRelacionesEnAmbasDirecciones() {
        Long implementoId = implemento().getId();
        Reserva guardada = reservas.guardar(reserva(List.of(new ReservaUtileria(null, null, implementoId, 2))));
        entityManager.clear();
        Reserva recuperada = reservas.buscarPorId(guardada.getId()).orElseThrow();
        assertEquals(LocalDateTime.of(2026, 12, 21, 10, 0), recuperada.getInicio());
        assertEquals(LocalDateTime.of(2026, 12, 21, 12, 0), recuperada.getFin());
        assertEquals(LocalDateTime.of(2026, 11, 15, 9, 0), recuperada.getFechaCreacion());
        assertEquals(EstadoReserva.CONFIRMADA, recuperada.getEstado());
        assertEquals(guardada.getId(), recuperada.getUtileria().getFirst().getReservaId());
        assertEquals(implementoId, recuperada.getUtileria().getFirst().getUtileriaId());
        assertEquals(2, recuperada.getUtileria().getFirst().getCantidad());
        assertNotNull(recuperada.getUtileria().getFirst().getId());

        assertTrue(entityManager.find(UsuarioEntity.class, recuperada.getResponsableId()).getReservas()
                .stream().anyMatch(r -> r.getId().equals(recuperada.getId())));
        assertTrue(entityManager.find(CanchaEntity.class, recuperada.getCanchaId()).getReservas()
                .stream().anyMatch(r -> r.getId().equals(recuperada.getId())));
        assertTrue(entityManager.find(UtileriaEntity.class, implementoId).getReservas()
                .stream().anyMatch(d -> d.getReserva().getId().equals(recuperada.getId())));
        assertEquals(0, utileria.buscarPorId(implementoId).orElseThrow().getExistencias());
    }

    @Test
    void persisteReservaSinImplementos() {
        Reserva guardada = reservas.guardar(reserva(List.of()));
        entityManager.clear();
        assertTrue(reservas.buscarPorId(guardada.getId()).orElseThrow().getUtileria().isEmpty());
    }

    @Test
    void mysqlRechazaNombreDeCanchaDuplicado() {
        Cancha existente = cancha();
        assertThrows(DataIntegrityViolationException.class, () -> canchaRepository.saveAndFlush(
                new CanchaEntity(null, existente.getNombre(), existente.getTipo(), existente.getSuperficie(), true)));
    }

    @Test
    void mysqlRechazaDiaDeAtencionDuplicado() {
        assertThrows(DataIntegrityViolationException.class, () -> horarioRepository.saveAndFlush(
                new HorarioAtencionEntity(null, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0))));
    }

    @Test
    void mysqlRechazaImplementoDuplicadoEnLaMismaReserva() {
        Long implementoId = implemento().getId();
        var detalles = List.of(new ReservaUtileria(null, null, implementoId, 1),
                new ReservaUtileria(null, null, implementoId, 2));
        assertThrows(DataIntegrityViolationException.class, () -> reservas.guardar(reserva(detalles)));
    }

    @Test
    void mysqlRechazaStockNegativo() {
        assertThrows(DataIntegrityViolationException.class, () -> utileriaRepository.saveAndFlush(
                new UtileriaEntity(null, "Prueba transaccional stock negativo", TipoImplemento.BALON, -1, true)));
    }

    @Test
    void mysqlRechazaCantidadNoPositiva() {
        Long implementoId = implemento().getId();
        assertThrows(DataIntegrityViolationException.class, () -> reservas.guardar(
                reserva(List.of(new ReservaUtileria(null, null, implementoId, 0)))));
    }

    @Test
    void mysqlRechazaResponsableInexistente() {
        Reserva valida = reserva(List.of());
        Reserva sinResponsable = new Reserva(null, Long.MAX_VALUE, valida.getCanchaId(), valida.getDisciplina(),
                valida.getInicio(), 2, valida.getEstado(), valida.getFechaCreacion(), List.of());
        assertThrows(DataIntegrityViolationException.class, () -> reservas.guardar(sinResponsable));
    }

    private Cancha cancha() {
        return canchas.listar().stream().filter(c -> c.getTipo() == TipoCancha.FUTBOL_11).findFirst().orElseThrow();
    }

    private Utileria implemento() {
        return utileria.listar().stream()
                .filter(u -> u.getTipo() == TipoImplemento.BALON
                        && u.getDisciplinasCompatibles().equals(Set.of(Disciplina.FUTBOL_11)))
                .findFirst().orElseThrow();
    }

    private Reserva reserva(List<ReservaUtileria> detalles) {
        return new Reserva(null, usuarios.buscarPorUsername("admin").orElseThrow().getId(),
                cancha().getId(), Disciplina.FUTBOL_11, LocalDateTime.of(2026, 12, 21, 10, 0),
                2, EstadoReserva.CONFIRMADA, LocalDateTime.of(2026, 11, 15, 9, 0), detalles);
    }
}

