package org.example.sistema_canchas_deportivas.infraestructure.controllers;

import java.time.DayOfWeek;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.example.sistema_canchas_deportivas.application.ports.in.HorarioAtencionIn;
import org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos.*;
import org.example.sistema_canchas_deportivas.infraestructure.mappers.HorarioAtencionMapper;

@RestController
@RequestMapping("/api")
public class HorarioAtencionController {
    private final HorarioAtencionIn horarios;
    private final HorarioAtencionMapper mapper;

    public HorarioAtencionController(HorarioAtencionIn horarios, HorarioAtencionMapper mapper) {
        this.horarios = horarios;
        this.mapper = mapper;
    }

    @GetMapping({"/horarios", "/admin/horarios"})
    public List<HorarioAtencionResponse> listar() {
        return horarios.listar().stream().map(mapper::toResponse).toList();
    }

    @PutMapping("/admin/horarios/{dia}")
    public HorarioAtencionResponse actualizar(@PathVariable DayOfWeek dia,
                                              @Valid @RequestBody HorarioAtencionRequest request) {
        return mapper.toResponse(horarios.actualizar(dia, request.apertura(), request.cierre()));
    }
    @GetMapping({"/horarios/{dia}", "/admin/horarios/{dia}"})
    public HorarioAtencionResponse buscar(@PathVariable DayOfWeek dia) { return mapper.toResponse(horarios.buscarPorDia(dia)); }
    @PostMapping("/admin/horarios")
    public org.springframework.http.ResponseEntity<HorarioAtencionResponse> crear(@Valid @RequestBody HorarioCrearRequest request) {
        var nuevo = horarios.crear(request.dia(), request.apertura(), request.cierre());
        return org.springframework.http.ResponseEntity.created(java.net.URI.create("/api/admin/horarios/" + nuevo.getDia())).body(mapper.toResponse(nuevo));
    }
    @DeleteMapping("/admin/horarios/{dia}")
    public org.springframework.http.ResponseEntity<Void> eliminar(@PathVariable DayOfWeek dia) {
        horarios.eliminar(dia); return org.springframework.http.ResponseEntity.noContent().build();
    }

}

