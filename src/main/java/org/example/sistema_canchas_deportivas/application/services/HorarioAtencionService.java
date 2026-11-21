package org.example.sistema_canchas_deportivas.application.services;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.sistema_canchas_deportivas.application.ports.in.HorarioAtencionIn;
import org.example.sistema_canchas_deportivas.application.ports.out.*;
import org.example.sistema_canchas_deportivas.domain.models.HorarioAtencion;
import org.example.sistema_canchas_deportivas.domain.exception.*;

@Service
@Transactional(readOnly = true)
public class HorarioAtencionService implements HorarioAtencionIn {
    private final HorarioAtencionOut horarios;
    private final ReservaOut reservas;
    private final Clock clock;

    public HorarioAtencionService(HorarioAtencionOut horarios, ReservaOut reservas, Clock clock) {
        this.horarios = horarios;
        this.reservas = reservas;
        this.clock = clock;
    }

    @Override
    public List<HorarioAtencion> listar() {
        return horarios.listar().stream().sorted(Comparator.comparing(HorarioAtencion::getDia)).toList();
    }

    @Override
    @Transactional
    public HorarioAtencion actualizar(DayOfWeek dia, LocalTime apertura, LocalTime cierre) {
        // Valida antes de adquirir el bloqueo. El día se identifica en la ruta y no se reasigna.
        new HorarioAtencion(null, dia, apertura, cierre);
        HorarioAtencion actual = horarios.bloquearPorDia(dia)
                .orElseThrow(() -> new RecursoNoEncontradoException("Horario no encontrado."));
        HorarioAtencion nuevo = actual.actualizar(apertura, cierre);
        LocalDateTime ahora = LocalDateTime.now(clock);
        boolean conflicto = reservas.ocupacionesConfirmadas(ahora.toLocalDate(), null).stream()
                .filter(r -> r.fecha().getDayOfWeek() == dia && r.fin().isAfter(ahora))
                .anyMatch(r -> !nuevo.contiene(r.inicio(), r.fin()));
        if (conflicto) throw new ReglaNegocioException("El horario propuesto deja reservas confirmadas fuera de atención.");
        return horarios.guardar(nuevo);
    }
    @Override public HorarioAtencion buscarPorDia(DayOfWeek dia) {
        if (dia == null) throw new DatoInvalidoException("El día es obligatorio.");
        return horarios.buscarPorDia(dia).orElseThrow(() -> new RecursoNoEncontradoException("Horario no encontrado."));
    }
    @Override @Transactional
    public HorarioAtencion crear(DayOfWeek dia, LocalTime apertura, LocalTime cierre) {
        var nuevo = HorarioAtencion.nuevo(dia, apertura, cierre);
        if (horarios.existePorDia(dia)) throw new ReglaNegocioException("Ya existe un horario para ese día.");
        return horarios.guardar(nuevo);
    }
    @Override @Transactional
    public void eliminar(DayOfWeek dia) {
        if (dia == null) throw new DatoInvalidoException("El día es obligatorio.");
        var actual = horarios.bloquearPorDia(dia).orElseThrow(() -> new RecursoNoEncontradoException("Horario no encontrado."));
        var ahora = LocalDateTime.now(clock);
        if (reservas.ocupacionesConfirmadas(ahora.toLocalDate(), null).stream()
                .anyMatch(r -> r.fecha().getDayOfWeek() == dia && r.fin().isAfter(ahora))) {
            throw new ReglaNegocioException("El día tiene reservas confirmadas pendientes.");
        }
        horarios.eliminar(actual.getId());
    }

}

