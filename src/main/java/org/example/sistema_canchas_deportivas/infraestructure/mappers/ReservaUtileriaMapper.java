package org.example.sistema_canchas_deportivas.infraestructure.mappers;

import org.springframework.stereotype.Component;
import org.example.sistema_canchas_deportivas.domain.models.ReservaUtileria;
import org.example.sistema_canchas_deportivas.infraestructure.entities.ReservaEntity;
import org.example.sistema_canchas_deportivas.infraestructure.entities.ReservaUtileriaEntity;
import org.example.sistema_canchas_deportivas.infraestructure.entities.UtileriaEntity;

@Component
public class ReservaUtileriaMapper {
    public ReservaUtileriaEntity toEntity(ReservaUtileria modelo, ReservaEntity reserva, UtileriaEntity utileria) {
        return new ReservaUtileriaEntity(modelo.getId(), reserva, utileria, modelo.getCantidad());
    }

    public ReservaUtileria toDomain(ReservaUtileriaEntity entity) {
        return new ReservaUtileria(entity.getId(), entity.getReserva().getId(),
                entity.getUtileria().getId(), entity.getCantidad(), entity.getCreado(), entity.getModificado());
    }
}
