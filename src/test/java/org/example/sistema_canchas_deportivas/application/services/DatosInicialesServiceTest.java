package org.example.sistema_canchas_deportivas.application.services;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.example.sistema_canchas_deportivas.application.ports.out.*;
import org.example.sistema_canchas_deportivas.domain.models.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DatosInicialesServiceTest {
    private CanchaOut canchas;
    private HorarioAtencionOut horarios;
    private UtileriaOut utileria;
    private DatosInicialesService service;

    @BeforeEach
    void preparar() {
        canchas = mock(CanchaOut.class);
        horarios = mock(HorarioAtencionOut.class);
        utileria = mock(UtileriaOut.class);
        service = new DatosInicialesService(canchas, horarios, utileria);
    }

    @Test
    void creaExactamente17CanchasFisicasConSuperficiesConocidas() {
        service.inicializar();
        var captor = ArgumentCaptor.forClass(Cancha.class);
        verify(canchas, times(17)).guardar(captor.capture());
        var lista = captor.getAllValues();
        assertEquals(17, lista.stream().map(Cancha::getNombre).distinct().count());
        assertEquals(Map.of(TipoCancha.FUTBOL_11, 2L, TipoCancha.FUTBOL_6, 5L,
                        TipoCancha.BASQUET, 4L, TipoCancha.VOLEY_ECUAVOLEY, 4L, TipoCancha.TENIS, 2L),
                lista.stream().collect(Collectors.groupingBy(Cancha::getTipo, Collectors.counting())));
        lista.forEach(cancha -> {
            assertTrue(cancha.isActiva());
            switch (cancha.getTipo()) {
                case FUTBOL_11 -> assertEquals(Superficie.NATURAL, cancha.getSuperficie());
                case FUTBOL_6 -> assertEquals(Superficie.SINTETICA, cancha.getSuperficie());
                default -> assertNull(cancha.getSuperficie());
            }
        });
    }

    @Test
    void creaLosSieteHorariosDelComplejo() {
        service.inicializar();
        var captor = ArgumentCaptor.forClass(HorarioAtencion.class);
        verify(horarios, times(7)).guardar(captor.capture());
        assertEquals(Set.of(DayOfWeek.values()),
                captor.getAllValues().stream().map(HorarioAtencion::getDia).collect(Collectors.toSet()));
        captor.getAllValues().forEach(horario -> {
            assertEquals(LocalTime.of(8, 0), horario.getApertura());
            assertEquals(LocalTime.of(horario.getDia().getValue() <= 4 ? 16 : 22, 0), horario.getCierre());
        });
    }

    @Test
    void creaOchoImplementosSinInventarExistenciasYConCompatibilidades() {
        service.inicializar();
        var captor = ArgumentCaptor.forClass(Utileria.class);
        verify(utileria, times(8)).guardar(captor.capture());
        assertEquals(8, captor.getAllValues().stream().map(Utileria::getNombre).distinct().count());
        captor.getAllValues().forEach(item -> {
            assertEquals(0, item.getExistencias());
            assertTrue(item.isActiva());
            assertFalse(item.getDisciplinasCompatibles().isEmpty());
        });
        Utileria chalecos = captor.getAllValues().stream().filter(u -> u.getTipo() == TipoImplemento.CHALECO).findFirst().orElseThrow();
        assertEquals(Set.of(Disciplina.FUTBOL_11, Disciplina.FUTBOL_6), chalecos.getDisciplinasCompatibles());
        Utileria tubos = captor.getAllValues().stream().filter(u -> u.getTipo() == TipoImplemento.TUBO_PELOTAS).findFirst().orElseThrow();
        assertEquals(Set.of(Disciplina.TENIS), tubos.getDisciplinasCompatibles());
        assertTrue(tubos.getNombre().contains("3 pelotas"));
    }

    @Test
    void registrosExistentesNoSeReinsertanNiSeSobrescriben() {
        when(canchas.existePorNombre(anyString())).thenReturn(true);
        when(horarios.existePorDia(any())).thenReturn(true);
        when(utileria.existePorNombre(anyString())).thenReturn(true);
        service.inicializar();
        service.inicializar();
        verify(canchas, never()).guardar(any());
        verify(horarios, never()).guardar(any());
        verify(utileria, never()).guardar(any());
    }

    @Test
    void catalogoConNombresEditadosNoSeRecrea() {
        when(canchas.listar()).thenReturn(java.util.List.of(
                new Cancha(1L, "Nombre modificado", TipoCancha.TENIS, null, false)));
        service.inicializar();
        verify(canchas, never()).guardar(any());
    }
}
