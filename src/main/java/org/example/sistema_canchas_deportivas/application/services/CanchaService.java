package org.example.sistema_canchas_deportivas.application.services;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.sistema_canchas_deportivas.domain.models.DisponibilidadCancha;
import org.example.sistema_canchas_deportivas.application.ports.in.CanchaIn;
import org.example.sistema_canchas_deportivas.application.ports.out.*;
import org.example.sistema_canchas_deportivas.domain.models.*;
import org.example.sistema_canchas_deportivas.domain.exception.*;

@Service
@Transactional(readOnly = true)
public class CanchaService implements CanchaIn {
    private final CanchaOut canchas;
    private final HorarioAtencionOut horarios;
    private final ReservaOut reservas;
    private final Clock clock;

    public CanchaService(CanchaOut canchas, HorarioAtencionOut horarios, ReservaOut reservas, Clock clock) {
        this.canchas = canchas;
        this.horarios = horarios;
        this.reservas = reservas;
        this.clock = clock;
    }

    @Override
    public List<Cancha> listar() {
        return canchas.listar().stream().sorted(Comparator.comparing(Cancha::getId)).toList();
    }

    @Override
    public Cancha buscarPorId(Long id) {
        validarId(id);
        return canchas.buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("Cancha no encontrada."));
    }

    @Override
    @Transactional
    public Cancha crear(String nombre, TipoCancha tipo, Superficie superficie, boolean activa) {
        Cancha nueva = Cancha.nuevo(nombre, tipo, superficie, activa);
        if (canchas.existePorNombre(nueva.getNombre())) throw new ReglaNegocioException("El nombre de cancha ya existe.");
        return canchas.guardar(nueva);
    }

    @Override
    @Transactional
    public Cancha actualizar(Long id, String nombre, TipoCancha tipo, Superficie superficie, boolean activa) {
        Cancha actual = bloquear(id);
        return guardarCambio(actual, actual.actualizar(nombre, tipo, superficie, activa));
    }

    @Override
    @Transactional
    public Cancha cambiarEstado(Long id, boolean activa) {
        Cancha actual = bloquear(id);
        return guardarCambio(actual, actual.actualizar(actual.getNombre(), actual.getTipo(), actual.getSuperficie(), activa));
    }

    private Cancha guardarCambio(Cancha actual, Cancha nueva) {
        if (canchas.existePorNombreExcluyendoId(nueva.getNombre(), actual.getId())) {
            throw new ReglaNegocioException("El nombre de cancha ya existe.");
        }
        if (actual.getTipo() != nueva.getTipo() && reservas.existeHistorialCancha(actual.getId())) {
            throw new ReglaNegocioException("No se puede cambiar el tipo de una cancha con historial de reservas.");
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        if (!nueva.isActiva() && reservas.ocupacionesConfirmadas(ahora.toLocalDate(), null).stream()
                .anyMatch(r -> r.canchaId().equals(actual.getId()) && r.fin().isAfter(ahora))) {
            throw new ReglaNegocioException("La cancha tiene reservas confirmadas pendientes; resuélvelas antes de desactivarla.");
        }
        return canchas.guardar(nueva);
    }

    private Cancha bloquear(Long id) {
        validarId(id);
        return canchas.bloquearPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("Cancha no encontrada."));
    }

    private void validarId(Long id) {
        if (id == null || id <= 0) throw new DatoInvalidoException("El ID debe ser positivo.");
    }

    @Override
    public List<DisponibilidadCancha> disponibilidad(LocalDate fecha, Disciplina disciplina, int duracion) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        if (fecha == null || fecha.isBefore(ahora.toLocalDate()) || fecha.getYear() > 9999
                || disciplina == null || (duracion != 1 && duracion != 2)) {
            throw new DatoInvalidoException("Indica una fecha actual o futura válida, disciplina y duración de 1 o 2 horas.");
        }
        HorarioAtencion horario = horarios.buscarPorDia(fecha.getDayOfWeek())
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay horario configurado para ese día."));
        var ocupaciones = reservas.ocupacionesConfirmadas(fecha, fecha);
        LocalDateTime apertura = fecha.atTime(horario.getApertura());
        LocalDateTime primerInicio = apertura.truncatedTo(ChronoUnit.HOURS);
        if (primerInicio.isBefore(apertura)) primerInicio = primerInicio.plusHours(1);
        LocalDateTime cierre = fecha.atTime(horario.getCierre());
        List<DisponibilidadCancha> resultado = new ArrayList<>();
        for (Cancha cancha : listar()) {
            if (!cancha.isActiva() || !cancha.admite(disciplina)) continue;
            List<DisponibilidadCancha.Bloque> bloques = new ArrayList<>();
            for (LocalDateTime inicio = primerInicio; !inicio.plusHours(duracion).isAfter(cierre); inicio = inicio.plusHours(1)) {
                LocalDateTime desde = inicio;
                LocalDateTime hasta = inicio.plusHours(duracion);
                if (desde.isBefore(ahora)) continue;
                boolean ocupada = ocupaciones.stream().anyMatch(r ->
                        r.canchaId().equals(cancha.getId()) && r.seSuperpone(desde, hasta));
                if (!ocupada) bloques.add(new DisponibilidadCancha.Bloque(desde.toLocalTime(), hasta.toLocalTime()));
            }
            if (!bloques.isEmpty()) resultado.add(new DisponibilidadCancha(cancha, bloques));
        }
        return List.copyOf(resultado);
    }
    @Override @Transactional
    public void eliminar(Long id) {
        bloquear(id);
        if (reservas.existeHistorialCancha(id)) throw new ReglaNegocioException("La cancha tiene reservas; elimina primero las reservas relacionadas o desactiva la cancha.");
        canchas.eliminar(id);
    }

}

