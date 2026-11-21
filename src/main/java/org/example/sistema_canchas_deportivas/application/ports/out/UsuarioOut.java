package org.example.sistema_canchas_deportivas.application.ports.out;

import java.util.Optional;
import org.example.sistema_canchas_deportivas.domain.models.Usuario;

public interface UsuarioOut {
    boolean existePorUsername(String username);
    void guardar(Usuario usuario);
    Optional<Usuario> buscarPorId(Long id);
    Optional<Usuario> buscarPorUsername(String username);
    Optional<Usuario> bloquearPorId(Long id);
    java.util.List<Usuario> listar();
    boolean existePorClienteId(Long clienteId);
    boolean existePorUsernameExcluyendoId(String username, Long id);
    void eliminar(Long id);
}

