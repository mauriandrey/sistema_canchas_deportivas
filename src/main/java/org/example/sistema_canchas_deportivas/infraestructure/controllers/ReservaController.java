package org.example.sistema_canchas_deportivas.infraestructure.controllers;

import org.example.sistema_canchas_deportivas.domain.models.SolicitudUtileria;

import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.example.sistema_canchas_deportivas.application.ports.in.ReservaIn;
import org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos.*;
import org.example.sistema_canchas_deportivas.infraestructure.mappers.ReservaMapper;
import org.example.sistema_canchas_deportivas.infraestructure.securities.UsuarioPrincipal;

@RestController
@RequestMapping("/api")
public class ReservaController {
    private final ReservaIn reservas;
    private final ReservaMapper mapper;

    public ReservaController(ReservaIn reservas, ReservaMapper mapper) {
        this.reservas = reservas;
        this.mapper = mapper;
    }

    @PostMapping("/reservas")
    public ResponseEntity<ReservaResponse> crear(@AuthenticationPrincipal UsuarioPrincipal principal,
                                                @Valid @RequestBody ReservaRequest request) {
        var creada = reservas.crear(principal.getId(), request.canchaId(), request.disciplina(),
                request.fecha().atTime(request.horaInicio()), request.duracion(),
                request.utileria() == null ? List.of() : request.utileria().stream()
                        .map(d -> new SolicitudUtileria(d.utileriaId(), d.cantidad())).toList());
        return ResponseEntity.created(URI.create("/api/reservas/" + creada.getId())).body(mapper.toResponse(creada));
    }

    @GetMapping("/reservas/mis-reservas")
    public List<ReservaResponse> propias(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return reservas.misReservas(principal.getId()).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/reservas/{id}")
    public ReservaResponse consultar(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
        return mapper.toResponse(reservas.consultar(principal.getId(), id));
    }

    @PatchMapping("/reservas/{id}/cancelar")
    public ReservaResponse cancelar(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
        return mapper.toResponse(reservas.cancelar(principal.getId(), id));
    }

    @GetMapping("/admin/reservas")
    public List<ReservaResponse> todas(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return reservas.listarTodas(principal.getId()).stream().map(mapper::toResponse).toList();
    }
    @GetMapping("/admin/reservas/{id}")
    public ReservaResponse consultarAdmin(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
        return mapper.toResponse(reservas.consultar(principal.getId(), id));
    }
    @PostMapping("/admin/reservas")
    public ResponseEntity<ReservaResponse> crearAdmin(@AuthenticationPrincipal UsuarioPrincipal principal,
                                                     @Valid @RequestBody ReservaAdminRequest request) {
        if (request.estado() != null && request.estado() != org.example.sistema_canchas_deportivas.domain.models.EstadoReserva.CONFIRMADA)
            throw new org.example.sistema_canchas_deportivas.domain.exception.DatoInvalidoException("Las reservas nuevas se crean confirmadas.");
        var nueva = reservas.crearAdmin(principal.getId(), request.responsableId(), request.canchaId(), request.disciplina(),
                request.fecha().atTime(request.horaInicio()), request.duracion(), detalles(request));
        return ResponseEntity.created(URI.create("/api/admin/reservas/" + nueva.getId())).body(mapper.toResponse(nueva));
    }
    @PutMapping("/admin/reservas/{id}")
    public ReservaResponse actualizarAdmin(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id,
                                            @Valid @RequestBody ReservaAdminRequest request) {
        return mapper.toResponse(reservas.actualizarAdmin(principal.getId(), id, request.responsableId(), request.canchaId(),
                request.disciplina(), request.fecha().atTime(request.horaInicio()), request.duracion(), request.estado(), detalles(request)));
    }
    @DeleteMapping("/admin/reservas/{id}")
    public ResponseEntity<Void> eliminarAdmin(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
        reservas.eliminarAdmin(principal.getId(), id); return ResponseEntity.noContent().build();
    }
    @PatchMapping("/admin/reservas/{id}/cancelar")
    public ReservaResponse cancelarAdmin(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
        return mapper.toResponse(reservas.cancelarAdmin(principal.getId(), id));
    }
    @GetMapping("/admin/reservas/{id}/utileria")
    public List<ReservaUtileriaResponse> detallesAdmin(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
        return mapper.toResponse(reservas.consultar(principal.getId(), id)).utileria();
    }
    @GetMapping("/admin/reservas/{id}/utileria/{utileriaId}")
    public ReservaUtileriaResponse detalleAdmin(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id, @PathVariable Long utileriaId) {
        return detallesAdmin(principal, id).stream().filter(d -> d.utileriaId().equals(utileriaId)).findFirst()
                .orElseThrow(() -> new org.example.sistema_canchas_deportivas.domain.exception.RecursoNoEncontradoException("Detalle no encontrado en esta reserva."));
    }
    @PostMapping("/admin/reservas/{id}/utileria")
    public ResponseEntity<ReservaResponse> agregarDetalle(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id,
                                                         @Valid @RequestBody ReservaUtileriaRequest request) {
        var resultado = reservas.cambiarDetalleAdmin(principal.getId(), id, request.utileriaId(), request.cantidad(), true);
        return ResponseEntity.created(URI.create("/api/admin/reservas/" + id + "/utileria/" + request.utileriaId())).body(mapper.toResponse(resultado));
    }
    @PutMapping("/admin/reservas/{id}/utileria/{utileriaId}")
    public ReservaResponse editarDetalle(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id,
                                         @PathVariable Long utileriaId, @Valid @RequestBody CantidadDetalleRequest request) {
        return mapper.toResponse(reservas.cambiarDetalleAdmin(principal.getId(), id, utileriaId, request.cantidad(), false));
    }
    @DeleteMapping("/admin/reservas/{id}/utileria/{utileriaId}")
    public ResponseEntity<Void> eliminarDetalle(@AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id, @PathVariable Long utileriaId) {
        reservas.cambiarDetalleAdmin(principal.getId(), id, utileriaId, null, false);
        return ResponseEntity.noContent().build();
    }
    private List<SolicitudUtileria> detalles(ReservaAdminRequest request) {
        return request.utileria() == null ? List.of() : request.utileria().stream().map(d -> new SolicitudUtileria(d.utileriaId(), d.cantidad())).toList();
    }

}

