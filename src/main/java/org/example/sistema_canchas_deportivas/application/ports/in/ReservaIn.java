package org.example.sistema_canchas_deportivas.application.ports.in;

import org.example.sistema_canchas_deportivas.domain.models.SolicitudUtileria;
import java.time.LocalDateTime;
import java.util.List;
import org.example.sistema_canchas_deportivas.domain.models.*;

public interface ReservaIn {
    Reserva crear(Long actorId, Long canchaId, Disciplina disciplina, LocalDateTime inicio, int duracion);
    Reserva crear(Long actorId, Long canchaId, Disciplina disciplina, LocalDateTime inicio, int duracion,
                  List<SolicitudUtileria> utileria);
    List<Reserva> misReservas(Long actorId);
    Reserva consultar(Long actorId, Long reservaId);
    Reserva cancelar(Long actorId, Long reservaId);
    List<Reserva> listarTodas(Long actorId);
    Reserva crearAdmin(Long actorId, Long responsableId, Long canchaId, Disciplina disciplina, LocalDateTime inicio, int duracion, List<SolicitudUtileria> utileria);
    Reserva actualizarAdmin(Long actorId, Long id, Long responsableId, Long canchaId, Disciplina disciplina, LocalDateTime inicio, int duracion, EstadoReserva estado, List<SolicitudUtileria> utileria);
    void eliminarAdmin(Long actorId, Long id);
    Reserva cancelarAdmin(Long actorId, Long id);
    Reserva cambiarDetalleAdmin(Long actorId, Long id, Long utileriaId, Integer cantidad, boolean crear);

}

