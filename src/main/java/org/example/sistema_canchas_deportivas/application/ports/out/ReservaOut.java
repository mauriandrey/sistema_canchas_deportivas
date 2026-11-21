package org.example.sistema_canchas_deportivas.application.ports.out;

import org.example.sistema_canchas_deportivas.domain.models.ReferenciaReserva;
import java.time.LocalDate;
import org.example.sistema_canchas_deportivas.domain.models.OcupacionCancha;
import java.util.List;
import java.util.Optional;
import org.example.sistema_canchas_deportivas.domain.models.Reserva;

public interface ReservaOut {
    Reserva guardar(Reserva reserva);
    Optional<Reserva> buscarPorId(Long id);
    List<Reserva> listar();
    boolean existeHistorialCancha(Long canchaId);
    // Hasta inclusivo; null consulta todas las fechas desde el límite inferior.
    List<OcupacionCancha> ocupacionesConfirmadas(
            LocalDate desde, LocalDate hasta);
    List<Reserva> listarPorResponsable(Long responsableId);
    List<Reserva> confirmadasPorResponsableYFecha(Long responsableId, LocalDate fecha);
    List<Reserva> confirmadasPorCanchaYFecha(Long canchaId, LocalDate fecha);
    Optional<ReferenciaReserva> referenciaPorId(Long id);
    Optional<Reserva> bloquearPorId(Long id);
    List<Long> utileriaIdsPorReserva(Long id);
    void eliminar(Long id);
}

