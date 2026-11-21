package org.example.sistema_canchas_deportivas.infraestructure.mappers;

import org.springframework.stereotype.Component;
import org.example.sistema_canchas_deportivas.domain.models.Usuario;
import org.example.sistema_canchas_deportivas.infraestructure.entities.UsuarioEntity;

@Component
public class UsuarioMapper {
    public UsuarioEntity toEntity(Usuario usuario) {
        return new UsuarioEntity(usuario.getId(), usuario.getUsername(), usuario.getPasswordHash(),
                usuario.getRol(), usuario.isActivo());
    }

    public Usuario toDomain(UsuarioEntity entity) {
        return new Usuario(entity.getId(), entity.getUsername(), entity.getPasswordHash(),
                entity.getRol(), entity.isActivo(), entity.getCreado(), entity.getModificado(),
                entity.getCliente() == null ? null : entity.getCliente().getId());
    }
}

