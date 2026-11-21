package org.example.sistema_canchas_deportivas.infraestructure.adapters;

import org.example.sistema_canchas_deportivas.domain.models.CompromisoUtileria;
import java.time.LocalDate;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.example.sistema_canchas_deportivas.infraestructure.auditoria.AuditoriaListener;
import org.springframework.transaction.annotation.Transactional;
import org.example.sistema_canchas_deportivas.application.ports.out.UtileriaOut;
import org.example.sistema_canchas_deportivas.domain.models.Utileria;
import org.example.sistema_canchas_deportivas.infraestructure.mappers.UtileriaMapper;
import org.example.sistema_canchas_deportivas.infraestructure.repositories.UtileriaRepository;

@Repository
@Transactional(readOnly = true)
public class UtileriaAdapter implements UtileriaOut {
    private final UtileriaRepository repository;
    private final UtileriaMapper mapper;
    private final AuditoriaListener auditoria;

    public UtileriaAdapter(UtileriaRepository repository, UtileriaMapper mapper, AuditoriaListener auditoria) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditoria = auditoria;
    }

    @Override
    public boolean existePorNombre(String nombre) {
        return repository.existsByNombre(nombre);
    }

    @Override
    @Transactional
    public Utileria guardar(Utileria modelo) {
        var entity = mapper.toEntity(modelo);
        if (modelo.getId() != null) {
            var existente = repository.findById(modelo.getId()).orElseThrow(() ->
                    new org.example.sistema_canchas_deportivas.domain.exception.RecursoNoEncontradoException("Registro no encontrado."));
            // El ID y las fechas se recuperan de la base, nunca de la solicitud.
            entity.conservarAuditoriaDe(existente);
            // Cambiar solo @ElementCollection no dispara @PreUpdate de la entidad propietaria.
            if (!existente.getDisciplinasCompatibles().equals(modelo.getDisciplinasCompatibles())) {
                auditoria.alModificar(entity);
            }
        }
        return mapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public Optional<Utileria> buscarPorId(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Utileria> listar() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }
    @Override
    public boolean existePorNombreExcluyendoId(String nombre, Long id) {
        return repository.existsByNombreAndIdNot(nombre, id);
    }

    @Override
    @Transactional
    public Optional<Utileria> bloquearPorId(Long id) {
        return repository.bloquearPorId(id).map(mapper::toDomain);
    }

    @Override
    public boolean tieneHistorial(Long id) { return repository.tieneHistorial(id); }

    @Override
    public List<CompromisoUtileria> compromisosConfirmados(
            Long id, LocalDate desde, LocalDate hasta) {
        return repository.compromisosConfirmados(id, desde, hasta);
    }
    @Override @Transactional public void eliminar(Long id) { repository.deleteById(id); repository.flush(); }
    @Override public List<CompromisoUtileria> compromisosExcluyendo(Long id, LocalDate desde, LocalDate hasta, Long excluir) {
        return repository.compromisosExcluyendo(id, desde, hasta, excluir);
    }

}

