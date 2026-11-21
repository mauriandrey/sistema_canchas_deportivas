package org.example.sistema_canchas_deportivas.infraestructure.auditoria;

import java.time.*;
import java.util.List;
import jakarta.persistence.EntityListeners;
import org.junit.jupiter.api.Test;
import org.example.sistema_canchas_deportivas.domain.models.*;
import org.example.sistema_canchas_deportivas.infraestructure.entities.*;
import org.example.sistema_canchas_deportivas.infraestructure.mappers.CanchaMapper;
import static org.junit.jupiter.api.Assertions.*;

class AuditoriaTest {
    @Test void lasSieteEntidadesRegistranCreacionYModificacionConLaZonaConfigurada() {
        Clock clock = Clock.fixed(Instant.parse("2030-01-07T15:00:00.123456789Z"), ZoneId.of("America/Guayaquil"));
        var listener = new AuditoriaListener(clock);
        List<RegistroAuditable> entidades = List.of(
                new UsuarioEntity(null, "usuario", "hash", Rol.CLIENTE, true),
                new CanchaEntity(null, "Cancha", TipoCancha.TENIS, null, true),
                new HorarioAtencionEntity(null, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0)),
                new UtileriaEntity(null, "Raquetas", TipoImplemento.RAQUETA, 0, true),
                new ReservaEntity(null, null, null, Disciplina.TENIS, LocalDate.of(2030, 1, 7),
                        LocalTime.of(10, 0), 1, EstadoReserva.CONFIRMADA, LocalDateTime.of(2030, 1, 7, 9, 0)),
                new ReservaUtileriaEntity(null, null, null, 1), new ClienteEntity());
        LocalDateTime esperado = LocalDateTime.of(2030, 1, 7, 10, 0, 0, 123456000);
        for (RegistroAuditable entity : entidades) {
            assertArrayEquals(new Class<?>[]{AuditoriaListener.class},
                    entity.getClass().getAnnotation(EntityListeners.class).value());
            listener.alCrear(entity);
            assertEquals(esperado, entity.getCreado());
            assertEquals(esperado, entity.getModificado());
            new AuditoriaListener(Clock.offset(clock, Duration.ofMinutes(5))).alModificar(entity);
            assertEquals(esperado, entity.getCreado());
            assertEquals(esperado.plusMinutes(5), entity.getModificado());
        }
    }

    @Test void modeloNuevoNoCalculaFechasYActualizacionConservaAuditoria() {
        assertNull(Cancha.nuevo("Tenis", TipoCancha.TENIS, null, true).getCreado());
        assertNull(Usuario.nuevo("cliente", "hash", Rol.CLIENTE).getModificado());
        LocalDateTime creado = LocalDateTime.of(2030, 1, 7, 10, 0);
        var cancha = new Cancha(1L, "Tenis", TipoCancha.TENIS, null, true, creado, creado.plusHours(1));
        var editada = cancha.actualizar("Renombrada", TipoCancha.TENIS, null, false);
        assertEquals(creado, editada.getCreado());
        assertEquals(cancha.getModificado(), editada.getModificado());
        assertEquals("Tenis", cancha.getNombre()); // Se conserva la inmutabilidad existente.
        var horario = new HorarioAtencion(1L, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), creado, creado);
        assertEquals(creado, horario.actualizar(LocalTime.of(9, 0), LocalTime.of(15, 0)).getCreado());
    }

    @Test void mapperDevuelveAuditoriaDelRegistroPeroNoAceptaFechasParaInsertar() {
        var mapper = new CanchaMapper();
        var entity = new CanchaEntity(1L, "Tenis", TipoCancha.TENIS, null, true);
        LocalDateTime creado = LocalDateTime.of(2030, 1, 7, 10, 0);
        entity.alCrear(creado);
        entity.alModificar(creado.plusHours(1));
        var modelo = mapper.toDomain(entity);
        assertEquals(creado, modelo.getCreado());
        assertEquals(creado.plusHours(1), modelo.getModificado());
        var nuevaEntity = mapper.toEntity(modelo);
        assertNull(nuevaEntity.getCreado());
        assertNull(nuevaEntity.getModificado());
    }
}
