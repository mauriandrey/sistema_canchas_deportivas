package org.example.sistema_canchas_deportivas.application.services;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.sistema_canchas_deportivas.application.ports.in.UsuarioIn;
import org.example.sistema_canchas_deportivas.application.ports.out.PasswordOut;
import org.example.sistema_canchas_deportivas.application.ports.out.UsuarioOut;
import org.example.sistema_canchas_deportivas.domain.exception.DatoInvalidoException;
import org.example.sistema_canchas_deportivas.domain.exception.ReglaNegocioException;
import org.example.sistema_canchas_deportivas.domain.models.Rol;
import org.example.sistema_canchas_deportivas.domain.models.Usuario;
import org.example.sistema_canchas_deportivas.domain.models.Cliente;
import org.example.sistema_canchas_deportivas.domain.exception.RecursoNoEncontradoException;
import org.example.sistema_canchas_deportivas.application.ports.in.ClienteIn;
import org.example.sistema_canchas_deportivas.application.ports.out.ClienteOut;
import org.example.sistema_canchas_deportivas.application.ports.out.ReservaOut;

@Service
public class UsuarioService implements UsuarioIn {
    private final UsuarioOut usuarioOut;
    private final PasswordOut passwordOut;

    private final ClienteIn clientes;
    private final ClienteOut clientesOut;
    private final ReservaOut reservas;

    public UsuarioService(UsuarioOut usuarioOut, PasswordOut passwordOut, ClienteIn clientes,
                          ClienteOut clientesOut, ReservaOut reservas) {
        this.clientes = clientes; this.clientesOut = clientesOut; this.reservas = reservas;
        this.usuarioOut = usuarioOut;
        this.passwordOut = passwordOut;
    }

    @Override
    @Transactional
    public boolean crearAdministradorInicial(String password) {
        if (usuarioOut.existePorUsername("admin")) {
            return false;
        }
        if (password == null || password.isBlank()) {
            throw new DatoInvalidoException("La contraseña inicial no puede estar vacía.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new DatoInvalidoException("La contraseña inicial no puede superar 72 bytes UTF-8.");
        }
        if (password.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}")) {
            throw new DatoInvalidoException("Se requiere la contraseña original, no un hash BCrypt.");
        }
        // Este es el único punto que solicita el hash; adaptador y mapper reciben el hash ya calculado.
        String hash = passwordOut.encriptar(password);
        usuarioOut.guardar(Usuario.nuevo("admin", hash, Rol.ADMIN));
        return true;
    }

    @Override
    @Transactional
    public Usuario registrarCliente(String username, String password, Cliente datos) {
        if (username == null || !username.matches("[A-Za-z0-9._-]{3,50}")) {
            throw new DatoInvalidoException("El username debe tener entre 3 y 50 letras, números, puntos, guiones o guiones bajos.");
        }
        if (password == null || password.isBlank() || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72
                || password.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}")) {
            throw new DatoInvalidoException("La contraseña debe tener al menos 12 caracteres y como máximo 72 bytes UTF-8; no se aceptan hashes.");
        }
        String normalizado = username.toLowerCase(Locale.ROOT);
        if (usuarioOut.existePorUsername(normalizado)) {
            throw new ReglaNegocioException("El username ya está registrado.");
        }
        if (datos == null || datos.getTelefono() == null || datos.getTelefono().isBlank()) {
            throw new DatoInvalidoException("El perfil del cliente y su teléfono son obligatorios para crear una cuenta.");
        }
        // Ambas inserciones pertenecen a esta transacción; cualquier conflicto revierte las dos.
        Cliente cliente = clientes.crear(datos);
        usuarioOut.guardar(new Usuario(null, normalizado, passwordOut.encriptar(password), Rol.CLIENTE,
                true, null, null, cliente.getId()));
        return usuarioOut.buscarPorUsername(normalizado)
                .orElseThrow(() -> new IllegalStateException("No se pudo recuperar el usuario registrado."));
    }
    @Override @Transactional(readOnly = true)
    public java.util.List<Usuario> listar() { return usuarioOut.listar(); }

    @Override @Transactional(readOnly = true)
    public Usuario buscarPorId(Long id) {
        validarId(id);
        return usuarioOut.buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));
    }

    @Override @Transactional
    public Usuario actualizar(Long id, String username, String password, boolean activo, Long clienteId) {
        validarId(id);
        if (username == null || !username.matches("[A-Za-z0-9._-]{3,50}")) throw new DatoInvalidoException("Username inválido.");
        String normalizado = username.toLowerCase(Locale.ROOT);
        // El perfil se bloquea antes de la cuenta, igual que al eliminar un Cliente.
        if (clienteId != null) {
            validarId(clienteId);
            clientesOut.bloquearPorId(clienteId).orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado."));
        }
        Usuario actual = usuarioOut.bloquearPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));
        if (actual.getRol() == Rol.ADMIN && (!activo || !actual.getUsername().equals(normalizado) || clienteId != null)) {
            throw new ReglaNegocioException("La cuenta administradora conserva su identidad, rol y estado activo.");
        }
        if (actual.getRol() == Rol.CLIENTE && clienteId == null) throw new DatoInvalidoException("La cuenta CLIENTE requiere un perfil.");
        if (actual.getClienteId() != null && !actual.getClienteId().equals(clienteId)) {
            throw new ReglaNegocioException("No se puede transferir una cuenta a otro cliente.");
        }
        if (clienteId != null && !clienteId.equals(actual.getClienteId()) && usuarioOut.existePorClienteId(clienteId)) {
            throw new ReglaNegocioException("El cliente ya tiene una cuenta.");
        }
        if (usuarioOut.existePorUsernameExcluyendoId(normalizado, id)) throw new ReglaNegocioException("El username ya está registrado.");
        String hash = actual.getPasswordHash();
        if (password != null) {
            if (password.isBlank() || password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72
                    || password.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}")) {
                throw new DatoInvalidoException("La contraseña debe tener al menos 12 caracteres y hasta 72 bytes UTF-8; no se aceptan hashes.");
            }
            hash = passwordOut.encriptar(password);
        }
        usuarioOut.guardar(new Usuario(id, normalizado, hash, actual.getRol(), activo,
                actual.getCreado(), actual.getModificado(), clienteId));
        return buscarPorId(id);
    }

    @Override @Transactional
    public void eliminar(Long id) {
        validarId(id);
        Usuario actual = usuarioOut.bloquearPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));
        if (actual.getRol() == Rol.ADMIN) throw new ReglaNegocioException("No se elimina la cuenta administradora del sistema.");
        if (!reservas.listarPorResponsable(id).isEmpty()) throw new ReglaNegocioException("El usuario tiene reservas; elimina primero sus reservas o desactiva su cuenta.");
        usuarioOut.eliminar(id);
    }

    private void validarId(Long id) {
        if (id == null || id <= 0) throw new DatoInvalidoException("El ID debe ser positivo.");
    }

}

