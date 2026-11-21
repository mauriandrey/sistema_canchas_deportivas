package org.example.sistema_canchas_deportivas.infraestructure.controllers;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.example.sistema_canchas_deportivas.application.ports.in.UsuarioIn;
import org.example.sistema_canchas_deportivas.domain.models.Cliente;
import org.example.sistema_canchas_deportivas.domain.models.Usuario;
import org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos.*;

/** Cuentas y registro; el login permanece exclusivamente en AuthController. */
@RestController
public class UsuarioController {
    private final UsuarioIn usuarios;
    public UsuarioController(UsuarioIn usuarios) { this.usuarios = usuarios; }

    @PostMapping({"/api/auth/registro", "/api/admin/usuarios"})
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody UsuarioRequest request) {
        var usuario = usuarios.registrarCliente(request.username(), request.password(),
                Cliente.nuevo(request.cedula(), request.nombres(), request.apellidos(), request.email(), request.telefono()));
        return ResponseEntity.created(URI.create("/api/admin/usuarios/" + usuario.getId())).body(respuesta(usuario));
    }
    @GetMapping("/api/admin/usuarios")
    public List<UsuarioResponse> listar() { return usuarios.listar().stream().map(this::respuesta).toList(); }
    @GetMapping("/api/admin/usuarios/{id}")
    public UsuarioResponse buscar(@PathVariable Long id) { return respuesta(usuarios.buscarPorId(id)); }
    @PutMapping("/api/admin/usuarios/{id}")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioActualizarRequest request) {
        return respuesta(usuarios.actualizar(id, request.username(), request.password(), request.activo(), request.clienteId()));
    }
    @DeleteMapping("/api/admin/usuarios/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        usuarios.eliminar(id); return ResponseEntity.noContent().build();
    }
    private UsuarioResponse respuesta(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getUsername(), usuario.getRol(), usuario.isActivo(), usuario.getClienteId());
    }
}
