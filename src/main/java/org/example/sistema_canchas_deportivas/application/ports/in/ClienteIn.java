package org.example.sistema_canchas_deportivas.application.ports.in;
import java.util.List;
import org.example.sistema_canchas_deportivas.domain.models.Cliente;
public interface ClienteIn {
    List<Cliente> listar();
    Cliente buscarPorId(Long id);
    Cliente crear(Cliente cliente);
    Cliente actualizar(Long id, Cliente cliente);
    void eliminar(Long id);
}
