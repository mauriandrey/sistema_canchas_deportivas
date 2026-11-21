package org.example.sistema_canchas_deportivas.infraestructure.mappers;

import org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos.HorarioAtencionResponse;
import org.springframework.stereotype.Component;
import org.example.sistema_canchas_deportivas.domain.models.HorarioAtencion;
import org.example.sistema_canchas_deportivas.infraestructure.entities.HorarioAtencionEntity;

@Component
public class HorarioAtencionMapper {
    public HorarioAtencionResponse toResponse(HorarioAtencion modelo) {
        return new HorarioAtencionResponse(
                modelo.getId(), modelo.getDia(), modelo.getApertura(), modelo.getCierre());
    }

    public HorarioAtencionEntity toEntity(HorarioAtencion modelo) {
        return new HorarioAtencionEntity(modelo.getId(), modelo.getDia(), modelo.getApertura(), modelo.getCierre());
    }

    public HorarioAtencion toDomain(HorarioAtencionEntity entity) {
        return new HorarioAtencion(entity.getId(), entity.getDia(), entity.getApertura(), entity.getCierre(), entity.getCreado(), entity.getModificado());
    }
}

