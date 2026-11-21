package org.example.sistema_canchas_deportivas.infraestructure.controllers;

import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.example.sistema_canchas_deportivas.application.ports.in.ClienteIn;
import org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos.*;
import org.example.sistema_canchas_deportivas.infraestructure.mappers.ClienteMapper;

@RestController
@RequestMapping("/api/admin/clientes")
public class ClienteController {
    private final ClienteIn clientes;
    public ClienteController(ClienteIn clientes) { this.clientes = clientes; }

    @GetMapping
    public ActionResponse<List<ClienteResponse>> listar() {
        var lista = clientes.listar().stream().map(ClienteMapper::toResponse).toList();
        return new ActionResponse<>("Registros encontrados: " + lista.size(), lista);
    }
    @GetMapping("/{id}")
    public ActionResponse<ClienteResponse> buscar(@PathVariable Long id) {
        return new ActionResponse<>("Cliente encontrado", ClienteMapper.toResponse(clientes.buscarPorId(id)));
    }
    @PostMapping
    public ResponseEntity<ActionResponse<ClienteResponse>> crear(@Valid @RequestBody ClienteRequest request) {
        var creado = clientes.crear(ClienteMapper.toRequest(request));
        return ResponseEntity.created(URI.create("/api/admin/clientes/" + creado.getId()))
                .body(new ActionResponse<>("Cliente creado correctamente", ClienteMapper.toResponse(creado)));
    }
    @PutMapping("/{id}")
    public ActionResponse<ClienteResponse> actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        var actualizado = clientes.actualizar(id, ClienteMapper.toRequest(request));
        return new ActionResponse<>("Cliente actualizado correctamente", ClienteMapper.toResponse(actualizado));
    }
    @DeleteMapping("/{id}")
    public ActionResponse<Void> eliminar(@PathVariable Long id) {
        clientes.eliminar(id);
        return new ActionResponse<>("Cliente eliminado correctamente", null);
    }
}
