package org.example.sistema_canchas_deportivas.infraestructure.repositories;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.example.sistema_canchas_deportivas.infraestructure.entities.UsuarioEntity;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {
    boolean existsByUsername(String username);
    Optional<UsuarioEntity> findByUsername(String username);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UsuarioEntity u where u.id = :id")
    Optional<UsuarioEntity> bloquearPorId(Long id);
    boolean existsByClienteId(Long clienteId);
    boolean existsByUsernameAndIdNot(String username, Long id);
}

