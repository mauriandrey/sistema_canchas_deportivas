package org.example.sistema_canchas_deportivas.infraestructure.adapters;

import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;
import org.example.sistema_canchas_deportivas.application.ports.out.UsuarioOut;
import org.example.sistema_canchas_deportivas.domain.models.Usuario;
import org.example.sistema_canchas_deportivas.infraestructure.mappers.UsuarioMapper;
import org.example.sistema_canchas_deportivas.infraestructure.repositories.UsuarioRepository;

@Repository
@Transactional(readOnly = true)
public class UsuarioAdapter implements UsuarioOut {
    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final org.example.sistema_canchas_deportivas.infraestructure.repositories.ClienteRepository clientes;

    public UsuarioAdapter(UsuarioRepository usuarioRepository, UsuarioMapper usuarioMapper,
            org.example.sistema_canchas_deportivas.infraestructure.repositories.ClienteRepository clientes) {
        this.clientes = clientes;
        this.usuarioRepository = usuarioRepository;
        this.usuarioMapper = usuarioMapper;
    }

    @Override
    public boolean existePorUsername(String username) {
        return usuarioRepository.existsByUsername(username);
    }

    @Override
    @Transactional
    public void guardar(Usuario usuario) {
        var entity = usuarioMapper.toEntity(usuario);
        if (usuario.getClienteId() != null) entity.setCliente(clientes.getReferenceById(usuario.getClienteId()));
        if (usuario.getId() != null) {
            var existente = usuarioRepository.findById(usuario.getId()).orElseThrow(() ->
                    new org.example.sistema_canchas_deportivas.domain.exception.RecursoNoEncontradoException("Usuario no encontrado."));
            entity.conservarAuditoriaDe(existente);
        }
        usuarioRepository.saveAndFlush(entity);
    }
    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id).map(usuarioMapper::toDomain);
    }

    @Override
    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username).map(usuarioMapper::toDomain);
    }
    @Override
    @Transactional
    public Optional<Usuario> bloquearPorId(Long id) {
        return usuarioRepository.bloquearPorId(id).map(usuarioMapper::toDomain);
    }
    @Override public java.util.List<Usuario> listar() { return usuarioRepository.findAll().stream().map(usuarioMapper::toDomain).toList(); }
    @Override public boolean existePorClienteId(Long id) { return usuarioRepository.existsByClienteId(id); }
    @Override public boolean existePorUsernameExcluyendoId(String username, Long id) { return usuarioRepository.existsByUsernameAndIdNot(username, id); }
    @Override @Transactional public void eliminar(Long id) { usuarioRepository.deleteById(id); usuarioRepository.flush(); }

}

