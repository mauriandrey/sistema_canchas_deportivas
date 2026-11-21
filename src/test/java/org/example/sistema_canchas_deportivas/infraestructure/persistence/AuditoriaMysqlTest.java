package org.example.sistema_canchas_deportivas.infraestructure.persistence;

import java.time.*;
import java.util.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.example.sistema_canchas_deportivas.application.ports.in.DatosInicialesIn;
import org.example.sistema_canchas_deportivas.application.ports.out.*;
import org.example.sistema_canchas_deportivas.domain.models.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.jpa.show-sql=false")
@ActiveProfiles({"local", "test"})
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_TESTS", matches = "true")
@Import(AuditoriaMysqlTest.ConfiguracionReloj.class)
@Transactional
class AuditoriaMysqlTest {
    @Autowired UsuarioOut usuarios;
    @Autowired CanchaOut canchas;
    @Autowired HorarioAtencionOut horarios;
    @Autowired UtileriaOut utileria;
    @Autowired ReservaOut reservas;
    @Autowired DatosInicialesIn inicializador;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired RelojPrueba reloj;

    @BeforeEach void preparar() {
        reloj.instante = Instant.parse("2030-01-07T15:00:00.123456Z");
    }

    @Test void insercionesYActualizacionesAuditanUsuarioCanchaUtileriaReservaYDetalle() {
        LocalDateTime inicio = LocalDateTime.now(reloj);
        String nombre = "audit_" + UUID.randomUUID().toString().replace("-", "");
        usuarios.guardar(Usuario.nuevo(nombre, "hash-de-prueba", Rol.CLIENTE));
        var usuario = usuarios.buscarPorUsername(nombre).orElseThrow();
        var cancha = canchas.guardar(Cancha.nuevo(nombre, TipoCancha.TENIS, null, true));
        var implemento = utileria.guardar(new Utileria(null, nombre, TipoImplemento.RAQUETA, 3, true, Set.of(Disciplina.TENIS)));
        var reserva = reservas.guardar(new Reserva(null, usuario.getId(), cancha.getId(), Disciplina.TENIS,
                inicio.plusDays(1).withMinute(0).withSecond(0).withNano(0), 1, EstadoReserva.CONFIRMADA,
                inicio.minusMinutes(1), List.of(new ReservaUtileria(null, null, implemento.getId(), 1))));
        assertEquals(inicio, usuario.getCreado());
        assertEquals(inicio, cancha.getCreado());
        assertEquals(inicio, implemento.getCreado());
        assertEquals(inicio, reserva.getCreado());
        assertEquals(inicio, reserva.getUtileria().getFirst().getCreado());
        assertEquals(inicio, reserva.getModificado());
        assertEquals(inicio.minusMinutes(1), reserva.getFechaCreacion()); // Fecha de negocio, distinta de inserción.

        reloj.instante = reloj.instante.plusSeconds(300);
        // Los constructores antiguos omiten fechas: el adaptador debe conservarlas desde MySQL.
        usuarios.guardar(new Usuario(usuario.getId(), nombre, usuario.getPasswordHash(), Rol.CLIENTE, false));
        var editada = canchas.guardar(new Cancha(cancha.getId(), nombre + "_editada", TipoCancha.TENIS, null, false));
        var implementoEditado = utileria.guardar(new Utileria(implemento.getId(), nombre, TipoImplemento.RAQUETA, 5, true, Set.of(Disciplina.TENIS)));
        var detalle = reserva.getUtileria().getFirst();
        var cancelada = reservas.guardar(new Reserva(reserva.getId(), usuario.getId(), cancha.getId(), Disciplina.TENIS,
                reserva.getInicio(), 1, EstadoReserva.CANCELADA, reserva.getFechaCreacion(),
                List.of(new ReservaUtileria(detalle.getId(), reserva.getId(), implemento.getId(), 2))));
        em.clear();
        var usuarioActual = usuarios.buscarPorId(usuario.getId()).orElseThrow();
        var canchaActual = canchas.buscarPorId(cancha.getId()).orElseThrow();
        var utileriaActual = utileria.buscarPorId(implemento.getId()).orElseThrow();
        var reservaActual = reservas.buscarPorId(reserva.getId()).orElseThrow();
        assertEquals(inicio, usuarioActual.getCreado());
        assertEquals(inicio.plusMinutes(5), usuarioActual.getModificado());
        assertEquals(inicio, editada.getCreado());
        assertEquals(inicio.plusMinutes(5), canchaActual.getModificado());
        assertEquals(inicio, utileriaActual.getCreado());
        assertEquals(inicio.plusMinutes(5), implementoEditado.getModificado());
        assertEquals(inicio, cancelada.getCreado());
        assertEquals(inicio.plusMinutes(5), reservaActual.getModificado());
        assertEquals(inicio, reservaActual.getUtileria().getFirst().getCreado());
        assertEquals(inicio.plusMinutes(5), reservaActual.getUtileria().getFirst().getModificado());
    }

