package org.example.sistema_canchas_deportivas.application.ports.in;

import org.example.sistema_canchas_deportivas.domain.models.Usuario;

public interface UsuarioIn {
    /** Recibe una contraseña sin codificar. Devuelve true únicamente cuando crea el administrador. */
    boolean crearAdministradorInicial(String password);
    Usuario registrarCliente(String username, String password, org.example.sistema_canchas_deportivas.domain.models.Cliente datos);
    java.util.List<Usuario> listar();
    Usuario buscarPorId(Long id);
    Usuario actualizar(Long id, String username, String password, boolean activo, Long clienteId);
    void eliminar(Long id);
}

