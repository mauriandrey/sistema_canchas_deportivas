package org.example.sistema_canchas_deportivas.infraestructure.persistence;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.example.sistema_canchas_deportivas.application.ports.in.*;
import org.example.sistema_canchas_deportivas.application.ports.out.*;
import org.example.sistema_canchas_deportivas.domain.models.*;
import org.example.sistema_canchas_deportivas.domain.exception.ReglaNegocioException;
import static org.junit.jupiter.api.Assertions.*;

/** Transacciones independientes y commits reales; se eliminan únicamente fixtures identificados por ID. */
@SpringBootTest(properties = "spring.jpa.show-sql=false")
@ActiveProfiles({"local", "test"})
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_TESTS", matches = "true")
class ReservaConcurrenciaMysqlTest {
    @Autowired ReservaIn servicio;
    @Autowired HorarioAtencionIn gestionHorarios;
    @Autowired UsuarioOut usuarios;
    @Autowired CanchaOut canchas;
    @Autowired HorarioAtencionOut horarios;
    @Autowired ReservaOut reservas;
    @Autowired PlatformTransactionManager manager;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;
    private TransactionTemplate tx;
    private ExecutorService executor;
    private Long usuarioA, usuarioB, canchaA, canchaB;
    private LocalDate fecha;

    @BeforeEach void preparar() {
        tx = new TransactionTemplate(manager);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        tx.setTimeout(20);
        executor = Executors.newFixedThreadPool(3);
        fecha = LocalDate.now(clock).with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(2);
        tx.executeWithoutResult(status -> {
            String nombre = "f5_" + UUID.randomUUID().toString().replace("-", "");
            usuarios.guardar(Usuario.nuevo(nombre + "a", "hash-fixture", Rol.CLIENTE));
            usuarios.guardar(Usuario.nuevo(nombre + "b", "hash-fixture", Rol.CLIENTE));
            usuarioA = usuarios.buscarPorUsername(nombre + "a").orElseThrow().getId();
            usuarioB = usuarios.buscarPorUsername(nombre + "b").orElseThrow().getId();
            canchaA = canchas.guardar(Cancha.nuevo(nombre + "a", TipoCancha.VOLEY_ECUAVOLEY, null, true)).getId();
            canchaB = canchas.guardar(Cancha.nuevo(nombre + "b", TipoCancha.VOLEY_ECUAVOLEY, null, true)).getId();
        });
    }