    @Test void horarioConservaCreacionYRegistraSoloLaEdicion() {
        var horario = horarios.buscarPorDia(DayOfWeek.MONDAY).orElseThrow();
        LocalDateTime creado = horario.getCreado();
        var editado = horarios.guardar(new HorarioAtencion(horario.getId(), horario.getDia(), LocalTime.of(9, 0), LocalTime.of(15, 0)));
        em.clear();
        var recargado = horarios.buscarPorId(horario.getId()).orElseThrow();
        assertEquals(creado, editado.getCreado());
        assertEquals(creado, recargado.getCreado());
        assertEquals(LocalDateTime.now(reloj), recargado.getModificado());
    }

    @Test void guardarSinCambiosYReinicializarNoModificanFechas() {
        var cancha = canchas.listar().getFirst();
        LocalDateTime creado = cancha.getCreado();
        LocalDateTime modificado = cancha.getModificado();
        var guardada = canchas.guardar(cancha);
        inicializador.inicializar();
        em.clear();
        assertEquals(creado, guardada.getCreado());
        var recargada = canchas.buscarPorId(cancha.getId()).orElseThrow();
        assertEquals(creado, recargada.getCreado());
        assertEquals(modificado, recargada.getModificado());
    }

    @Test void fechasInventadasEnElModeloNoSobrescribenLaAuditoria() {
        var cancha = canchas.listar().getFirst();
        var fechaInventada = LocalDateTime.of(2000, 1, 1, 0, 0);
        var guardada = canchas.guardar(new Cancha(cancha.getId(), cancha.getNombre() + " editada",
                cancha.getTipo(), cancha.getSuperficie(), cancha.isActiva(), fechaInventada, fechaInventada));
        em.clear();
        assertEquals(cancha.getCreado(), guardada.getCreado());
        assertEquals(LocalDateTime.now(reloj), guardada.getModificado());
        assertEquals(cancha.getCreado(), canchas.buscarPorId(cancha.getId()).orElseThrow().getCreado());
    }

    @Test void tablasPersistenFechasNoNulasConPrecisionDeMicrosegundos() {
        for (String tabla : List.of("usuarios", "canchas", "horarios_atencion", "utileria", "reservas", "reserva_utileria")) {
            assertEquals(2, jdbc.queryForObject("""
                    select count(*) from information_schema.columns
                    where table_schema=database() and table_name=? and column_name in ('creado','modificado')
                    and is_nullable='NO' and datetime_precision=6
                    """, Integer.class, tabla));
            assertEquals(0, jdbc.queryForObject("select count(*) from " + tabla + " where creado is null or modificado is null", Integer.class));
        }
    }

    @TestConfiguration
    static class ConfiguracionReloj {
        @Bean @Primary RelojPrueba relojAuditoria() { return new RelojPrueba(); }
    }

    static class RelojPrueba extends Clock {
        Instant instante = Instant.parse("2030-01-07T15:00:00.123456Z");
        @Override public ZoneId getZone() { return ZoneId.of("America/Guayaquil"); }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instante, zone); }
        @Override public Instant instant() { return instante; }
    }
}
