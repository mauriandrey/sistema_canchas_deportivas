package org.example.sistema_canchas_deportivas.infraestructure.adapters;

import java.util.List;
import java.util.Optional;
import java.time.DayOfWeek;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.example.sistema_canchas_deportivas.application.ports.out.HorarioAtencionOut;
import org.example.sistema_canchas_deportivas.domain.models.HorarioAtencion;
import org.example.sistema_canchas_deportivas.infraestructure.mappers.HorarioAtencionMapper;
import org.example.sistema_canchas_deportivas.infraestructure.repositories.HorarioAtencionRepository;

@Repository
@Transactional(readOnly = true)
public class HorarioAtencionAdapter implements HorarioAtencionOut {
    private final HorarioAtencionRepository repository;
    private final HorarioAtencionMapper mapper;

    public HorarioAtencionAdapter(HorarioAtencionRepository repository, HorarioAtencionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public boolean existePorDia(DayOfWeek dia) {
        return repository.existsByDia(dia);
    }

    @Override
    @Transactional
    public HorarioAtencion guardar(HorarioAtencion modelo) {
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
    public Optional<HorarioAtencion> buscarPorId(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<HorarioAtencion> listar() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }
    @Override
    public Optional<HorarioAtencion> buscarPorDia(DayOfWeek dia) {
        return repository.findByDia(dia).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<HorarioAtencion> bloquearPorDia(DayOfWeek dia) {
        return repository.bloquearPorDia(dia).map(mapper::toDomain);
    }
    @Override @Transactional public void eliminar(Long id) { repository.deleteById(id); repository.flush(); }
}

