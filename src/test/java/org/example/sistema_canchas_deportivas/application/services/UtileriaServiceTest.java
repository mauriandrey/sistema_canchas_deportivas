package org.example.sistema_canchas_deportivas.application.services;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.example.sistema_canchas_deportivas.domain.models.SolicitudUtileria;
import org.example.sistema_canchas_deportivas.application.ports.out.*;
import org.example.sistema_canchas_deportivas.domain.models.*;
import org.example.sistema_canchas_deportivas.domain.exception.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class UtileriaServiceTest {
    final UtileriaOut utileria = mock(UtileriaOut.class);
    final HorarioAtencionOut horarios = mock(HorarioAtencionOut.class);
    final ReservaOut reservas = mock(ReservaOut.class);
    final UsuarioOut usuarios = mock(UsuarioOut.class);
    final CanchaOut canchas = mock(CanchaOut.class);
    final Clock clock = Clock.fixed(Instant.parse("2030-01-07T15:00:00Z"), ZoneId.of("America/Guayaquil"));
    final UtileriaService service = new UtileriaService(utileria, horarios, clock);
    final ReservaService reservaService = new ReservaService(reservas, usuarios, canchas, horarios, clock, utileria);
    final LocalDateTime inicio = LocalDateTime.of(2030, 1, 14, 10, 0);

    @BeforeEach void preparar() {
        var horario = new HorarioAtencion(1L, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0));
        when(horarios.bloquearPorDia(any())).thenReturn(Optional.of(horario));
        when(horarios.buscarPorDia(any())).thenReturn(Optional.of(horario));
        when(usuarios.bloquearPorId(1L)).thenReturn(Optional.of(new Usuario(1L, "cliente", "hash", Rol.CLIENTE, true)));
        when(canchas.bloquearPorId(1L)).thenReturn(Optional.of(new Cancha(1L, "Compartida", TipoCancha.VOLEY_ECUAVOLEY, null, true)));
        when(utileria.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reservas.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test void picoCuentaSoloUnidadesSimultaneasYRespetaExtremos() {
        var ocupaciones = List.of(compromiso(9, 1, 100), compromiso(10, 1, 2), compromiso(11, 1, 3), compromiso(12, 1, 100));
        assertEquals(3, CompromisoUtileria.maximoSimultaneo(ocupaciones, inicio, inicio.plusHours(2)));
        assertEquals(2, CompromisoUtileria.maximoSimultaneo(ocupaciones, inicio, inicio.plusHours(1)));
    }

    @Test void picoSumaReservasQueSeCruzanYNoDesbordaUnEntero() {
        assertEquals(5, CompromisoUtileria.maximoSimultaneo(
                List.of(compromiso(10, 2, 2), compromiso(11, 1, 3)), inicio, inicio.plusHours(2)));
        assertEquals(2L * Integer.MAX_VALUE, CompromisoUtileria.maximoSimultaneo(
                List.of(compromiso(10, 1, Integer.MAX_VALUE), compromiso(10, 1, Integer.MAX_VALUE)), inicio, inicio.plusHours(1)));
    }

    @Test void disponibilidadFiltraCompatibilidadYEstadoYMuestraStockCero() {
        var activo = item(1L, 4);
        var cero = item(2L, 0);
        when(utileria.listar()).thenReturn(List.of(activo, cero,
                new Utileria(3L, "Inactivo", TipoImplemento.BALON, 8, false, Set.of(Disciplina.VOLEY)),
                new Utileria(4L, "Tenis", TipoImplemento.RAQUETA, 8, true, Set.of(Disciplina.TENIS))));
        when(utileria.compromisosConfirmados(1L, inicio.toLocalDate(), inicio.toLocalDate())).thenReturn(List.of(compromiso(10, 1, 2)));
        var respuesta = service.disponibilidad(inicio, 2, Disciplina.VOLEY);
        assertEquals(2, respuesta.size());
        assertEquals(2, respuesta.getFirst().disponibles());
        assertEquals(2, respuesta.getFirst().comprometidas());
        assertEquals(0, respuesta.getLast().disponibles());
    }

    @Test void reservaSinImplementosNoConsultaNiModificaInventario() {
        assertTrue(reservaService.crear(1L, 1L, Disciplina.VOLEY, inicio, 1).getUtileria().isEmpty());
        assertTrue(crear(null).getUtileria().isEmpty());
        assertTrue(crear(List.of()).getUtileria().isEmpty());
        verifyNoInteractions(utileria);
    }

    @Test void variosImplementosSeBloqueanPorIdYNoSeDescuentan() {
        when(utileria.bloquearPorId(10L)).thenReturn(Optional.of(item(10L, 2)));
        when(utileria.bloquearPorId(20L)).thenReturn(Optional.of(item(20L, 3)));
        var reserva = crear(List.of(new SolicitudUtileria(20L, 2), new SolicitudUtileria(10L, 1)));
        assertEquals(List.of(10L, 20L), reserva.getUtileria().stream().map(ReservaUtileria::getUtileriaId).toList());
        var orden = inOrder(utileria);
        orden.verify(utileria).bloquearPorId(10L);
        orden.verify(utileria).bloquearPorId(20L);
        verify(utileria, never()).guardar(any());
    }

    @Test void validaCantidadesDuplicadosYEntradasNulasAntesDeBloquear() {
        assertThrows(DatoInvalidoException.class, () -> crear(List.of(new SolicitudUtileria(1L, 0))));
        assertThrows(DatoInvalidoException.class, () -> crear(List.of(new SolicitudUtileria(1L, -1))));
        assertThrows(DatoInvalidoException.class, () -> crear(List.of(new SolicitudUtileria(null, 1))));
        assertThrows(DatoInvalidoException.class, () -> crear(Arrays.asList((SolicitudUtileria) null)));
        assertThrows(DatoInvalidoException.class, () -> crear(List.of(new SolicitudUtileria(1L, 1), new SolicitudUtileria(1L, 2))));
        verifyNoInteractions(utileria);
        verify(reservas, never()).guardar(any());
    }

    @Test void rechazaImplementoInexistenteInactivoOIncompatible() {
        assertThrows(RecursoNoEncontradoException.class, () -> crear(List.of(new SolicitudUtileria(1L, 1))));
        when(utileria.bloquearPorId(1L)).thenReturn(Optional.of(new Utileria(1L, "Balón", TipoImplemento.BALON, 1, false, Set.of(Disciplina.VOLEY))));
        assertThrows(ReglaNegocioException.class, () -> crear(List.of(new SolicitudUtileria(1L, 1))));
        when(utileria.bloquearPorId(1L)).thenReturn(Optional.of(new Utileria(1L, "Raqueta", TipoImplemento.RAQUETA, 1, true, Set.of(Disciplina.TENIS))));
        assertThrows(DatoInvalidoException.class, () -> crear(List.of(new SolicitudUtileria(1L, 1))));
        verify(reservas, never()).guardar(any());
    }

    @Test void capacidadTemporalSeValidaAntesDeGuardarCualquierParte() {
        when(utileria.bloquearPorId(1L)).thenReturn(Optional.of(item(1L, 2)));
        when(utileria.compromisosConfirmados(1L, inicio.toLocalDate(), inicio.toLocalDate())).thenReturn(List.of(compromiso(10, 1, 2)));
        assertThrows(ReglaNegocioException.class, () -> crear(List.of(new SolicitudUtileria(1L, 1))));
        verify(reservas, never()).guardar(any());
    }

    @Test void administracionNoReduceStockPorDebajoDelPicoPeroPermiteReutilizacion() {
        when(utileria.bloquearPorId(1L)).thenReturn(Optional.of(item(1L, 4)));
        when(utileria.compromisosConfirmados(1L, LocalDate.now(clock), null)).thenReturn(List.of(compromiso(10, 1, 2), compromiso(11, 1, 2)));
        assertEquals(2, service.cambiarExistencias(1L, 2).getExistencias());
        assertThrows(ReglaNegocioException.class, () -> service.cambiarExistencias(1L, 1));
    }

    @Test void administracionNoDesactivaNiQuitaCompatibilidadConReservasPendientes() {
        when(utileria.bloquearPorId(1L)).thenReturn(Optional.of(item(1L, 4)));
        when(utileria.compromisosConfirmados(1L, LocalDate.now(clock), null)).thenReturn(List.of(compromiso(10, 1, 1)));
        assertThrows(ReglaNegocioException.class, () -> service.actualizar(1L, "Balón", TipoImplemento.BALON, 4, false, Set.of(Disciplina.VOLEY)));
        assertThrows(ReglaNegocioException.class, () -> service.actualizar(1L, "Balón", TipoImplemento.BALON, 4, true, Set.of(Disciplina.ECUAVOLEY)));
    }

    @Test void tipoNoCambiaConHistorialYNombreEsUnico() {
        when(utileria.bloquearPorId(1L)).thenReturn(Optional.of(item(1L, 0)));
        when(utileria.tieneHistorial(1L)).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> service.actualizar(1L, "Raqueta", TipoImplemento.RAQUETA, 0, true, Set.of(Disciplina.TENIS)));
        when(utileria.existePorNombre("Balón")).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> service.crear(" Balón ", TipoImplemento.BALON, 0, true, Set.of(Disciplina.VOLEY)));
    }

    @Test void validaCatalogoYConsultaDeDisponibilidad() {
        assertThrows(DatoInvalidoException.class, () -> service.crear("Balón", TipoImplemento.BALON, -1, true, Set.of(Disciplina.VOLEY)));
        assertThrows(DatoInvalidoException.class, () -> service.crear("Balón", TipoImplemento.BALON, 0, true, Set.of()));
        assertThrows(DatoInvalidoException.class, () -> service.disponibilidad(inicio.plusMinutes(30), 1, Disciplina.VOLEY));
        assertThrows(ReglaNegocioException.class, () -> service.disponibilidad(inicio.withHour(15), 2, Disciplina.VOLEY));
    }

    private Utileria item(Long id, int stock) { return new Utileria(id, "Balón " + id, TipoImplemento.BALON, stock, true, Set.of(Disciplina.VOLEY)); }
    private CompromisoUtileria compromiso(int hora, int duracion, int cantidad) {
        return new CompromisoUtileria(Disciplina.VOLEY, inicio.toLocalDate(), LocalTime.of(hora, 0), duracion, cantidad);
    }
    private Reserva crear(List<SolicitudUtileria> detalles) { return reservaService.crear(1L, 1L, Disciplina.VOLEY, inicio, 2, detalles); }
}
