package org.example.sistema_canchas_deportivas.infraestructure.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.DayOfWeek;
import org.example.sistema_canchas_deportivas.infraestructure.entities.HorarioAtencionEntity;

public interface HorarioAtencionRepository extends JpaRepository<HorarioAtencionEntity, Long> {
    boolean existsByDia(DayOfWeek dia);
    Optional<HorarioAtencionEntity> findByDia(DayOfWeek dia);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from HorarioAtencionEntity h where h.dia = :dia")
    Optional<HorarioAtencionEntity> bloquearPorDia(DayOfWeek dia);
}

