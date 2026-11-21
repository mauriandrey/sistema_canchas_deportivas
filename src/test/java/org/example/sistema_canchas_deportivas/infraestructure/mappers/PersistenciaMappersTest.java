package org.example.sistema_canchas_deportivas.infraestructure.mappers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.example.sistema_canchas_deportivas.domain.models.*;
import org.example.sistema_canchas_deportivas.infraestructure.entities.*;

import static org.junit.jupiter.api.Assertions.*;

class PersistenciaMappersTest {
    @Test
    void reservaConservaFechasEstadoYDetalleSinCiclosEnElDominio() {
        var fecha = LocalDateTime.of(2026, 12, 21, 10, 0);
        var creada = LocalDateTime.of(2026, 11, 15, 10, 0);
        var modelo = new Reserva(5L, 1L, 2L, Disciplina.FUTBOL_11, fecha, 2,
                EstadoReserva.CONFIRMADA, creada, List.of(new ReservaUtileria(6L, 5L, 3L, 1)));
        var usuario = new UsuarioEntity(1L, "admin", "hash", Rol.ADMIN, true);
        var cancha = new CanchaEntity(2L, "Cancha", TipoCancha.FUTBOL_11, Superficie.NATURAL, true);
        var implemento = new UtileriaEntity(3L, "Balón", TipoImplemento.BALON, 0, true);
        var mapper = new ReservaMapper(new ReservaUtileriaMapper());
        var entity = mapper.toEntity(modelo, usuario, cancha, Map.of(3L, implemento));
        assertSame(entity, entity.getUtileria().getFirst().getReserva());
        assertEquals(fecha.plusHours(2), entity.getFin());
        var resultado = mapper.toDomain(entity);
        assertEquals(modelo.getFecha(), resultado.getFecha());
        assertEquals(modelo.getHoraInicio(), resultado.getHoraInicio());
        assertEquals(modelo.getHoraFin(), resultado.getHoraFin());
        assertEquals(creada, resultado.getFechaCreacion());
        assertEquals(EstadoReserva.CONFIRMADA, resultado.getEstado());
        assertEquals(5L, resultado.getUtileria().getFirst().getReservaId());
        assertEquals(3L, resultado.getUtileria().getFirst().getUtileriaId());
        assertEquals(1, resultado.getUtileria().getFirst().getCantidad());
    }

    @Test
    void utileriaConservaCompatibilidadesSinCompartirColeccionesMutables() {
        var modelo = new Utileria(1L, "Chalecos", TipoImplemento.CHALECO, 0, true,
                Set.of(Disciplina.FUTBOL_11, Disciplina.FUTBOL_6));
        var mapper = new UtileriaMapper();
        var entity = mapper.toEntity(modelo);
        var resultado = mapper.toDomain(entity);
        entity.definirDisciplinas(Set.of(Disciplina.BASQUET));
        assertEquals(modelo.getDisciplinasCompatibles(), resultado.getDisciplinasCompatibles());
        assertThrows(UnsupportedOperationException.class, () -> resultado.getDisciplinasCompatibles().clear());
    }
}
