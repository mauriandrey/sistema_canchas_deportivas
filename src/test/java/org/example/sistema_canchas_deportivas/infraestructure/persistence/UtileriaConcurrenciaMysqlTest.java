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
import org.example.sistema_canchas_deportivas.domain.models.SolicitudUtileria;
import org.example.sistema_canchas_deportivas.application.ports.in.*;
import org.example.sistema_canchas_deportivas.application.ports.out.*;
import org.example.sistema_canchas_deportivas.domain.models.*;
import org.example.sistema_canchas_deportivas.domain.exception.ReglaNegocioException;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.jpa.show-sql=false")
@ActiveProfiles({"local", "test"})
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_TESTS", matches = "true")
class UtileriaConcurrenciaMysqlTest {
    @Autowired ReservaIn servicio;
    @Autowired UtileriaIn gestion;
    @Autowired UsuarioOut usuarios;
    @Autowired CanchaOut canchas;
    @Autowired UtileriaOut utileria;
    @Autowired ReservaOut reservas;
    @Autowired PlatformTransactionManager manager;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;
    TransactionTemplate tx;
    ExecutorService executor;
    Long usuarioA, usuarioB, canchaA, canchaB, itemA, itemB;
    LocalDate fecha;

    @BeforeEach void preparar() {
        tx = new TransactionTemplate(manager);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        tx.setTimeout(20);
        executor = Executors.newFixedThreadPool(3);
        fecha = LocalDate.now(clock).with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(2);
        tx.executeWithoutResult(status -> {
            String nombre = "f6_" + UUID.randomUUID().toString().replace("-", "");
            usuarios.guardar(Usuario.nuevo(nombre + "a", "hash-fixture", Rol.CLIENTE));
            usuarios.guardar(Usuario.nuevo(nombre + "b", "hash-fixture", Rol.CLIENTE));
            usuarioA = usuarios.buscarPorUsername(nombre + "a").orElseThrow().getId();
            usuarioB = usuarios.buscarPorUsername(nombre + "b").orElseThrow().getId();
            canchaA = canchas.guardar(Cancha.nuevo(nombre + "a", TipoCancha.VOLEY_ECUAVOLEY, null, true)).getId();
            canchaB = canchas.guardar(Cancha.nuevo(nombre + "b", TipoCancha.VOLEY_ECUAVOLEY, null, true)).getId();
            itemA = gestion.crear(nombre + "a", TipoImplemento.BALON, 1, true, Set.of(Disciplina.VOLEY)).getId();
            itemB = gestion.crear(nombre + "b", TipoImplemento.CHALECO, 1, true, Set.of(Disciplina.VOLEY)).getId();
        });
    }

