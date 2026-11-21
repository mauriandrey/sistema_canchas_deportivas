package org.example.sistema_canchas_deportivas.infraestructure.controllers;

import java.time.*;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.example.sistema_canchas_deportivas.application.ports.in.UtileriaIn;
import org.example.sistema_canchas_deportivas.domain.models.Disciplina;
import org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos.*;
import org.example.sistema_canchas_deportivas.infraestructure.mappers.UtileriaMapper;

@RestController
@RequestMapping("/api")
public class UtileriaController {
    private final UtileriaIn utileria;
    private final UtileriaMapper mapper;

    public UtileriaController(UtileriaIn utileria, UtileriaMapper mapper) {
        this.utileria = utileria;
        this.mapper = mapper;
    }

    @GetMapping({"/utileria", "/admin/utileria"})
    public List<UtileriaResponse> listar() {
        return utileria.listar().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/utileria/disponibilidad")
    public UtileriaDisponibilidadResponse disponibilidad(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio,
            @RequestParam int duracion, @RequestParam Disciplina disciplina) {
        var disponibles = utileria.disponibilidad(fecha.atTime(horaInicio), duracion, disciplina);
        return new UtileriaDisponibilidadResponse(fecha, horaInicio, horaInicio.plusHours(duracion), duracion, disciplina,
                disponibles.stream().map(d -> new UtileriaDisponibilidadResponse.ImplementoDisponible(
                        mapper.toResponse(d.utileria()), d.comprometidas(), d.disponibles())).toList());
    }

    @PostMapping("/admin/utileria")
    public ResponseEntity<UtileriaResponse> crear(@Valid @RequestBody UtileriaRequest request) {
        var creada = utileria.crear(request.nombre(), request.tipo(), request.existencias(),
                request.activa(), request.disciplinasCompatibles());
        return ResponseEntity.created(java.net.URI.create("/api/admin/utileria/" + creada.getId())).body(mapper.toResponse(creada));
    }

    @PutMapping("/admin/utileria/{id}")
    public UtileriaResponse actualizar(@PathVariable Long id, @Valid @RequestBody UtileriaRequest request) {
        return mapper.toResponse(utileria.actualizar(id, request.nombre(), request.tipo(), request.existencias(),
                request.activa(), request.disciplinasCompatibles()));
    }

    @PatchMapping("/admin/utileria/{id}/existencias")
    public UtileriaResponse existencias(@PathVariable Long id, @Valid @RequestBody ExistenciasRequest request) {
        return mapper.toResponse(utileria.cambiarExistencias(id, request.existencias()));
    }
    @DeleteMapping("/admin/utileria/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        utileria.eliminar(id); return ResponseEntity.noContent().build();
    }
    @GetMapping({"/utileria/{id}", "/admin/utileria/{id}"})
    public UtileriaResponse buscar(@PathVariable Long id) { return mapper.toResponse(utileria.buscarPorId(id)); }

}

