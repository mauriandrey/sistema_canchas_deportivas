package org.example.sistema_canchas_deportivas.infraestructure.mappers;

import org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos.ReservaUtileriaResponse;

import org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos.ReservaResponse;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.example.sistema_canchas_deportivas.domain.models.Reserva;
import org.example.sistema_canchas_deportivas.domain.models.ReservaUtileria;
import org.example.sistema_canchas_deportivas.infraestructure.entities.ReservaEntity;
import org.example.sistema_canchas_deportivas.infraestructure.entities.UsuarioEntity;
import org.example.sistema_canchas_deportivas.infraestructure.entities.CanchaEntity;
import org.example.sistema_canchas_deportivas.infraestructure.entities.UtileriaEntity;

@Component
public class ReservaMapper {
    private final ReservaUtileriaMapper detalleMapper;

    public ReservaMapper(ReservaUtileriaMapper detalleMapper) {
        this.detalleMapper = detalleMapper;
    }

    public ReservaEntity toEntity(Reserva modelo, UsuarioEntity responsable, CanchaEntity cancha,
                                  Map<Long, UtileriaEntity> implementos) {
        ReservaEntity entity = new ReservaEntity(modelo.getId(), responsable, cancha, modelo.getDisciplina(),
                modelo.getFecha(), modelo.getHoraInicio(), modelo.getDuracionHoras(),
                modelo.getEstado(), modelo.getFechaCreacion());
        for (ReservaUtileria detalle : modelo.getUtileria()) {
            entity.agregarUtileria(detalleMapper.toEntity(detalle, entity, implementos.get(detalle.getUtileriaId())));
        }
        return entity;
    }

    public ReservaResponse toResponse(Reserva modelo) {
        return new ReservaResponse(
                modelo.getId(), modelo.getResponsableId(), modelo.getCanchaId(), modelo.getDisciplina(),
                modelo.getFecha(), modelo.getHoraInicio(), modelo.getHoraFin(), modelo.getDuracionHoras(),
                modelo.getEstado(), modelo.getFechaCreacion(), modelo.getCreado(), modelo.getModificado(),
                modelo.getUtileria().stream().map(d -> new ReservaUtileriaResponse(
                        d.getId(), d.getUtileriaId(), d.getCantidad(), d.getCreado(), d.getModificado())).toList());
    }

    public Reserva toDomain(ReservaEntity entity) {
        return new Reserva(entity.getId(), entity.getResponsable().getId(), entity.getCancha().getId(),
                entity.getDisciplina(), entity.getInicio(), entity.getDuracionHoras(),
                entity.getEstado(), entity.getFechaCreacion(),
                entity.getUtileria().stream().map(detalleMapper::toDomain).toList(), entity.getCreado(), entity.getModificado());
    }
}

