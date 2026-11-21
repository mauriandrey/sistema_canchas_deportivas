package org.example.sistema_canchas_deportivas.infraestructure.adapters;

import org.example.sistema_canchas_deportivas.domain.models.ReferenciaReserva;
import org.example.sistema_canchas_deportivas.domain.models.EstadoReserva;
import java.time.LocalDate;
import org.example.sistema_canchas_deportivas.domain.models.OcupacionCancha;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.example.sistema_canchas_deportivas.application.ports.out.ReservaOut;
import org.example.sistema_canchas_deportivas.domain.models.Reserva;
import org.example.sistema_canchas_deportivas.domain.models.ReservaUtileria;
import org.example.sistema_canchas_deportivas.infraestructure.entities.UtileriaEntity;
import org.example.sistema_canchas_deportivas.infraestructure.mappers.ReservaMapper;
import org.example.sistema_canchas_deportivas.infraestructure.repositories.ReservaRepository;
import org.example.sistema_canchas_deportivas.infraestructure.repositories.UsuarioRepository;
import org.example.sistema_canchas_deportivas.infraestructure.repositories.CanchaRepository;
import org.example.sistema_canchas_deportivas.infraestructure.repositories.UtileriaRepository;

@Repository
@Transactional(readOnly = true)
public class ReservaAdapter implements ReservaOut {
    private final ReservaRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final CanchaRepository canchaRepository;
    private final UtileriaRepository utileriaRepository;
    private final ReservaMapper mapper;
    private final org.example.sistema_canchas_deportivas.infraestructure.auditoria.AuditoriaListener auditoria;

    public ReservaAdapter(ReservaRepository repository, UsuarioRepository usuarioRepository,
                          CanchaRepository canchaRepository, UtileriaRepository utileriaRepository,
                          ReservaMapper mapper, org.example.sistema_canchas_deportivas.infraestructure.auditoria.AuditoriaListener auditoria) {
        this.auditoria = auditoria;
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.canchaRepository = canchaRepository;
        this.utileriaRepository = utileriaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Reserva guardar(Reserva modelo) {
        // Referencias a filas existentes: no se crean usuarios, canchas o implementos por cascada.
        Map<Long, UtileriaEntity> implementos = new HashMap<>();
        for (ReservaUtileria detalle : modelo.getUtileria()) {
            implementos.computeIfAbsent(detalle.getUtileriaId(), utileriaRepository::getReferenceById);
        }
        var entity = mapper.toEntity(modelo,
                usuarioRepository.getReferenceById(modelo.getResponsableId()),
                canchaRepository.getReferenceById(modelo.getCanchaId()), implementos);
        if (modelo.getId() != null) {
            var existente = repository.findById(modelo.getId()).orElseThrow(() ->
                    new org.example.sistema_canchas_deportivas.domain.exception.RecursoNoEncontradoException("Reserva no encontrada."));
            entity.conservarAuditoriaDe(existente);
            // Los cambios de detalles también cuentan como modificación de la reserva.
            var previos = existente.getUtileria().stream().collect(java.util.stream.Collectors.toMap(
                    d -> d.getUtileria().getId(), d -> d.getCantidad()));
            var nuevos = modelo.getUtileria().stream().collect(java.util.stream.Collectors.toMap(
                    ReservaUtileria::getUtileriaId, ReservaUtileria::getCantidad));
            if (!previos.equals(nuevos)) auditoria.alModificar(entity);
            for (var detalle : entity.getUtileria()) {
                if (detalle.getId() != null) {
                    var anterior = existente.getUtileria().stream().filter(d -> d.getId().equals(detalle.getId()))
                            .findFirst().orElseThrow(() -> new IllegalArgumentException("Detalle ajeno a la reserva."));
                    detalle.conservarAuditoriaDe(anterior);
                }
            }
        }
        return mapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public Optional<Reserva> buscarPorId(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Reserva> listar() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }
    @Override
    public boolean existeHistorialCancha(Long canchaId) {
        return repository.existsByCanchaId(canchaId);
    }

    @Override
    public List<OcupacionCancha> ocupacionesConfirmadas(
            LocalDate desde, LocalDate hasta) {
        return repository.ocupacionesConfirmadas(desde, hasta);
    }
    @Override
    public List<Reserva> listarPorResponsable(Long responsableId) {
        return repository.findByResponsableIdOrderByFechaDescHoraInicioDescIdDesc(responsableId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Reserva> confirmadasPorResponsableYFecha(Long responsableId, LocalDate fecha) {
        return repository.findByResponsableIdAndFechaAndEstado(responsableId, fecha,
                EstadoReserva.CONFIRMADA)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Reserva> confirmadasPorCanchaYFecha(Long canchaId, LocalDate fecha) {
        return repository.findByCanchaIdAndFechaAndEstado(canchaId, fecha,
                EstadoReserva.CONFIRMADA)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<ReferenciaReserva> referenciaPorId(Long id) {
        return repository.referenciaPorId(id);
    }

    @Override
    @Transactional
    public Optional<Reserva> bloquearPorId(Long id) {
        return repository.bloquearPorId(id).map(mapper::toDomain);
    }
    @Override
    public List<Long> utileriaIdsPorReserva(Long id) { return repository.utileriaIdsPorReserva(id); }
    @Override @Transactional public void eliminar(Long id) { repository.deleteById(id); repository.flush(); }
}

