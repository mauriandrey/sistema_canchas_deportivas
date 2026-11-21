package org.example.sistema_canchas_deportivas.infraestructure.adapters;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.example.sistema_canchas_deportivas.application.ports.out.ClienteOut;
import org.example.sistema_canchas_deportivas.domain.models.Cliente;
import org.example.sistema_canchas_deportivas.domain.exception.RecursoNoEncontradoException;
import org.example.sistema_canchas_deportivas.infraestructure.entities.ClienteEntity;
import org.example.sistema_canchas_deportivas.infraestructure.mappers.ClienteMapper;
import org.example.sistema_canchas_deportivas.infraestructure.repositories.ClienteRepository;

@Component
@Transactional(readOnly = true)
public class ClienteAdapter implements ClienteOut {
    private final ClienteRepository repository;
    public ClienteAdapter(ClienteRepository repository) { this.repository = repository; }
    @Override public List<Cliente> listar() { return repository.findAll().stream().map(ClienteMapper::toDomain).toList(); }
    @Override public Optional<Cliente> buscarPorId(Long id) { return repository.findById(id).map(ClienteMapper::toDomain); }

    @Override @Transactional
    public Cliente guardar(Cliente cliente) {
        ClienteEntity entity = cliente.getId() == null ? new ClienteEntity() : repository.findById(cliente.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado."));
        // Se modifica la entidad gestionada; ID y auditoría nunca se copian desde la solicitud.
        ClienteMapper.toEntity(cliente, entity);
        return ClienteMapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override @Transactional
    public void eliminar(Long id) { repository.deleteById(id); repository.flush(); }
    @Override public boolean existePorCedula(String cedula, Long idExcluir) {
        return idExcluir == null ? repository.existsByCedulaIgnoreCase(cedula)
                : repository.existsByCedulaIgnoreCaseAndIdNot(cedula, idExcluir);
    }
    @Override public boolean existePorEmail(String email, Long idExcluir) {
        return idExcluir == null ? repository.existsByEmailIgnoreCase(email)
                : repository.existsByEmailIgnoreCaseAndIdNot(email, idExcluir);
    }
    @Override @Transactional public Optional<Cliente> bloquearPorId(Long id) { return repository.bloquearPorId(id).map(ClienteMapper::toDomain); }
}
