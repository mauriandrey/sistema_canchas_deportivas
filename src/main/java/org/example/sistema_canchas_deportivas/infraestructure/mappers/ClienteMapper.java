package org.example.sistema_canchas_deportivas.infraestructure.mappers;
import org.example.sistema_canchas_deportivas.domain.models.Cliente;
import org.example.sistema_canchas_deportivas.infraestructure.entities.ClienteEntity;
import org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos.*;

public final class ClienteMapper {
    private ClienteMapper() {}
    public static Cliente toDomain(ClienteEntity entity) {
        return new Cliente(entity.getId(), entity.getCedula(), entity.getNombres(), entity.getApellidos(),
                entity.getEmail(), entity.getTelefono(), entity.getCreado(), entity.getModificado());
    }
    public static void toEntity(Cliente cliente, ClienteEntity entity) {
        entity.setCedula(cliente.getCedula());
        entity.setNombres(cliente.getNombres());
        entity.setApellidos(cliente.getApellidos());
        entity.setEmail(cliente.getEmail());
        entity.setTelefono(cliente.getTelefono());
    }
    public static Cliente toRequest(ClienteRequest request) {
        return Cliente.nuevo(request.cedula(), request.nombres(), request.apellidos(), request.email(), request.telefono());
    }
    public static ClienteResponse toResponse(Cliente cliente) {
        return new ClienteResponse(cliente.getId(), cliente.getCedula(), cliente.getNombres(), cliente.getApellidos(),
                cliente.getEmail(), cliente.getTelefono(), cliente.getCreado(), cliente.getModificado());
    }
}
