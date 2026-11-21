package org.example.sistema_canchas_deportivas.domain.models;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.example.sistema_canchas_deportivas.domain.exception.*;
import static org.junit.jupiter.api.Assertions.*;

/** Reglas de negocio sin Spring, mocks ni persistencia. */
class ReglasDominioTest {
    private final LocalDateTime ahora = LocalDateTime.of(2030, 1, 7, 8, 0);
    private Reserva reserva(int hora, int duracion) {
        return Reserva.nueva(1L, 2L, Disciplina.TENIS, ahora.withHour(hora), duracion, ahora);
    }

    @Test void intervalosConsecutivosNoSeSuperponenPeroCrucesSi() {
        assertFalse(reserva(10, 1).seSuperpone(reserva(11, 1)));
        assertFalse(reserva(11, 1).seSuperpone(reserva(10, 1)));
        assertTrue(reserva(10, 2).seSuperpone(reserva(11, 2)));
        assertTrue(reserva(10, 1).seSuperpone(reserva(10, 1)));
    }

    @Test void reservaRechazaPasadoHoraFraccionadaYDuracionFueraDeRango() {
        assertThrows(DatoInvalidoException.class, () -> reserva(7, 1));
        for (int duracion : List.of(-1, 0, 3)) {
            assertThrows(DatoInvalidoException.class, () -> reserva(10, duracion));
        }
        for (LocalDateTime inicio : List.of(ahora.withMinute(30), ahora.withSecond(1), ahora.withNano(1))) {
            assertThrows(DatoInvalidoException.class, () -> Reserva.validarHorario(inicio, 1, ahora));
        }
    }

    @Test void cancelacionConservaIdentidadAuditoriaYDetallesSinModificarOriginal() {
        var detalle = ReservaUtileria.nuevo(4L, 2);
        var original = new Reserva(7L, 1L, 2L, Disciplina.TENIS, ahora.withHour(10), 1,
                EstadoReserva.CONFIRMADA, ahora, List.of(detalle), ahora, ahora);
        var cancelada = original.cancelar(ahora);
        assertEquals(EstadoReserva.CONFIRMADA, original.getEstado());
        assertEquals(EstadoReserva.CANCELADA, cancelada.getEstado());
        assertEquals(original.getId(), cancelada.getId());
        assertEquals(ahora, cancelada.getCreado());
        assertEquals(ahora, cancelada.getModificado());
        assertEquals(original.getUtileria(), cancelada.getUtileria());
        assertSame(cancelada, cancelada.cancelar(ahora.plusDays(1)));
    }

    @Test void noCancelaDesdeElInstanteDeInicio() {
        assertThrows(ReglaNegocioException.class, () -> reserva(10, 1).cancelar(ahora.withHour(10)));
        assertThrows(ReglaNegocioException.class, () -> reserva(10, 1).cancelar(ahora.withHour(11)));
    }

    @Test void detallesNoSeDuplicanNiSeModificanDesdeFuera() {
        var detalles = new ArrayList<>(List.of(ReservaUtileria.nuevo(4L, 1)));
        var conImplementos = reserva(10, 1).conUtileria(detalles);
        detalles.clear();
        assertEquals(1, conImplementos.getUtileria().size());
        assertThrows(UnsupportedOperationException.class, () -> conImplementos.getUtileria().clear());
        assertThrows(DatoInvalidoException.class, () -> reserva(10, 1).conUtileria(
                List.of(ReservaUtileria.nuevo(4L, 1), ReservaUtileria.nuevo(4L, 2))));
        assertTrue(reserva(10, 1).getUtileria().isEmpty());
    }

    @Test void cierreEsInclusivoYHorarioNoCruzaElDia() {
        var horario = HorarioAtencion.nuevo(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0));
        assertTrue(horario.contiene(ahora.withHour(14), ahora.withHour(16)));
        assertFalse(horario.contiene(ahora.withHour(15), ahora.withHour(17)));
        assertFalse(horario.contiene(ahora, ahora.plusDays(1)));
        assertThrows(DatoInvalidoException.class, () -> horario.actualizar(LocalTime.of(16, 0), LocalTime.of(8, 0)));
    }

    @Test void canchasCompartidasYSuperficiesSeValidanEnDominio() {
        var cancha = Cancha.nuevo("Compartida", TipoCancha.VOLEY_ECUAVOLEY, null, true);
        assertTrue(cancha.admite(Disciplina.VOLEY));
        assertTrue(cancha.admite(Disciplina.ECUAVOLEY));
        assertFalse(cancha.admite(Disciplina.TENIS));
        assertNull(cancha.getSuperficie());
        assertThrows(DatoInvalidoException.class, () -> Cancha.nuevo("Fútbol", TipoCancha.FUTBOL_11, Superficie.SINTETICA, true));
    }
}
