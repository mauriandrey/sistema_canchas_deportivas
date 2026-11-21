package org.example.sistema_canchas_deportivas.infraestructure.repositories;

import java.util.Optional;
import org.example.sistema_canchas_deportivas.domain.models.ReferenciaReserva;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.example.sistema_canchas_deportivas.domain.models.EstadoReserva;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.time.LocalDate;
import org.example.sistema_canchas_deportivas.domain.models.OcupacionCancha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.example.sistema_canchas_deportivas.infraestructure.entities.ReservaEntity;

public interface ReservaRepository extends JpaRepository<ReservaEntity, Long> {
    boolean existsByCanchaId(Long canchaId);
    @Query("""
            select new org.example.sistema_canchas_deportivas.domain.models.OcupacionCancha(
                r.cancha.id, r.disciplina, r.fecha, r.horaInicio, r.duracionHoras)
            from ReservaEntity r
            where r.estado = org.example.sistema_canchas_deportivas.domain.models.EstadoReserva.CONFIRMADA
              and r.fecha >= :desde and (:hasta is null or r.fecha <= :hasta)
            order by r.fecha, r.horaInicio, r.cancha.id
            """)
    List<OcupacionCancha> ocupacionesConfirmadas(
            LocalDate desde, LocalDate hasta);
    List<ReservaEntity> findByResponsableIdOrderByFechaDescHoraInicioDescIdDesc(Long responsableId);
    List<ReservaEntity> findByResponsableIdAndFechaAndEstado(Long responsableId, LocalDate fecha,
            EstadoReserva estado);
    List<ReservaEntity> findByCanchaIdAndFechaAndEstado(Long canchaId, LocalDate fecha,
            EstadoReserva estado);

    @Query("""
            select new org.example.sistema_canchas_deportivas.domain.models.ReferenciaReserva(
                r.responsable.id, r.cancha.id, r.fecha) from ReservaEntity r where r.id = :id
            """)
    Optional<ReferenciaReserva> referenciaPorId(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReservaEntity r where r.id = :id")
    Optional<ReservaEntity> bloquearPorId(Long id);
    @Query("select d.utileria.id from ReservaUtileriaEntity d where d.reserva.id = :id order by d.utileria.id")
    List<Long> utileriaIdsPorReserva(Long id);
}

