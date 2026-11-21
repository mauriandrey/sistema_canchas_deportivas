package org.example.sistema_canchas_deportivas.infraestructure.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.example.sistema_canchas_deportivas.infraestructure.entities.CanchaEntity;

public interface CanchaRepository extends JpaRepository<CanchaEntity, Long> {
    boolean existsByNombre(String nombre);
    boolean existsByNombreAndIdNot(String nombre, Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CanchaEntity c where c.id = :id")
    Optional<CanchaEntity> bloquearPorId(Long id);
}

