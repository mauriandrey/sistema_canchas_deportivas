package org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos;
import java.time.LocalDateTime;
import java.util.Set;
import org.example.sistema_canchas_deportivas.domain.models.*;

public record UtileriaResponse(Long id, String nombre, TipoImplemento tipo, int existencias, boolean activa,
        Set<Disciplina> disciplinasCompatibles, LocalDateTime creado, LocalDateTime modificado) {}

