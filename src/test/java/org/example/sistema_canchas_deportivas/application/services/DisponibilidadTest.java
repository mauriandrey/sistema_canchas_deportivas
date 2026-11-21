package org.example.sistema_canchas_deportivas.application.services;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.example.sistema_canchas_deportivas.domain.models.*;
import org.example.sistema_canchas_deportivas.application.ports.out.*;
import org.example.sistema_canchas_deportivas.domain.models.*;
import org.example.sistema_canchas_deportivas.domain.exception.DatoInvalidoException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DisponibilidadTest {
    private final CanchaOut canchas = mock(CanchaOut.class);
    private final HorarioAtencionOut horarios = mock(HorarioAtencionOut.class);
    private final ReservaOut reservas = mock(ReservaOut.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-16T15:30:00Z"), ZoneId.of("America/Guayaquil"));
    private final CanchaService service = new CanchaService(canchas, horarios, reservas, clock);
    private final LocalDate lunes = LocalDate.of(2026, 11, 23);

    @BeforeEach void preparar() {
        when(canchas.listar()).thenReturn(List.of(new Cancha(1L, "Compartida", TipoCancha.VOLEY_ECUAVOLEY, null, true)));
        when(horarios.buscarPorDia(any())).thenAnswer(inv -> {
            DayOfWeek dia = inv.getArgument(0);
            return Optional.of(new HorarioAtencion(1L, dia, LocalTime.of(8, 0), LocalTime.of(dia.getValue() <= 4 ? 16 : 22, 0)));
        });
    }

    @ParameterizedTest @EnumSource(DayOfWeek.class)
    void respetaAperturaCierreYDuracionesTodosLosDias(DayOfWeek dia) {
        LocalDate fecha = lunes.plusDays(dia.getValue() - 1);
        for (int duracion : List.of(1, 2)) {
            var bloques = service.disponibilidad(fecha, Disciplina.VOLEY, duracion).getFirst().horarios();
            int cierre = dia.getValue() <= 4 ? 16 : 22;
            assertEquals(cierre - 8 - duracion + 1, bloques.size());
            assertEquals(LocalTime.of(8, 0), bloques.getFirst().inicio());
            assertEquals(LocalTime.of(cierre, 0), bloques.getLast().fin());
            assertEquals(LocalTime.of(cierre - duracion, 0), bloques.getLast().inicio());
        }
    }

    @Test void unaReservaDeVoleyBloqueaEcuavoleyEnLaMismaCanchaPeroNoLaContigua() {
        when(canchas.listar()).thenReturn(List.of(
                new Cancha(1L, "Compartida", TipoCancha.VOLEY_ECUAVOLEY, null, true),
                new Cancha(2L, "Otra", TipoCancha.VOLEY_ECUAVOLEY, null, true)));
        when(reservas.ocupacionesConfirmadas(lunes, lunes)).thenReturn(List.of(
                new OcupacionCancha(1L, Disciplina.VOLEY, lunes, LocalTime.of(10, 0), 2)));
        var resultado = service.disponibilidad(lunes, Disciplina.ECUAVOLEY, 1);
        var inicios = resultado.getFirst().horarios().stream().map(DisponibilidadCancha.Bloque::inicio).toList();
        assertFalse(inicios.contains(LocalTime.of(10, 0)));
        assertFalse(inicios.contains(LocalTime.of(11, 0)));
        assertTrue(inicios.contains(LocalTime.of(9, 0)));
        assertTrue(inicios.contains(LocalTime.of(12, 0)));
        assertEquals(8, resultado.getLast().horarios().size());
        var bloquesDosHoras = service.disponibilidad(lunes, Disciplina.ECUAVOLEY, 2).getFirst().horarios();
        assertEquals(List.of(8, 12, 13, 14), bloquesDosHoras.stream().map(b -> b.inicio().getHour()).toList());
    }

    @Test void excluyeInactivasEIncompatibles() {
        when(canchas.listar()).thenReturn(List.of(
                new Cancha(1L, "Inactiva", TipoCancha.VOLEY_ECUAVOLEY, null, false),
                new Cancha(2L, "Tenis", TipoCancha.TENIS, null, true)));
        assertTrue(service.disponibilidad(lunes, Disciplina.VOLEY, 1).isEmpty());
    }

    @Test void usaGuayaquilYOmiteHorasYaTranscurridasDelDiaActual() {
        var bloques = service.disponibilidad(LocalDate.of(2026, 11, 16), Disciplina.VOLEY, 1).getFirst().horarios();
        assertEquals(LocalTime.of(11, 0), bloques.getFirst().inicio()); // 10:30 en Guayaquil, 15:30 UTC.
    }

    @Test void rechazaPasadoDuracionInvalidaYDisciplinaAusente() {
        assertThrows(DatoInvalidoException.class, () -> service.disponibilidad(lunes.minusWeeks(2), Disciplina.VOLEY, 1));
        assertThrows(DatoInvalidoException.class, () -> service.disponibilidad(lunes, Disciplina.VOLEY, 3));
        assertThrows(DatoInvalidoException.class, () -> service.disponibilidad(lunes, null, 1));
    }

    @Test void aperturaConMinutosSeRedondeaHaciaLaSiguienteHoraExacta() {
        when(horarios.buscarPorDia(DayOfWeek.MONDAY)).thenReturn(Optional.of(
                new HorarioAtencion(1L, DayOfWeek.MONDAY, LocalTime.of(8, 30), LocalTime.of(10, 30))));
        var bloques = service.disponibilidad(lunes, Disciplina.VOLEY, 1).getFirst().horarios();
        assertEquals(List.of(new DisponibilidadCancha.Bloque(LocalTime.of(9, 0), LocalTime.of(10, 0))), bloques);
    }

    @Test void noPublicaCanchasSinBloquesDisponibles() {
        when(horarios.buscarPorDia(DayOfWeek.MONDAY)).thenReturn(Optional.of(
                new HorarioAtencion(1L, DayOfWeek.MONDAY, LocalTime.of(8, 30), LocalTime.of(9, 0))));
        assertTrue(service.disponibilidad(lunes, Disciplina.VOLEY, 1).isEmpty());
    }
}
