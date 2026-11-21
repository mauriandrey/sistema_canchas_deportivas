package org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos;

import java.util.Set;
import org.example.sistema_canchas_deportivas.domain.models.*;

public record CanchaResponse(Long id, String nombre, TipoCancha tipo, Superficie superficie,
                             boolean activa, Set<Disciplina> disciplinasCompatibles) {}

