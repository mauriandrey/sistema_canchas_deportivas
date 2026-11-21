package org.example.sistema_canchas_deportivas.application.ports.out;
import java.util.List;
import java.util.Optional;
import org.example.sistema_canchas_deportivas.domain.models.Cliente;
public interface ClienteOut {
    List<Cliente> listar();
    Optional<Cliente> buscarPorId(Long id);
    Cliente guardar(Cliente cliente);
    void eliminar(Long id);
    boolean existePorCedula(String cedula, Long idExcluir);
    boolean existePorEmail(String email, Long idExcluir);
    Optional<Cliente> bloquearPorId(Long id);
}
