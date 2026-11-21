package org.example.sistema_canchas_deportivas.infraestructure.adapters;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.example.sistema_canchas_deportivas.application.ports.out.CanchaOut;
import org.example.sistema_canchas_deportivas.domain.models.Cancha;
import org.example.sistema_canchas_deportivas.infraestructure.mappers.CanchaMapper;
import org.example.sistema_canchas_deportivas.infraestructure.repositories.CanchaRepository;

@Repository
@Transactional(readOnly = true)
public class CanchaAdapter implements CanchaOut {
    private final CanchaRepository repository;
    private final CanchaMapper mapper;

    public CanchaAdapter(CanchaRepository repository, CanchaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public boolean existePorNombre(String nombre) {
        return repository.existsByNombre(nombre);
    }

    @Override
    @Transactional
    public Cancha guardar(Cancha modelo) {
        var entity = mapper.toEntity(modelo);
        if (modelo.getId() != null) {
            var existente = repository.findById(modelo.getId()).orElseThrow(() ->
                    new org.example.sistema_canchas_deportivas.domain.exception.RecursoNoEncontradoException("Registro no encontrado."));
            // El ID y las fechas se recuperan de la base, nunca de la solicitud.
            entity.conservarAuditoriaDe(existente);
        }
        return mapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public Optional<Cancha> buscarPorId(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Cancha> listar() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }
    @Override
    public boolean existePorNombreExcluyendoId(String nombre, Long id) {
        return repository.existsByNombreAndIdNot(nombre, id);
    }

    @Override
    @Transactional
    public Optional<Cancha> bloquearPorId(Long id) {
        return repository.bloquearPorId(id).map(mapper::toDomain);
    }
    @Override @Transactional public void eliminar(Long id) { repository.deleteById(id); repository.flush(); }
}

