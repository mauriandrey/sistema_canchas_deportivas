package org.example.sistema_canchas_deportivas.infraestructure.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.example.sistema_canchas_deportivas.infraestructure.entities.ClienteEntity;
public interface ClienteRepository extends JpaRepository<ClienteEntity, Long> {
    boolean existsByCedulaIgnoreCase(String cedula);
    boolean existsByCedulaIgnoreCaseAndIdNot(String cedula, Long id);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from ClienteEntity c where c.id = :id")
    java.util.Optional<ClienteEntity> bloquearPorId(Long id);
}