    @AfterEach void limpiar() throws Exception {
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(20, TimeUnit.SECONDS));
        if (usuarioA == null || usuarioB == null) return;
        tx.executeWithoutResult(status -> {
            jdbc.update("delete d from reserva_utileria d join reservas r on d.reserva_id=r.id where r.responsable_id in (?,?)", usuarioA, usuarioB);
            jdbc.update("delete from reservas where responsable_id in (?,?)", usuarioA, usuarioB);
            jdbc.update("delete from utileria_disciplinas where utileria_id in (?,?)", itemA, itemB);
            jdbc.update("delete from utileria where id in (?,?)", itemA, itemB);
            jdbc.update("delete from canchas where id in (?,?)", canchaA, canchaB);
            jdbc.update("delete from usuarios where id in (?,?)", usuarioA, usuarioB);
        });
    }

    @Test void soloUnaReservaObtieneLaUltimaUnidadEnCanchasDistintas() throws Exception {
        assertUnGanador(simultaneas(
                () -> crear(usuarioA, canchaA, 10, 1, List.of(new SolicitudUtileria(itemA, 1))),
                () -> crear(usuarioB, canchaB, 10, 1, List.of(new SolicitudUtileria(itemA, 1)))));
        assertEquals(1, utileria.compromisosConfirmados(itemA, fecha, fecha).size());
        assertEquals(1, utileria.buscarPorId(itemA).orElseThrow().getExistencias());
    }

    @Test void noSeAsignanCuatroUnidadesCuandoSoloExistenTres() throws Exception {
        gestion.cambiarExistencias(itemA, 3);
        assertUnGanador(simultaneas(
                () -> crear(usuarioA, canchaA, 10, 1, List.of(new SolicitudUtileria(itemA, 2))),
                () -> crear(usuarioB, canchaB, 10, 1, List.of(new SolicitudUtileria(itemA, 2)))));
        assertEquals(2, utileria.compromisosConfirmados(itemA, fecha, fecha).getFirst().cantidad());
    }

    @Test void reservasConsecutivasReutilizanLaUnicaUnidad() throws Exception {
        assertEquals(List.of("OK", "OK"), simultaneas(
                () -> crear(usuarioA, canchaA, 10, 1, List.of(new SolicitudUtileria(itemA, 1))),
                () -> crear(usuarioB, canchaB, 11, 1, List.of(new SolicitudUtileria(itemA, 1)))));
        assertEquals(1, CompromisoUtileria.maximoSimultaneo(utileria.compromisosConfirmados(itemA, fecha, fecha),
                fecha.atTime(10, 0), fecha.atTime(12, 0)));
    }

    @Test void solicitudesConImplementosEnOrdenInversoSeCompletanSinInterbloqueo() throws Exception {
        gestion.cambiarExistencias(itemA, 2);
        gestion.cambiarExistencias(itemB, 2);
        assertEquals(List.of("OK", "OK"), simultaneas(
                () -> crear(usuarioA, canchaA, 10, 1, List.of(new SolicitudUtileria(itemA, 1), new SolicitudUtileria(itemB, 1))),
                () -> crear(usuarioB, canchaB, 10, 1, List.of(new SolicitudUtileria(itemB, 1), new SolicitudUtileria(itemA, 1)))));
        assertEquals(2, utileria.compromisosConfirmados(itemA, fecha, fecha).size());
        assertEquals(2, utileria.compromisosConfirmados(itemB, fecha, fecha).size());
    }

    @Test void reservaEsperaCambioDeStockYRechazaElNuevoCero() throws Exception {
        var listo = new CountDownLatch(1); var liberar = new CountDownLatch(1);
        var editor = executor.submit(() -> tx.executeWithoutResult(status -> {
            gestion.cambiarExistencias(itemA, 0);
            listo.countDown(); esperar(liberar);
        }));
        try {
            assertTrue(listo.await(5, TimeUnit.SECONDS));
            var intento = executor.submit(() -> resultado(() -> crear(usuarioA, canchaA, 10, 1, List.of(new SolicitudUtileria(itemA, 1)))));
            assertThrows(TimeoutException.class, () -> intento.get(300, TimeUnit.MILLISECONDS));
            liberar.countDown();
            editor.get(10, TimeUnit.SECONDS);
            assertEquals("CONFLICTO", intento.get(10, TimeUnit.SECONDS));
            assertTrue(reservas.listarPorResponsable(usuarioA).isEmpty());
        } finally { liberar.countDown(); }
    }

    @Test void reducirStockEsperaReservaYNoInvalidaElCompromisoConfirmado() throws Exception {
        var listo = new CountDownLatch(1); var liberar = new CountDownLatch(1);
        var creador = executor.submit(() -> tx.executeWithoutResult(status -> {
            crear(usuarioA, canchaA, 10, 1, List.of(new SolicitudUtileria(itemA, 1)));
            listo.countDown(); esperar(liberar);
        }));
        try {
            assertTrue(listo.await(5, TimeUnit.SECONDS));
            var cambio = executor.submit(() -> {
                try { gestion.cambiarExistencias(itemA, 0); return "OK"; }
                catch (ReglaNegocioException e) { return "CONFLICTO"; }
            });
            assertThrows(TimeoutException.class, () -> cambio.get(300, TimeUnit.MILLISECONDS));
            liberar.countDown();
            creador.get(10, TimeUnit.SECONDS);
            assertEquals("CONFLICTO", cambio.get(10, TimeUnit.SECONDS));
            assertEquals(1, utileria.buscarPorId(itemA).orElseThrow().getExistencias());
        } finally { liberar.countDown(); }
    }

    @Test void cancelacionLiberaLaUnidadAntesDeLaNuevaAsignacion() throws Exception {
        var original = crear(usuarioA, canchaA, 10, 1, List.of(new SolicitudUtileria(itemA, 1)));
        var listo = new CountDownLatch(1); var liberar = new CountDownLatch(1);
        var cancelador = executor.submit(() -> tx.executeWithoutResult(status -> {
            servicio.cancelar(usuarioA, original.getId());
            listo.countDown(); esperar(liberar);
        }));
        try {
            assertTrue(listo.await(5, TimeUnit.SECONDS));
            var nueva = executor.submit(() -> resultado(() -> crear(usuarioB, canchaB, 10, 1, List.of(new SolicitudUtileria(itemA, 1)))));
            assertThrows(TimeoutException.class, () -> nueva.get(300, TimeUnit.MILLISECONDS));
            liberar.countDown();
            cancelador.get(10, TimeUnit.SECONDS);
            assertEquals("OK", nueva.get(10, TimeUnit.SECONDS));
            assertEquals(EstadoReserva.CANCELADA, reservas.buscarPorId(original.getId()).orElseThrow().getEstado());
            assertEquals(1, utileria.compromisosConfirmados(itemA, fecha, fecha).size());
        } finally { liberar.countDown(); }
    }

    private Reserva crear(Long usuario, Long cancha, int hora, int duracion, List<SolicitudUtileria> detalles) {
        return servicio.crear(usuario, cancha, Disciplina.VOLEY, fecha.atTime(hora, 0), duracion, detalles);
    }

    @Test void agregarDetalleAdministrativoYCrearReservaCompitenPorLaUltimaUnidad() throws Exception {
        Long adminId = usuarios.buscarPorUsername("admin").orElseThrow().getId();
        var original = crear(usuarioA, canchaA, 10, 1, List.of());
        assertUnGanador(simultaneas(
                () -> servicio.cambiarDetalleAdmin(adminId, original.getId(), itemA, 1, true),
                () -> crear(usuarioB, canchaB, 10, 1, List.of(new SolicitudUtileria(itemA, 1)))));
        assertEquals(1, CompromisoUtileria.maximoSimultaneo(utileria.compromisosConfirmados(itemA, fecha, fecha),
                fecha.atTime(10, 0), fecha.atTime(11, 0)));
    }
    private List<String> simultaneas(Callable<Reserva> primera, Callable<Reserva> segunda) throws Exception {
        var listas = new CountDownLatch(2); var iniciar = new CountDownLatch(1);
        List<Future<String>> futuros = new ArrayList<>();
        for (var operacion : List.of(primera, segunda)) futuros.add(executor.submit(() -> {
            listas.countDown(); esperar(iniciar); return resultado(operacion);
        }));
        try { assertTrue(listas.await(5, TimeUnit.SECONDS)); } finally { iniciar.countDown(); }
        return List.of(futuros.get(0).get(20, TimeUnit.SECONDS), futuros.get(1).get(20, TimeUnit.SECONDS));
    }
    private String resultado(Callable<Reserva> operacion) throws Exception {
        try { operacion.call(); return "OK"; } catch (ReglaNegocioException e) { return "CONFLICTO"; }
    }
    private void esperar(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Tiempo de coordinación agotado");
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
    }
    private void assertUnGanador(List<String> resultados) {
        assertEquals(1, resultados.stream().filter("OK"::equals).count());
        assertEquals(1, resultados.stream().filter("CONFLICTO"::equals).count());
    }
}
