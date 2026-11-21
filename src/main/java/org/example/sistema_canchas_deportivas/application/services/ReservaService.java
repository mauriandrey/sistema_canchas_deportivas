package org.example.sistema_canchas_deportivas.application.services;

import org.example.sistema_canchas_deportivas.domain.models.SolicitudUtileria;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.example.sistema_canchas_deportivas.application.ports.in.ReservaIn;
import org.example.sistema_canchas_deportivas.application.ports.out.*;
import org.example.sistema_canchas_deportivas.domain.models.*;
import org.example.sistema_canchas_deportivas.domain.exception.*;

@Service
@Transactional(readOnly = true)
public class ReservaService implements ReservaIn {
    private final ReservaOut reservas;
    private final UsuarioOut usuarios;
    private final CanchaOut canchas;
    private final HorarioAtencionOut horarios;
    private final Clock clock;
    private final UtileriaOut utileria;

    public ReservaService(ReservaOut reservas, UsuarioOut usuarios, CanchaOut canchas,
                          HorarioAtencionOut horarios, Clock clock, UtileriaOut utileria) {
        this.reservas = reservas;
        this.usuarios = usuarios;
        this.canchas = canchas;
        this.horarios = horarios;
        this.clock = clock;
        this.utileria = utileria;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 15)
    public Reserva crear(Long actorId, Long canchaId, Disciplina disciplina, LocalDateTime inicio, int duracion) {
        return crear(actorId, canchaId, disciplina, inicio, duracion, List.of());
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 15)
    public Reserva crear(Long actorId, Long canchaId, Disciplina disciplina, LocalDateTime inicio, int duracion,
                         List<SolicitudUtileria> solicitudes) {
        validarId(actorId);
        validarId(canchaId);
        List<ReservaUtileria> detalles = validarSolicitudes(solicitudes);
        Reserva.validarHorario(inicio, duracion, LocalDateTime.now(clock));
        // Orden único: horario → responsable → cancha. Los bloqueos duran hasta commit/rollback.
        var horario = horarios.bloquearPorDia(inicio.getDayOfWeek())
                .orElseThrow(() -> new RecursoNoEncontradoException("Horario no configurado."));
        var usuario = usuarios.bloquearPorId(actorId).orElseThrow(AccesoDenegadoException::new);
        if (!usuario.isActivo() || usuario.getRol() != Rol.CLIENTE) throw new AccesoDenegadoException();
        var cancha = canchas.bloquearPorId(canchaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cancha no encontrada."));
        // Se vuelve a comprobar el reloj después de esperar los bloqueos.
        Reserva nueva = Reserva.nueva(actorId, canchaId, disciplina, inicio, duracion, LocalDateTime.now(clock));
        if (!cancha.isActiva()) throw new ReglaNegocioException("La cancha está inactiva.");
        if (!cancha.admite(disciplina)) throw new DatoInvalidoException("La disciplina no corresponde a la cancha.");
        if (!horario.contiene(nueva.getInicio(), nueva.getFin())) {
            throw new ReglaNegocioException("La reserva está fuera del horario de atención.");
        }
        var propias = reservas.confirmadasPorResponsableYFecha(actorId, nueva.getFecha());
        if (propias.stream().mapToInt(Reserva::getDuracionHoras).sum() + duracion > 2) {
            throw new ReglaNegocioException("El responsable no puede superar 2 horas acumuladas por día.");
        }
        if (propias.stream().anyMatch(nueva::seSuperpone)) {
            throw new ReglaNegocioException("El responsable ya tiene una reserva superpuesta.");
        }
        if (reservas.confirmadasPorCanchaYFecha(canchaId, nueva.getFecha()).stream().anyMatch(nueva::seSuperpone)) {
            throw new ReglaNegocioException("La cancha ya tiene una reserva superpuesta.");
        }
        // Bloqueo por ID ascendente: todos los implementos se validan antes de guardar la reserva.
        for (var detalle : detalles) {
            var item = utileria.bloquearPorId(detalle.getUtileriaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Utilería no encontrada."));
            item.validarUso(disciplina);
            long comprometidas = CompromisoUtileria.maximoSimultaneo(
                    utileria.compromisosConfirmados(item.getId(), nueva.getFecha(), nueva.getFecha()), nueva.getInicio(), nueva.getFin());
            if (comprometidas + detalle.getCantidad() > item.getExistencias()) {
                throw new ReglaNegocioException("No hay suficientes unidades de " + item.getNombre() + " en ese intervalo.");
            }
        }
        Reserva.validarHorario(inicio, duracion, LocalDateTime.now(clock));
        return reservas.guardar(nueva.conUtileria(detalles));
    }

    @Override
    public List<Reserva> misReservas(Long actorId) {
        usuarioActivo(actorId);
        return reservas.listarPorResponsable(actorId);
    }

    @Override
    public Reserva consultar(Long actorId, Long reservaId) {
        var actor = usuarioActivo(actorId);
        validarId(reservaId);
        var reserva = reservas.buscarPorId(reservaId).orElseThrow(() -> {
            if (actor.getRol() == Rol.ADMIN) return new RecursoNoEncontradoException("Reserva no encontrada.");
            return new AccesoDenegadoException();
        });
        if (actor.getRol() != Rol.ADMIN && !reserva.getResponsableId().equals(actorId)) {
            throw new AccesoDenegadoException();
        }
        return reserva;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 15)
    public Reserva cancelar(Long actorId, Long reservaId) {
        validarId(actorId);
        validarId(reservaId);
        // Proyección, no entidad gestionada: el estado se lee después de obtener los bloqueos.
        var referencia = reservas.referenciaPorId(reservaId).orElseThrow(AccesoDenegadoException::new);
        if (!referencia.responsableId().equals(actorId)) throw new AccesoDenegadoException();
        // Un horario puede haberse eliminado después de cancelar; repetir la cancelación sigue siendo válido.
        horarios.bloquearPorDia(referencia.fecha().getDayOfWeek());
        var actor = usuarios.bloquearPorId(actorId).orElseThrow(AccesoDenegadoException::new);
        if (!actor.isActivo()) throw new AccesoDenegadoException();
        canchas.bloquearPorId(referencia.canchaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cancha no encontrada."));
        for (Long implementoId : reservas.utileriaIdsPorReserva(reservaId).stream().distinct().sorted().toList()) {
            utileria.bloquearPorId(implementoId).orElseThrow(() -> new RecursoNoEncontradoException("Utilería no encontrada."));
        }
        var reserva = reservas.bloquearPorId(reservaId).orElseThrow(AccesoDenegadoException::new);
        if (!reserva.getResponsableId().equals(actorId)) throw new AccesoDenegadoException();
        if (!referencia.equals(new ReferenciaReserva(reserva.getResponsableId(), reserva.getCanchaId(), reserva.getFecha()))) {
            throw new ReglaNegocioException("La reserva cambió durante la operación; vuelve a consultarla.");
        }
        if (reserva.getEstado() == EstadoReserva.CANCELADA) return reserva; // Cancelación idempotente.
        return reservas.guardar(reserva.cancelar(LocalDateTime.now(clock)));
    }

    @Override
    public List<Reserva> listarTodas(Long actorId) {
        if (usuarioActivo(actorId).getRol() != Rol.ADMIN) throw new AccesoDenegadoException();
        return reservas.listar().stream().sorted(Comparator.comparing(Reserva::getInicio).reversed()
                .thenComparing(Reserva::getId, Comparator.reverseOrder())).toList();
    }

    private List<ReservaUtileria> validarSolicitudes(
            List<SolicitudUtileria> solicitudes) {
        if (solicitudes == null) return List.of();
        Map<Long, ReservaUtileria> detalles = new TreeMap<>();
        for (var solicitud : solicitudes) {
            if (solicitud == null) throw new DatoInvalidoException("El detalle de utilería no puede ser null.");
            var detalle = ReservaUtileria.nuevo(solicitud.utileriaId(), solicitud.cantidad());
            if (detalles.putIfAbsent(detalle.getUtileriaId(), detalle) != null) {
                throw new DatoInvalidoException("No se puede repetir un implemento en la reserva.");
            }
        }
        return List.copyOf(detalles.values());
    }

    private Usuario usuarioActivo(Long id) {
        validarId(id);
        var usuario = usuarios.buscarPorId(id).orElseThrow(AccesoDenegadoException::new);
        if (!usuario.isActivo()) throw new AccesoDenegadoException();
        return usuario;
    }

    private void validarId(Long id) {
        if (id == null || id <= 0) throw new DatoInvalidoException("El ID debe ser positivo.");
    }
    private void administrador(Long actorId) {
        if (usuarioActivo(actorId).getRol() != Rol.ADMIN) throw new AccesoDenegadoException();
    }

    @Override @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 15)
    public Reserva crearAdmin(Long actorId, Long responsableId, Long canchaId, Disciplina disciplina,
                              LocalDateTime inicio, int duracion, List<SolicitudUtileria> detalles) {
        administrador(actorId);
        return crear(responsableId, canchaId, disciplina, inicio, duracion, detalles);
    }

    @Override @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 15)
    public Reserva actualizarAdmin(Long actorId, Long id, Long responsableId, Long canchaId, Disciplina disciplina,
                                   LocalDateTime inicio, int duracion, EstadoReserva estado, List<SolicitudUtileria> solicitudes) {
        administrador(actorId); validarId(id); validarId(responsableId); validarId(canchaId);
        Reserva.validarHorario(inicio, duracion, LocalDateTime.now(clock));
        if (estado == null) throw new DatoInvalidoException("El estado es obligatorio.");
        var detalles = validarSolicitudes(solicitudes);
        var ref = referenciaAdmin(id);
        var anteriores = reservas.utileriaIdsPorReserva(id).stream().distinct().sorted().toList();
        var nuevaRef = new ReferenciaReserva(responsableId, canchaId, inicio.toLocalDate());
        bloquearRecursos(ref, nuevaRef, anteriores, detalles.stream().map(ReservaUtileria::getUtileriaId).toList());
        var actual = reservaEstable(id, ref, anteriores);
        var nueva = new Reserva(id, responsableId, canchaId, disciplina, inicio, duracion, estado,
                actual.getFechaCreacion(), conservarDetalles(actual, detalles), actual.getCreado(), actual.getModificado());
        validarCambioAdmin(nueva);
        return reservas.guardar(nueva);
    }

    @Override @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 15)
    public void eliminarAdmin(Long actorId, Long id) {
        administrador(actorId); validarId(id);
        var ref = referenciaAdmin(id);
        var ids = reservas.utileriaIdsPorReserva(id).stream().distinct().sorted().toList();
        bloquearRecursos(ref, ref, ids, List.of());
        reservaEstable(id, ref, ids);
        // Solo los detalles pertenecientes a esta reserva se eliminan por cascada.
        reservas.eliminar(id);
    }

