package org.example.sistema_canchas_deportivas.infraestructure.mappers;

import org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos.UtileriaResponse;

import org.springframework.stereotype.Component;
import org.example.sistema_canchas_deportivas.domain.models.Utileria;
import org.example.sistema_canchas_deportivas.infraestructure.entities.UtileriaEntity;

@Component
public class UtileriaMapper {
    public UtileriaResponse toResponse(Utileria modelo) {
        return new UtileriaResponse(
                modelo.getId(), modelo.getNombre(), modelo.getTipo(), modelo.getExistencias(), modelo.isActiva(),
                modelo.getDisciplinasCompatibles(), modelo.getCreado(), modelo.getModificado());
    }

    public UtileriaEntity toEntity(Utileria modelo) {
        UtileriaEntity entity = new UtileriaEntity(modelo.getId(), modelo.getNombre(), modelo.getTipo(),
                modelo.getExistencias(), modelo.isActiva());
        entity.definirDisciplinas(modelo.getDisciplinasCompatibles());
        return entity;
    }

    public Utileria toDomain(UtileriaEntity entity) {
        return new Utileria(entity.getId(), entity.getNombre(), entity.getTipo(),
                entity.getExistencias(), entity.isActiva(), entity.getDisciplinasCompatibles(), entity.getCreado(), entity.getModificado());
    }
}

