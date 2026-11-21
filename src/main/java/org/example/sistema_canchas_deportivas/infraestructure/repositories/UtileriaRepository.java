package org.example.sistema_canchas_deportivas.infraestructure.repositories;

import org.example.sistema_canchas_deportivas.domain.models.CompromisoUtileria;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.example.sistema_canchas_deportivas.infraestructure.entities.UtileriaEntity;

public interface UtileriaRepository extends JpaRepository<UtileriaEntity, Long> {
    boolean existsByNombre(String nombre);
    boolean existsByNombreAndIdNot(String nombre, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UtileriaEntity u where u.id = :id")
    Optional<UtileriaEntity> bloquearPorId(Long id);

    @Query("select count(d) > 0 from ReservaUtileriaEntity d where d.utileria.id = :id")
    boolean tieneHistorial(Long id);

    @Query("""
            select new org.example.sistema_canchas_deportivas.domain.models.CompromisoUtileria(
                r.disciplina, r.fecha, r.horaInicio, r.duracionHoras, d.cantidad)
            from ReservaUtileriaEntity d join d.reserva r
            where d.utileria.id = :id
              and r.estado = org.example.sistema_canchas_deportivas.domain.models.EstadoReserva.CONFIRMADA
              and r.fecha >= :desde and (:hasta is null or r.fecha <= :hasta)
            order by r.fecha, r.horaInicio
            """)
    List<CompromisoUtileria> compromisosConfirmados(
            Long id, LocalDate desde, LocalDate hasta);
    @Query("""
            select new org.example.sistema_canchas_deportivas.domain.models.CompromisoUtileria(
                r.disciplina, r.fecha, r.horaInicio, r.duracionHoras, d.cantidad)
            from ReservaUtileriaEntity d join d.reserva r
            where d.utileria.id = :id and r.id <> :excluir
              and r.estado = org.example.sistema_canchas_deportivas.domain.models.EstadoReserva.CONFIRMADA
              and r.fecha >= :desde and (:hasta is null or r.fecha <= :hasta)
            order by r.fecha, r.horaInicio
            """)
    List<CompromisoUtileria> compromisosExcluyendo(Long id, LocalDate desde, LocalDate hasta, Long excluir);

}