    @Override @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 15)
    public Reserva cambiarDetalleAdmin(Long actorId, Long id, Long utileriaId, Integer cantidad, boolean crear) {
        administrador(actorId); validarId(id); validarId(utileriaId);
        if (cantidad != null) ReservaUtileria.nuevo(utileriaId, cantidad);
        var ref = referenciaAdmin(id);
        var ids = reservas.utileriaIdsPorReserva(id).stream().distinct().sorted().toList();
        bloquearRecursos(ref, ref, ids, List.of(utileriaId));
        var actual = reservaEstable(id, ref, ids);
        var detalles = new ArrayList<>(actual.getUtileria());
        var existente = detalles.stream().filter(d -> d.getUtileriaId().equals(utileriaId)).findFirst();
        if (crear && existente.isPresent()) throw new ReglaNegocioException("El implemento ya pertenece a esta reserva.");
        if (!crear && existente.isEmpty()) throw new RecursoNoEncontradoException("Detalle no encontrado en esta reserva.");
        detalles.removeIf(d -> d.getUtileriaId().equals(utileriaId));
        if (cantidad != null) detalles.add(ReservaUtileria.nuevo(utileriaId, cantidad));
        var nueva = actual.conUtileria(conservarDetalles(actual, detalles));
        validarCambioAdmin(nueva);
        return reservas.guardar(nueva);
    }

    private ReferenciaReserva referenciaAdmin(Long id) {
        return reservas.referenciaPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada."));
    }

    @Override @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 15)
    public Reserva cancelarAdmin(Long actorId, Long id) {
        administrador(actorId); validarId(id);
        var ref = referenciaAdmin(id);
        var ids = reservas.utileriaIdsPorReserva(id).stream().distinct().sorted().toList();
        bloquearRecursos(ref, ref, ids, List.of());
        var actual = reservaEstable(id, ref, ids);
        return reservas.guardar(actual.cancelar(LocalDateTime.now(clock)));
    }

    private void bloquearRecursos(ReferenciaReserva anterior, ReferenciaReserva nueva, List<Long> anteriores, List<Long> nuevos) {
        // Orden total también para cambios entre fechas, responsables o canchas distintos.
        java.util.stream.Stream.of(anterior.fecha().getDayOfWeek(), nueva.fecha().getDayOfWeek()).distinct().sorted()
                .forEach(horarios::bloquearPorDia);
        java.util.stream.Stream.of(anterior.responsableId(), nueva.responsableId()).distinct().sorted().forEach(id ->
                usuarios.bloquearPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("Responsable no encontrado.")));
        java.util.stream.Stream.of(anterior.canchaId(), nueva.canchaId()).distinct().sorted().forEach(id ->
                canchas.bloquearPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("Cancha no encontrada.")));
        java.util.stream.Stream.concat(anteriores.stream(), nuevos.stream()).distinct().sorted().forEach(id ->
                utileria.bloquearPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("Utilería no encontrada.")));
    }

    private Reserva reservaEstable(Long id, ReferenciaReserva ref, List<Long> ids) {
        var actual = reservas.bloquearPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada."));
        if (!ref.equals(new ReferenciaReserva(actual.getResponsableId(), actual.getCanchaId(), actual.getFecha()))
                || !ids.equals(actual.getUtileria().stream().map(ReservaUtileria::getUtileriaId).distinct().sorted().toList())) {
            throw new ReglaNegocioException("La reserva cambió durante la operación; vuelve a consultarla.");
        }
        return actual;
    }

    private List<ReservaUtileria> conservarDetalles(Reserva actual, List<ReservaUtileria> propuestos) {
        return propuestos.stream().sorted(Comparator.comparing(ReservaUtileria::getUtileriaId)).map(nuevo -> {
            var anterior = actual.getUtileria().stream().filter(d -> d.getUtileriaId().equals(nuevo.getUtileriaId())).findFirst();
            return anterior.map(d -> new ReservaUtileria(d.getId(), actual.getId(), d.getUtileriaId(), nuevo.getCantidad(),
                    d.getCreado(), d.getModificado())).orElse(nuevo);
        }).toList();
    }

    private void validarCambioAdmin(Reserva nueva) {
        Reserva.validarHorario(nueva.getInicio(), nueva.getDuracionHoras(), LocalDateTime.now(clock));
        var responsable = usuarios.buscarPorId(nueva.getResponsableId()).orElseThrow(() -> new RecursoNoEncontradoException("Responsable no encontrado."));
        if (!responsable.isActivo() || responsable.getRol() != Rol.CLIENTE) throw new ReglaNegocioException("El responsable debe ser una cuenta CLIENTE activa.");
        var cancha = canchas.buscarPorId(nueva.getCanchaId()).orElseThrow(() -> new RecursoNoEncontradoException("Cancha no encontrada."));
        if (!cancha.isActiva()) throw new ReglaNegocioException("La cancha está inactiva.");
        if (nueva.getDisciplina() == null || !cancha.admite(nueva.getDisciplina())) throw new DatoInvalidoException("La disciplina no corresponde a la cancha.");
        var horario = horarios.buscarPorDia(nueva.getFecha().getDayOfWeek()).orElseThrow(() -> new RecursoNoEncontradoException("Horario no configurado."));
        if (!horario.contiene(nueva.getInicio(), nueva.getFin())) throw new ReglaNegocioException("La reserva está fuera del horario de atención.");
        if (nueva.getEstado() == EstadoReserva.CONFIRMADA) {
            var propias = reservas.confirmadasPorResponsableYFecha(nueva.getResponsableId(), nueva.getFecha()).stream()
                    .filter(r -> !r.getId().equals(nueva.getId())).toList();
            if (propias.stream().mapToInt(Reserva::getDuracionHoras).sum() + nueva.getDuracionHoras() > 2) throw new ReglaNegocioException("El responsable no puede superar 2 horas acumuladas por día.");
            if (propias.stream().anyMatch(nueva::seSuperpone)) throw new ReglaNegocioException("El responsable ya tiene una reserva superpuesta.");
            if (reservas.confirmadasPorCanchaYFecha(nueva.getCanchaId(), nueva.getFecha()).stream()
                    .filter(r -> !r.getId().equals(nueva.getId())).anyMatch(nueva::seSuperpone)) throw new ReglaNegocioException("La cancha ya tiene una reserva superpuesta.");
        }
        for (var detalle : nueva.getUtileria()) {
            var item = utileria.buscarPorId(detalle.getUtileriaId()).orElseThrow(() -> new RecursoNoEncontradoException("Utilería no encontrada."));
            item.validarUso(nueva.getDisciplina());
            if (nueva.getEstado() == EstadoReserva.CONFIRMADA) {
                long ocupadas = CompromisoUtileria.maximoSimultaneo(utileria.compromisosExcluyendo(item.getId(), nueva.getFecha(), nueva.getFecha(), nueva.getId()), nueva.getInicio(), nueva.getFin());
                if (ocupadas + detalle.getCantidad() > item.getExistencias()) throw new ReglaNegocioException("No hay suficientes unidades de " + item.getNombre() + " en ese intervalo.");
            }
        }
    }

}

