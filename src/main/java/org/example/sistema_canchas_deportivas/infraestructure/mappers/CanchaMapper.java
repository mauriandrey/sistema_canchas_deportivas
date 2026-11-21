package org.example.sistema_canchas_deportivas.infraestructure.mappers;

import org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos.CanchaResponse;
import org.springframework.stereotype.Component;
import org.example.sistema_canchas_deportivas.domain.models.Cancha;
import org.example.sistema_canchas_deportivas.infraestructure.entities.CanchaEntity;

@Component
public class CanchaMapper {
    public CanchaResponse toResponse(Cancha modelo) {
        return new CanchaResponse(
                modelo.getId(), modelo.getNombre(), modelo.getTipo(), modelo.getSuperficie(),
                modelo.isActiva(), modelo.getDisciplinasCompatibles());
    }

    public CanchaEntity toEntity(Cancha modelo) {
        return new CanchaEntity(modelo.getId(), modelo.getNombre(), modelo.getTipo(), modelo.getSuperficie(), modelo.isActiva());
    }

    public Cancha toDomain(CanchaEntity entity) {
        return new Cancha(entity.getId(), entity.getNombre(), entity.getTipo(), entity.getSuperficie(), entity.isActiva(), entity.getCreado(), entity.getModificado());
    }
}

