package org.example.sistema_canchas_deportivas.application.services;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.example.sistema_canchas_deportivas.application.ports.out.*;
import org.example.sistema_canchas_deportivas.domain.models.*;
import org.example.sistema_canchas_deportivas.domain.exception.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CrudAdminServiceTest {
    final ReservaOut reservas = mock(ReservaOut.class);
    final UsuarioOut usuarios = mock(UsuarioOut.class);
    final CanchaOut canchas = mock(CanchaOut.class);
    final HorarioAtencionOut horarios = mock(HorarioAtencionOut.class);
    final UtileriaOut utileria = mock(UtileriaOut.class);
    final Clock clock = Clock.fixed(Instant.parse("2030-01-07T15:00:00Z"), ZoneId.of("America/Guayaquil"));
    final ReservaService service = new ReservaService(reservas, usuarios, canchas, horarios, clock, utileria);
    final LocalDateTime inicio = LocalDateTime.of(2030, 1, 14, 10, 0);
    Reserva actual;

    @BeforeEach void preparar() {
        actual = new Reserva(50L, 2L, 10L, Disciplina.VOLEY, inicio, 1, EstadoReserva.CONFIRMADA,
                inicio.minusDays(1), List.of(new ReservaUtileria(90L, 50L, 20L, 1, inicio.minusDays(1), inicio.minusDays(1))),
                inicio.minusDays(1), inicio.minusDays(1));
        var cliente = new Usuario(2L, "cliente", "hash", Rol.CLIENTE, true);
        when(usuarios.buscarPorId(1L)).thenReturn(Optional.of(new Usuario(1L, "admin", "hash", Rol.ADMIN, true)));
        when(usuarios.buscarPorId(2L)).thenReturn(Optional.of(cliente));
        when(usuarios.bloquearPorId(2L)).thenReturn(Optional.of(cliente));
        var cancha = new Cancha(10L, "Prueba", TipoCancha.VOLEY_ECUAVOLEY, null, true);
        when(canchas.bloquearPorId(10L)).thenReturn(Optional.of(cancha));
        when(canchas.buscarPorId(10L)).thenReturn(Optional.of(cancha));
        var horario = new HorarioAtencion(1L, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0));
        when(horarios.buscarPorDia(DayOfWeek.MONDAY)).thenReturn(Optional.of(horario));
        when(horarios.bloquearPorDia(DayOfWeek.MONDAY)).thenReturn(Optional.of(horario));
        var item = new Utileria(20L, "Balón", TipoImplemento.BALON, 2, true, Set.of(Disciplina.VOLEY));
        when(utileria.bloquearPorId(20L)).thenReturn(Optional.of(item));
        when(utileria.buscarPorId(20L)).thenReturn(Optional.of(item));
        when(reservas.referenciaPorId(50L)).thenReturn(Optional.of(new ReferenciaReserva(2L, 10L, inicio.toLocalDate())));
        when(reservas.utileriaIdsPorReserva(50L)).thenReturn(List.of(20L));
        when(reservas.bloquearPorId(50L)).thenAnswer(inv -> Optional.of(actual));
        when(reservas.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    Reserva actualizar(List<SolicitudUtileria> detalles) {
        return service.actualizarAdmin(1L, 50L, 2L, 10L, Disciplina.VOLEY, inicio, 2, EstadoReserva.CONFIRMADA, detalles);
    }

    @Test void editarExcluyeLaMismaReservaYConservaIdYAuditoriaDeLosDetalles() {
        when(reservas.confirmadasPorResponsableYFecha(2L, inicio.toLocalDate())).thenReturn(List.of(actual));
        when(reservas.confirmadasPorCanchaYFecha(10L, inicio.toLocalDate())).thenReturn(List.of(actual));
        var editada = actualizar(List.of(new SolicitudUtileria(20L, 2)));
        assertEquals(50L, editada.getId()); assertEquals(actual.getCreado(), editada.getCreado());
        assertEquals(90L, editada.getUtileria().getFirst().getId());
        assertEquals(actual.getUtileria().getFirst().getCreado(), editada.getUtileria().getFirst().getCreado());
        assertEquals(2, editada.getUtileria().getFirst().getCantidad());
        verify(utileria).compromisosExcluyendo(20L, inicio.toLocalDate(), inicio.toLocalDate(), 50L);
    }

    @Test void rechazaConflictosSinGuardarCambios() {
        var otra = new Reserva(51L, 3L, 10L, Disciplina.VOLEY, inicio, 1, EstadoReserva.CONFIRMADA, inicio.minusDays(1), List.of());
        when(reservas.confirmadasPorCanchaYFecha(10L, inicio.toLocalDate())).thenReturn(List.of(otra));
        assertThrows(ReglaNegocioException.class, () -> actualizar(List.of()));
        verify(reservas, never()).guardar(any());
    }

    @Test void stockInsuficienteNoPermiteActualizar() {
        when(utileria.compromisosExcluyendo(20L, inicio.toLocalDate(), inicio.toLocalDate(), 50L)).thenReturn(
                List.of(new CompromisoUtileria(Disciplina.VOLEY, inicio.toLocalDate(), inicio.toLocalTime(), 1, 1)));
        assertThrows(ReglaNegocioException.class, () -> actualizar(List.of(new SolicitudUtileria(20L, 2))));
        verify(reservas, never()).guardar(any());
    }

    @Test void detallesNoSeRepitenNiSeEditanSiNoPertenecenALaReserva() {
        assertThrows(ReglaNegocioException.class, () -> service.cambiarDetalleAdmin(1L, 50L, 20L, 1, true));
        when(reservas.utileriaIdsPorReserva(50L)).thenReturn(List.of());
        actual = actual.conUtileria(List.of());
        assertThrows(RecursoNoEncontradoException.class, () -> service.cambiarDetalleAdmin(1L, 50L, 20L, 1, false));
        assertThrows(DatoInvalidoException.class, () -> actualizar(List.of(new SolicitudUtileria(20L, 1), new SolicitudUtileria(20L, 1))));
        verify(reservas, never()).guardar(any());
    }

    @Test void puedeEliminarUnDetalleSinEliminarLaReserva() {
        var editada = service.cambiarDetalleAdmin(1L, 50L, 20L, null, false);
        assertTrue(editada.getUtileria().isEmpty());
        verify(reservas, never()).eliminar(any());
    }

    @Test void borradoBloqueaRecursosEnOrdenYLaReservaAlFinal() {
        service.eliminarAdmin(1L, 50L);
        var orden = inOrder(horarios, usuarios, canchas, utileria, reservas);
        orden.verify(horarios).bloquearPorDia(DayOfWeek.MONDAY);
        orden.verify(usuarios).bloquearPorId(2L); orden.verify(canchas).bloquearPorId(10L);
        orden.verify(utileria).bloquearPorId(20L); orden.verify(reservas).bloquearPorId(50L);
        orden.verify(reservas).eliminar(50L);
    }

    @Test void clienteNoTieneCrudAdministrativoNiPuedeBorrarReservas() {
        assertThrows(AccesoDenegadoException.class, () -> service.eliminarAdmin(2L, 50L));
        assertThrows(AccesoDenegadoException.class, () -> service.cancelarAdmin(2L, 50L));
        assertThrows(AccesoDenegadoException.class, () -> service.crearAdmin(2L, 2L, 10L, Disciplina.VOLEY, inicio, 1, List.of()));
        assertThrows(AccesoDenegadoException.class, () -> service.cambiarDetalleAdmin(2L, 50L, 20L, null, false));
        verify(reservas, never()).eliminar(any());
    }

    @Test void movimientoConcurrenteObligaARecargarAntesDeEliminar() {
        actual = new Reserva(50L, 2L, 11L, Disciplina.VOLEY, inicio, 1, EstadoReserva.CONFIRMADA, inicio.minusDays(1), actual.getUtileria());
        assertThrows(ReglaNegocioException.class, () -> service.eliminarAdmin(1L, 50L));
        verify(reservas, never()).eliminar(any());
    }

    @Test void adminCancelaReservaAjenaConservandoDetalle() {
        var cancelada = service.cancelarAdmin(1L, 50L);
        assertEquals(EstadoReserva.CANCELADA, cancelada.getEstado());
        assertEquals(90L, cancelada.getUtileria().getFirst().getId());
    }

    @Test void canchaYUtileriaConReferenciasNoSeBorran() {
        var gestion = new CanchaService(canchas, horarios, reservas, clock);
        when(reservas.existeHistorialCancha(10L)).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> gestion.eliminar(10L));
        var inventario = new UtileriaService(utileria, horarios, clock);
        when(utileria.tieneHistorial(20L)).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> inventario.eliminar(20L));
        verify(canchas, never()).eliminar(any()); verify(utileria, never()).eliminar(any());
    }

    @Test void horarioPermiteCrudYRechazaDuplicadosOReservasPendientes() {
        var gestion = new HorarioAtencionService(horarios, reservas, clock);
        when(horarios.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        assertEquals(DayOfWeek.SUNDAY, gestion.crear(DayOfWeek.SUNDAY, LocalTime.of(8,0), LocalTime.of(22,0)).getDia());
        when(horarios.existePorDia(DayOfWeek.SUNDAY)).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> gestion.crear(DayOfWeek.SUNDAY, LocalTime.of(8,0), LocalTime.of(22,0)));
        when(reservas.ocupacionesConfirmadas(any(), isNull())).thenReturn(List.of(new OcupacionCancha(10L, Disciplina.VOLEY, inicio.toLocalDate(), inicio.toLocalTime(), 1)));
        assertThrows(ReglaNegocioException.class, () -> gestion.eliminar(DayOfWeek.MONDAY));
        when(reservas.ocupacionesConfirmadas(any(), isNull())).thenReturn(List.of());
        gestion.eliminar(DayOfWeek.MONDAY); verify(horarios).eliminar(1L);
    }
}