    @AfterEach void limpiar() throws Exception {
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(20, TimeUnit.SECONDS), "Las transacciones deben haber finalizado antes de limpiar");
        if (usuarioA == null || usuarioB == null) return;
        tx.executeWithoutResult(status -> {
            jdbc.update("""
                    delete d from reserva_utileria d join reservas r on d.reserva_id=r.id
                    where r.responsable_id in (?,?)
                    """, usuarioA, usuarioB);
            jdbc.update("delete from reservas where responsable_id in (?,?)", usuarioA, usuarioB);
            jdbc.update("delete from canchas where id in (?,?)", canchaA, canchaB);
            jdbc.update("delete from usuarios where id in (?,?)", usuarioA, usuarioB);
        });
    }

    @Test void soloUnaSolicitudConfirmaLaMismaCanchaEntreVoleyYEcuavoley() throws Exception {
        var resultados = simultaneas(
                () -> servicio.crear(usuarioA, canchaA, Disciplina.VOLEY, fecha.atTime(10, 0), 1),
                () -> servicio.crear(usuarioB, canchaA, Disciplina.ECUAVOLEY, fecha.atTime(10, 0), 1));
        assertUnGanador(resultados);
        assertEquals(1, reservas.confirmadasPorCanchaYFecha(canchaA, fecha).size());
    }

    @Test void limiteDiarioSeProtegeAunqueSeReservanCanchasYHorasDistintas() throws Exception {
        assertUnGanador(simultaneas(
                () -> servicio.crear(usuarioA, canchaA, Disciplina.VOLEY, fecha.atTime(10, 0), 2),
                () -> servicio.crear(usuarioA, canchaB, Disciplina.VOLEY, fecha.atTime(14, 0), 2)));
        assertEquals(2, reservas.confirmadasPorResponsableYFecha(usuarioA, fecha).stream().mapToInt(Reserva::getDuracionHoras).sum());
    }

    @Test void responsableNoConfirmaDosReservasSuperpuestasEnCanchasDistintas() throws Exception {
        assertUnGanador(simultaneas(
                () -> servicio.crear(usuarioA, canchaA, Disciplina.VOLEY, fecha.atTime(10, 0), 1),
                () -> servicio.crear(usuarioA, canchaB, Disciplina.VOLEY, fecha.atTime(10, 0), 1)));
        assertEquals(1, reservas.confirmadasPorResponsableYFecha(usuarioA, fecha).size());
    }

    @Test void dosSolicitudesConsecutivasEnLaMismaCanchaSiSeConfirman() throws Exception {
        assertEquals(List.of("OK", "OK"), simultaneas(
                () -> servicio.crear(usuarioA, canchaA, Disciplina.VOLEY, fecha.atTime(10, 0), 1),
                () -> servicio.crear(usuarioB, canchaA, Disciplina.VOLEY, fecha.atTime(11, 0), 1)));
        assertEquals(2, reservas.confirmadasPorCanchaYFecha(canchaA, fecha).size());
    }

    @Test void esperaLaEdicionDeCanchaYLeeElEstadoConfirmadoMasReciente() throws Exception {
        var bloqueada = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);
        var editor = executor.submit(() -> tx.executeWithoutResult(status -> {
            var cancha = canchas.bloquearPorId(canchaA).orElseThrow();
            canchas.guardar(cancha.actualizar(cancha.getNombre(), cancha.getTipo(), cancha.getSuperficie(), false));
            bloqueada.countDown();
            esperar(liberar);
        }));
        try {
            assertTrue(bloqueada.await(5, TimeUnit.SECONDS));
            var intento = executor.submit(() -> resultado(() ->
                    servicio.crear(usuarioA, canchaA, Disciplina.VOLEY, fecha.atTime(10, 0), 1)));
            assertThrows(TimeoutException.class, () -> intento.get(300, TimeUnit.MILLISECONDS));
            liberar.countDown();
            editor.get(10, TimeUnit.SECONDS);
            assertEquals("CONFLICTO", intento.get(10, TimeUnit.SECONDS));
            assertTrue(reservas.confirmadasPorCanchaYFecha(canchaA, fecha).isEmpty());
        } finally {
            liberar.countDown();
        }
    }

    @Test void cambiarHorarioEsperaLaReservaYRechazaInvalidarlaTrasSuCommit() throws Exception {
        var original = horarios.buscarPorDia(fecha.getDayOfWeek()).orElseThrow();
        var reservada = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);
        var creador = executor.submit(() -> tx.executeWithoutResult(status -> {
            servicio.crear(usuarioA, canchaA, Disciplina.VOLEY, fecha.atTime(10, 0), 1);
            reservada.countDown();
            esperar(liberar);
        }));
        Future<String> editor = null;
        try {
            assertTrue(reservada.await(5, TimeUnit.SECONDS));
            editor = executor.submit(() -> {
                try {
                    gestionHorarios.actualizar(fecha.getDayOfWeek(), LocalTime.of(8, 0), LocalTime.of(10, 0));
                    return "OK";
                } catch (ReglaNegocioException e) { return "CONFLICTO"; }
            });
            Future<String> esperando = editor;
            assertThrows(TimeoutException.class, () -> esperando.get(300, TimeUnit.MILLISECONDS));
            liberar.countDown();
            creador.get(10, TimeUnit.SECONDS);
            assertEquals("CONFLICTO", editor.get(10, TimeUnit.SECONDS));
            assertEquals(original.getCierre(), horarios.buscarPorDia(fecha.getDayOfWeek()).orElseThrow().getCierre());
        } finally {
            liberar.countDown();
            creador.get(10, TimeUnit.SECONDS);
            if (editor != null) editor.get(10, TimeUnit.SECONDS);
            // Restauración exacta de la configuración compartida incluso si una regresión hizo fallar la prueba.
            tx.executeWithoutResult(status -> jdbc.update("""
                    update horarios_atencion set apertura=?, cierre=?, creado=?, modificado=? where id=?
                    """, original.getApertura(), original.getCierre(), original.getCreado(), original.getModificado(), original.getId()));
        }
    }

    private List<String> simultaneas(Callable<Reserva> primera, Callable<Reserva> segunda) throws Exception {
        var listas = new CountDownLatch(2);
        var iniciar = new CountDownLatch(1);
        List<Future<String>> futuros = new ArrayList<>();
        for (var operacion : List.of(primera, segunda)) {
            futuros.add(executor.submit(() -> {
                listas.countDown();
                esperar(iniciar);
                return resultado(operacion);
            }));
        }
        try {
            assertTrue(listas.await(5, TimeUnit.SECONDS));
        } finally { iniciar.countDown(); }
        return List.of(futuros.get(0).get(20, TimeUnit.SECONDS), futuros.get(1).get(20, TimeUnit.SECONDS));
    }

    @Test void edicionAdministrativaCompiteConNuevaReservaSinDuplicarLaCancha() throws Exception {
        Long adminId = usuarios.buscarPorUsername("admin").orElseThrow().getId();
        var original = servicio.crear(usuarioA, canchaA, Disciplina.VOLEY, fecha.atTime(9, 0), 1);
        assertUnGanador(simultaneas(
                () -> servicio.actualizarAdmin(adminId, original.getId(), usuarioA, canchaA, Disciplina.VOLEY,
                        fecha.atTime(10, 0), 1, EstadoReserva.CONFIRMADA, List.of()),
                () -> servicio.crear(usuarioB, canchaA, Disciplina.VOLEY, fecha.atTime(10, 0), 1)));
        assertEquals(1, reservas.confirmadasPorCanchaYFecha(canchaA, fecha).stream()
                .filter(r -> r.getHoraInicio().equals(LocalTime.of(10, 0))).count());
    }

    @Test void dosEdicionesDeFechasEnOrdenInversoNoSeInterbloquean() throws Exception {
        Long adminId = usuarios.buscarPorUsername("admin").orElseThrow().getId();
        var primera = servicio.crear(usuarioA, canchaA, Disciplina.VOLEY, fecha.atTime(9, 0), 1);
        var segunda = servicio.crear(usuarioB, canchaB, Disciplina.VOLEY, fecha.plusDays(1).atTime(9, 0), 1);
        assertEquals(List.of("OK", "OK"), simultaneas(
                () -> servicio.actualizarAdmin(adminId, primera.getId(), usuarioA, canchaA, Disciplina.VOLEY,
                        fecha.plusDays(1).atTime(10, 0), 1, EstadoReserva.CONFIRMADA, List.of()),
                () -> servicio.actualizarAdmin(adminId, segunda.getId(), usuarioB, canchaB, Disciplina.VOLEY,
                        fecha.atTime(10, 0), 1, EstadoReserva.CONFIRMADA, List.of())));
    }

    private String resultado(Callable<Reserva> operacion) throws Exception {
        try { operacion.call(); return "OK"; }
        catch (ReglaNegocioException e) { return "CONFLICTO"; }
    }

    private void esperar(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Tiempo de coordinación agotado");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private void assertUnGanador(List<String> resultados) {
        assertEquals(1, resultados.stream().filter("OK"::equals).count());
        assertEquals(1, resultados.stream().filter("CONFLICTO"::equals).count());
    }
}
