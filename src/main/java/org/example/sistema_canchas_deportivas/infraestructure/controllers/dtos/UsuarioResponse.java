package org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos;

import org.example.sistema_canchas_deportivas.domain.models.Rol;

public record UsuarioResponse(Long id, String username, Rol rol, boolean activo, Long clienteId) {
}

