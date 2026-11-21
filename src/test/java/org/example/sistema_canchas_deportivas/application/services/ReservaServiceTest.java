package org.example.sistema_canchas_deportivas.application.services;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.example.sistema_canchas_deportivas.domain.models.ReferenciaReserva;
import org.example.sistema_canchas_deportivas.application.ports.out.*;
import org.example.sistema_canchas_deportivas.domain.models.*;
import org.example.sistema_canchas_deportivas.domain.exception.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ReservaServiceTest {
    final ReservaOut reservas = mock(ReservaOut.class);
    final UsuarioOut usuarios = mock(UsuarioOut.class);
    final CanchaOut canchas = mock(CanchaOut.class);
    final HorarioAtencionOut horarios = mock(HorarioAtencionOut.class);
    final Clock clock = Clock.fixed(Instant.parse("2030-01-07T15:00:00Z"), ZoneId.of("America/Guayaquil"));
    final UtileriaOut utileria = mock(UtileriaOut.class);
    final ReservaService service = new ReservaService(reservas, usuarios, canchas, horarios, clock, utileria);
    final LocalDateTime inicio = LocalDateTime.of(2030, 1, 14, 10, 0);
    final Usuario cliente = new Usuario(1L, "cliente", "hash", Rol.CLIENTE, true);

    @BeforeEach void preparar() {
        when(horarios.bloquearPorDia(any())).thenReturn(Optional.of(new HorarioAtencion(1L, DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(16, 0))));
        when(usuarios.bloquearPorId(1L)).thenReturn(Optional.of(cliente));
        when(usuarios.buscarPorId(1L)).thenReturn(Optional.of(cliente));
        when(canchas.bloquearPorId(10L)).thenReturn(Optional.of(new Cancha(10L, "Compartida", TipoCancha.VOLEY_ECUAVOLEY, null, true)));
        when(reservas.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @ParameterizedTest @ValueSource(ints = {1, 2})
    void confirmaUnaODosHorasSinUtileriaYBloqueaEnOrden(int duracion) {
        var creada = service.crear(1L, 10L, Disciplina.VOLEY, inicio, duracion);
        assertEquals(EstadoReserva.CONFIRMADA, creada.getEstado());
        assertEquals(inicio.plusHours(duracion), creada.getFin());
        assertEquals(1L, creada.getResponsableId());
        assertTrue(creada.getUtileria().isEmpty());
        var orden = inOrder(horarios, usuarios, canchas, reservas);
        orden.verify(horarios).bloquearPorDia(DayOfWeek.MONDAY);
        orden.verify(usuarios).bloquearPorId(1L);
        orden.verify(canchas).bloquearPorId(10L);
        orden.verify(reservas).confirmadasPorResponsableYFecha(1L, inicio.toLocalDate());
        orden.verify(reservas).confirmadasPorCanchaYFecha(10L, inicio.toLocalDate());
        orden.verify(reservas).guardar(any());
    }

    @Test void rechazaPasadoInicioFraccionadoYDuracionInvalida() {
        assertThrows(DatoInvalidoException.class, () -> crear(LocalDateTime.now(clock).minusHours(1), 1));
        assertThrows(DatoInvalidoException.class, () -> crear(inicio.plusMinutes(1), 1));
        assertThrows(DatoInvalidoException.class, () -> crear(inicio.plusSeconds(1), 1));
        assertThrows(DatoInvalidoException.class, () -> crear(inicio, 3));
        verifyNoInteractions(horarios);
    }

    @Test void noPermiteSuperarCierrePeroAceptaTerminarExactamenteAlCerrar() {
        assertThrows(ReglaNegocioException.class, () -> crear(inicio.withHour(15), 2));
        assertThrows(ReglaNegocioException.class, () -> crear(inicio.withHour(7), 1));
        assertEquals(LocalTime.of(16, 0), crear(inicio.withHour(14), 2).getHoraFin());
    }

    @Test void canchaInactivaNoSePuedeReservar() {
        when(canchas.bloquearPorId(10L)).thenReturn(Optional.of(new Cancha(10L, "Compartida", TipoCancha.VOLEY_ECUAVOLEY, null, false)));
        assertThrows(ReglaNegocioException.class, () -> crear(inicio, 1));
        verify(reservas, never()).guardar(any());
    }

    @Test void canchaODiaInexistentesDevuelvenNoEncontrado() {
        when(canchas.bloquearPorId(10L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> crear(inicio, 1));
        when(horarios.bloquearPorDia(any())).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> crear(inicio, 1));
    }

    @Test void usuarioDebeExistirEstarActivoYSerCliente() {
        when(usuarios.bloquearPorId(1L)).thenReturn(Optional.empty());
        assertThrows(AccesoDenegadoException.class, () -> crear(inicio, 1));
        when(usuarios.bloquearPorId(1L)).thenReturn(Optional.of(new Usuario(1L, "cliente", "hash", Rol.CLIENTE, false)));
        assertThrows(AccesoDenegadoException.class, () -> crear(inicio, 1));
        when(usuarios.bloquearPorId(1L)).thenReturn(Optional.of(new Usuario(1L, "admin", "hash", Rol.ADMIN, true)));
        assertThrows(AccesoDenegadoException.class, () -> crear(inicio, 1));
    }

    @Test void validaDisciplinaContraTipoDeCancha() {
        assertThrows(DatoInvalidoException.class, () -> service.crear(1L, 10L, Disciplina.TENIS, inicio, 1));
        assertThrows(DatoInvalidoException.class, () -> service.crear(1L, 10L, null, inicio, 1));
        assertEquals(Disciplina.ECUAVOLEY, service.crear(1L, 10L, Disciplina.ECUAVOLEY, inicio, 1).getDisciplina());
    }

    @Test void limiteDiarioAcumulaHorasDeOtrasCanchas() {
        when(reservas.confirmadasPorResponsableYFecha(1L, inicio.toLocalDate())).thenReturn(List.of(reserva(1L, 20L, inicio.withHour(8), 2)));
        assertThrows(ReglaNegocioException.class, () -> crear(inicio, 1));
        verify(reservas, never()).guardar(any());
    }

    @Test void responsableNoPuedeTenerDosReservasSimultaneasEnCanchasDistintas() {
        when(reservas.confirmadasPorResponsableYFecha(1L, inicio.toLocalDate())).thenReturn(List.of(reserva(1L, 20L, inicio, 1)));
        var error = assertThrows(ReglaNegocioException.class, () -> crear(inicio, 1));
        assertTrue(error.getMessage().contains("responsable ya tiene"));
    }

    @Test void mismaCanchaBloqueaAunqueLaDisciplinaSeaDiferente() {
        when(reservas.confirmadasPorCanchaYFecha(10L, inicio.toLocalDate())).thenReturn(List.of(reserva(2L, 10L, inicio, 1)));
        assertThrows(ReglaNegocioException.class, () -> service.crear(1L, 10L, Disciplina.ECUAVOLEY, inicio, 1));
    }

    @Test void permiteReservasConsecutivasHastaDosHoras() {
        var anterior = reserva(1L, 10L, inicio.minusHours(1), 1);
        when(reservas.confirmadasPorResponsableYFecha(1L, inicio.toLocalDate())).thenReturn(List.of(anterior));
        when(reservas.confirmadasPorCanchaYFecha(10L, inicio.toLocalDate())).thenReturn(List.of(anterior));
        assertEquals(inicio, crear(inicio, 1).getInicio());
    }

    @Test void revalidaElRelojDespuesDeEsperarBloqueos() {
        Clock cambiante = mock(Clock.class);
        when(cambiante.getZone()).thenReturn(ZoneId.of("America/Guayaquil"));
        when(cambiante.instant()).thenReturn(Instant.parse("2030-01-14T14:59:00Z"), Instant.parse("2030-01-14T15:01:00Z"));
        var otroServicio = new ReservaService(reservas, usuarios, canchas, horarios, cambiante, utileria);
        assertThrows(DatoInvalidoException.class, () -> otroServicio.crear(1L, 10L, Disciplina.VOLEY, inicio, 1));
        verify(reservas, never()).guardar(any());
    }

    @Test void cancelarConservaRegistroAuditoriaYEsIdempotente() {
        var original = reserva(1L, 10L, inicio, 1);
        prepararCancelacion(original);
        var cancelada = service.cancelar(1L, 99L);
        assertEquals(original.getId(), cancelada.getId());
        assertEquals(original.getCreado(), cancelada.getCreado());
        assertEquals(EstadoReserva.CANCELADA, cancelada.getEstado());
        when(reservas.bloquearPorId(99L)).thenReturn(Optional.of(cancelada));
        clearInvocations(reservas);
        assertSame(cancelada, service.cancelar(1L, 99L));
        verify(reservas, never()).guardar(any());
    }

    @Test void noCancelaReservaQueYaComenzo() {
        prepararCancelacion(reserva(1L, 10L, LocalDateTime.now(clock), 1));
        assertThrows(ReglaNegocioException.class, () -> service.cancelar(1L, 99L));
        verify(reservas, never()).guardar(any());
    }

    @Test void consultasYCancelacionesAjenasSeRechazanTambienEnElCasoDeUso() {
        when(reservas.buscarPorId(99L)).thenReturn(Optional.of(reserva(2L, 10L, inicio, 1)));
        when(reservas.referenciaPorId(99L)).thenReturn(Optional.of(new ReferenciaReserva(2L, 10L, inicio.toLocalDate())));
        assertThrows(AccesoDenegadoException.class, () -> service.consultar(1L, 99L));
        assertThrows(AccesoDenegadoException.class, () -> service.cancelar(1L, 99L));
        assertThrows(AccesoDenegadoException.class, () -> service.listarTodas(1L));
    }

    @Test void adminPuedeConsultarTodasPeroNoCancelarUnaAjena() {
        when(usuarios.buscarPorId(2L)).thenReturn(Optional.of(new Usuario(2L, "admin", "hash", Rol.ADMIN, true)));
        var reserva = reserva(1L, 10L, inicio, 1);
        when(reservas.buscarPorId(99L)).thenReturn(Optional.of(reserva));
        when(reservas.listar()).thenReturn(List.of(reserva));
        when(reservas.referenciaPorId(99L)).thenReturn(Optional.of(new ReferenciaReserva(1L, 10L, inicio.toLocalDate())));
        assertSame(reserva, service.consultar(2L, 99L));
        assertEquals(1, service.listarTodas(2L).size());
        assertThrows(AccesoDenegadoException.class, () -> service.cancelar(2L, 99L));
        assertThrows(RecursoNoEncontradoException.class, () -> service.consultar(2L, 88L));
    }

    private Reserva crear(LocalDateTime hora, int duracion) { return service.crear(1L, 10L, Disciplina.VOLEY, hora, duracion); }

    @Test void cancelacionRepetidaSigueSiendoIdempotenteSiAdminEliminoElHorario() {
        var cancelada = reserva(1L, 10L, inicio, 1).cancelar(LocalDateTime.now(clock));
        prepararCancelacion(cancelada);
        when(horarios.bloquearPorDia(DayOfWeek.MONDAY)).thenReturn(Optional.empty());
        assertSame(cancelada, service.cancelar(1L, 99L));
        verify(reservas, never()).guardar(any());
    }
    private Reserva reserva(Long responsable, Long cancha, LocalDateTime hora, int duracion) {
        return new Reserva(99L, responsable, cancha, Disciplina.VOLEY, hora, duracion, EstadoReserva.CONFIRMADA,
                LocalDateTime.now(clock), List.of(), LocalDateTime.now(clock), LocalDateTime.now(clock));
    }
    private void prepararCancelacion(Reserva reserva) {
        when(reservas.referenciaPorId(99L)).thenReturn(Optional.of(new ReferenciaReserva(1L, 10L, reserva.getFecha())));
        when(reservas.bloquearPorId(99L)).thenReturn(Optional.of(reserva));
    }
}
