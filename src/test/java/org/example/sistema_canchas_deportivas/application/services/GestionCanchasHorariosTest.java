package org.example.sistema_canchas_deportivas.application.services;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.example.sistema_canchas_deportivas.domain.models.OcupacionCancha;
import org.example.sistema_canchas_deportivas.application.ports.out.*;
import org.example.sistema_canchas_deportivas.domain.models.*;
import org.example.sistema_canchas_deportivas.domain.exception.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class GestionCanchasHorariosTest {
    private final CanchaOut canchas = mock(CanchaOut.class);
    private final HorarioAtencionOut horarios = mock(HorarioAtencionOut.class);
    private final ReservaOut reservas = mock(ReservaOut.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-16T15:30:00Z"), ZoneId.of("America/Guayaquil"));
    private final CanchaService service = new CanchaService(canchas, horarios, reservas, clock);
    private final HorarioAtencionService horarioService = new HorarioAtencionService(horarios, reservas, clock);
    private final Cancha cancha = new Cancha(1L, "Tenis", TipoCancha.TENIS, null, true);

    @BeforeEach void preparar() {
        when(canchas.bloquearPorId(1L)).thenReturn(Optional.of(cancha));
        when(canchas.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(horarios.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(horarios.bloquearPorDia(DayOfWeek.MONDAY)).thenReturn(Optional.of(
                new HorarioAtencion(1L, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0))));
    }

    @Test void validaSuperficiesNombresYCompatibilidad() {
        assertThrows(DatoInvalidoException.class, () -> service.crear("Fútbol", TipoCancha.FUTBOL_11, null, true));
        assertThrows(DatoInvalidoException.class, () -> service.crear("Fútbol", TipoCancha.FUTBOL_6, Superficie.NATURAL, true));
        assertThrows(DatoInvalidoException.class, () -> service.crear(" ", TipoCancha.TENIS, null, true));
        var nueva = service.crear(" Tenis nueva ", TipoCancha.TENIS, null, true);
        assertEquals("Tenis nueva", nueva.getNombre());
        assertNull(nueva.getSuperficie());
        assertTrue(nueva.admite(Disciplina.TENIS));
        assertFalse(nueva.admite(Disciplina.VOLEY));
    }

    @Test void rechazaDuplicadosAlCrearYEditar() {
        when(canchas.existePorNombre("Tenis")).thenReturn(true);
        when(canchas.existePorNombreExcluyendoId("Otra", 1L)).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> service.crear("Tenis", TipoCancha.TENIS, null, true));
        assertThrows(ReglaNegocioException.class, () -> service.actualizar(1L, "Otra", TipoCancha.TENIS, null, true));
        verify(canchas, never()).guardar(any());
    }

    @Test void editaYDesactivaConservandoIdYDatos() {
        var editada = service.actualizar(1L, "Nombre nuevo", TipoCancha.TENIS, null, true);
        assertEquals(1L, editada.getId());
        assertEquals("Nombre nuevo", editada.getNombre());
        var inactiva = service.cambiarEstado(1L, false);
        assertEquals(cancha.getNombre(), inactiva.getNombre());
        assertFalse(inactiva.isActiva());
    }

    @Test void noCambiaTipoConHistorialAunqueEsteCancelado() {
        when(reservas.existeHistorialCancha(1L)).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> service.actualizar(1L, "Nueva", TipoCancha.BASQUET, null, true));
    }

    @Test void noDesactivaSiTieneReservasVigentes() {
        ocupar(LocalDate.of(2026, 11, 23), 10, 1);
        assertThrows(ReglaNegocioException.class, () -> service.cambiarEstado(1L, false));
        verify(canchas, never()).guardar(any());
    }

    @Test void recursoAusenteEIdInvalidoSeDistinguen() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.buscarPorId(99L));
        assertThrows(RecursoNoEncontradoException.class, () -> service.cambiarEstado(99L, false));
        assertThrows(DatoInvalidoException.class, () -> service.buscarPorId(0L));
    }

    @Test void horarioNoPuedeCerrarAntesDeTerminarUnaReservaFutura() {
        ocupar(LocalDate.of(2026, 11, 23), 15, 1);
        assertThrows(ReglaNegocioException.class, () ->
                horarioService.actualizar(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(15, 0)));
        verify(horarios, never()).guardar(any());
    }

    @Test void horarioNoPuedeAbrirDespuesDeUnaReservaEnCurso() {
        ocupar(LocalDate.of(2026, 11, 16), 10, 1);
        assertThrows(ReglaNegocioException.class, () ->
                horarioService.actualizar(DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(16, 0)));
    }

    @Test void horarioPermiteLimitesExactosYReservasHistoricas() {
        ocupar(LocalDate.of(2026, 11, 16), 8, 1); // ya finalizó a las 09:00.
        var nuevo = horarioService.actualizar(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(16, 0));
        assertEquals(1L, nuevo.getId());
        ocupar(LocalDate.of(2026, 11, 23), 14, 2);
        assertEquals(LocalTime.of(16, 0),
                horarioService.actualizar(DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(16, 0)).getCierre());
    }

    @Test void horarioRechazaIntervalosInvalidos() {
        assertThrows(DatoInvalidoException.class, () ->
                horarioService.actualizar(DayOfWeek.MONDAY, LocalTime.of(16, 0), LocalTime.of(8, 0)));
        assertThrows(DatoInvalidoException.class, () ->
                horarioService.actualizar(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(8, 0)));
        verify(horarios, never()).bloquearPorDia(any());
    }

    private void ocupar(LocalDate fecha, int hora, int duracion) {
        when(reservas.ocupacionesConfirmadas(LocalDate.now(clock), null)).thenReturn(List.of(
                new OcupacionCancha(1L, Disciplina.TENIS, fecha, LocalTime.of(hora, 0), duracion)));
    }
}
