package org.example.sistema_canchas_deportivas.application.services;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.example.sistema_canchas_deportivas.domain.models.UtileriaDisponible;
import org.example.sistema_canchas_deportivas.application.ports.in.UtileriaIn;
import org.example.sistema_canchas_deportivas.application.ports.out.*;
import org.example.sistema_canchas_deportivas.domain.models.*;
import org.example.sistema_canchas_deportivas.domain.exception.*;

@Service
@Transactional(readOnly = true)
public class UtileriaService implements UtileriaIn {
    private final UtileriaOut utileria;
    private final HorarioAtencionOut horarios;
    private final Clock clock;

    public UtileriaService(UtileriaOut utileria, HorarioAtencionOut horarios, Clock clock) {
        this.utileria = utileria;
        this.horarios = horarios;
        this.clock = clock;
    }

    @Override
    public List<Utileria> listar() {
        return utileria.listar().stream().sorted(Comparator.comparing(Utileria::getId)).toList();
    }

    @Override
    @Transactional
    public Utileria crear(String nombre, TipoImplemento tipo, int existencias, boolean activa, Set<Disciplina> disciplinas) {
        var nueva = Utileria.nuevo(nombre, tipo, existencias, activa, disciplinas);
        if (utileria.existePorNombre(nueva.getNombre())) throw new ReglaNegocioException("El nombre de utilería ya existe.");
        return utileria.guardar(nueva);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 15)
    public Utileria actualizar(Long id, String nombre, TipoImplemento tipo, int existencias, boolean activa, Set<Disciplina> disciplinas) {
        var actual = bloquear(id);
        return guardarCambio(actual, actual.actualizar(nombre, tipo, existencias, activa, disciplinas));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 15)
    public Utileria cambiarExistencias(Long id, int existencias) {
        var actual = bloquear(id);
        return guardarCambio(actual, actual.actualizar(actual.getNombre(), actual.getTipo(), existencias,
                actual.isActiva(), actual.getDisciplinasCompatibles()));
    }

    private Utileria bloquear(Long id) {
        if (id == null || id <= 0) throw new DatoInvalidoException("El ID debe ser positivo.");
        return utileria.bloquearPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("Utilería no encontrada."));
    }

    private Utileria guardarCambio(Utileria actual, Utileria nueva) {
        if (utileria.existePorNombreExcluyendoId(nueva.getNombre(), actual.getId())) {
            throw new ReglaNegocioException("El nombre de utilería ya existe.");
        }
        if (actual.getTipo() != nueva.getTipo() && utileria.tieneHistorial(actual.getId())) {
            throw new ReglaNegocioException("No se puede cambiar el tipo de un implemento con historial.");
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        var pendientes = utileria.compromisosConfirmados(actual.getId(), ahora.toLocalDate(), null).stream()
                .filter(c -> c.fin().isAfter(ahora)).toList();
        if (!pendientes.isEmpty() && (!nueva.isActiva() || pendientes.stream().anyMatch(c -> !nueva.admite(c.disciplina())))) {
            throw new ReglaNegocioException("El cambio dejaría reservas confirmadas sin un implemento compatible y activo.");
        }
        if (CompromisoUtileria.maximoSimultaneo(pendientes, ahora, null) > nueva.getExistencias()) {
            throw new ReglaNegocioException("Las existencias no cubren los compromisos simultáneos pendientes.");
        }
        return utileria.guardar(nueva);
    }

    @Override
    public List<UtileriaDisponible> disponibilidad(LocalDateTime inicio, int duracion, Disciplina disciplina) {
        Reserva.validarHorario(inicio, duracion, LocalDateTime.now(clock));
        if (disciplina == null) throw new DatoInvalidoException("La disciplina es obligatoria.");
        var horario = horarios.buscarPorDia(inicio.getDayOfWeek())
                .orElseThrow(() -> new RecursoNoEncontradoException("Horario no configurado."));
        LocalDateTime fin = inicio.plusHours(duracion);
        if (!horario.contiene(inicio, fin)) throw new ReglaNegocioException("El intervalo está fuera del horario de atención.");
        List<UtileriaDisponible> resultado = new ArrayList<>();
        for (var item : listar()) {
            if (!item.isActiva() || !item.admite(disciplina)) continue;
            long ocupadas = CompromisoUtileria.maximoSimultaneo(
                    utileria.compromisosConfirmados(item.getId(), inicio.toLocalDate(), inicio.toLocalDate()), inicio, fin);
            int disponibles = (int) Math.max(0L, (long) item.getExistencias() - ocupadas);
            resultado.add(new UtileriaDisponible(item, ocupadas, disponibles));
        }
        return List.copyOf(resultado);
    }
    @Override public Utileria buscarPorId(Long id) {
        if (id == null || id <= 0) throw new DatoInvalidoException("El ID debe ser positivo.");
        return utileria.buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("Utilería no encontrada."));
    }
    @Override @Transactional
    public void eliminar(Long id) {
        bloquear(id);
        if (utileria.tieneHistorial(id)) throw new ReglaNegocioException("El implemento tiene detalles de reservas; elimina primero esas relaciones o desactívalo.");
        utileria.eliminar(id);
    }

}

