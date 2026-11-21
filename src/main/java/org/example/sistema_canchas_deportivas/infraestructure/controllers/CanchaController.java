package org.example.sistema_canchas_deportivas.infraestructure.controllers;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.example.sistema_canchas_deportivas.application.ports.in.CanchaIn;
import org.example.sistema_canchas_deportivas.domain.models.Disciplina;
import org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos.*;
import org.example.sistema_canchas_deportivas.infraestructure.mappers.CanchaMapper;

@RestController
@RequestMapping("/api")
public class CanchaController {
    private final CanchaIn canchas;
    private final CanchaMapper mapper;

    public CanchaController(CanchaIn canchas, CanchaMapper mapper) {
        this.canchas = canchas;
        this.mapper = mapper;
    }

    @GetMapping({"/canchas", "/admin/canchas"})
    public List<CanchaResponse> listar() {
        return canchas.listar().stream().map(mapper::toResponse).toList();
    }

    @GetMapping({"/canchas/{id}", "/admin/canchas/{id}"})
    public CanchaResponse obtener(@PathVariable Long id) {
        return mapper.toResponse(canchas.buscarPorId(id));
    }

    @PostMapping("/admin/canchas")
    public ResponseEntity<CanchaResponse> crear(@Valid @RequestBody CanchaRequest request) {
        var creada = canchas.crear(request.nombre(), request.tipo(), request.superficie(), request.activa());
        return ResponseEntity.created(URI.create("/api/canchas/" + creada.getId())).body(mapper.toResponse(creada));
    }

    @PutMapping("/admin/canchas/{id}")
    public CanchaResponse actualizar(@PathVariable Long id, @Valid @RequestBody CanchaRequest request) {
        return mapper.toResponse(canchas.actualizar(id, request.nombre(), request.tipo(), request.superficie(), request.activa()));
    }

    @PatchMapping("/admin/canchas/{id}/estado")
    public CanchaResponse estado(@PathVariable Long id, @Valid @RequestBody EstadoCanchaRequest request) {
        return mapper.toResponse(canchas.cambiarEstado(id, request.activa()));
    }

    @GetMapping("/canchas/disponibilidad")
    public DisponibilidadResponse disponibilidad(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam Disciplina disciplina, @RequestParam int duracion) {
        var disponibles = canchas.disponibilidad(fecha, disciplina, duracion).stream()
                .map(d -> new DisponibilidadResponse.CanchaDisponible(mapper.toResponse(d.cancha()),
                        d.horarios().stream().map(b -> new DisponibilidadResponse.Bloque(b.inicio(), b.fin())).toList()))
                .toList();
        return new DisponibilidadResponse(fecha, disciplina, duracion, disponibles);
    }
    @DeleteMapping("/admin/canchas/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        canchas.eliminar(id); return ResponseEntity.noContent().build();
    }
}

